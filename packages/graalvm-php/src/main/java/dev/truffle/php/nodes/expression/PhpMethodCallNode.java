package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpObject;
import dev.truffle.php.runtime.Visibility;

/**
 * AST node for calling object methods ($obj->method()).
 */
public final class PhpMethodCallNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode objectNode;

    @Children
    private final PhpExpressionNode[] argumentNodes;

    private final String methodName;
    private final String callerClassName; // The name of the class from which this call is being made (null for external)

    // Constructor with caller class name
    public PhpMethodCallNode(PhpExpressionNode objectNode, String methodName, PhpExpressionNode[] argumentNodes, String callerClassName) {
        this.objectNode = objectNode;
        this.methodName = methodName;
        this.argumentNodes = argumentNodes;
        this.callerClassName = callerClassName;
    }

    // Legacy constructor for backward compatibility
    public PhpMethodCallNode(PhpExpressionNode objectNode, String methodName, PhpExpressionNode[] argumentNodes, boolean isInternal) {
        this.objectNode = objectNode;
        this.methodName = methodName;
        this.argumentNodes = argumentNodes;
        this.callerClassName = null; // Treated as external access
    }

    // Old legacy constructor without isInternal (defaults to external)
    public PhpMethodCallNode(PhpExpressionNode objectNode, String methodName, PhpExpressionNode[] argumentNodes) {
        this(objectNode, methodName, argumentNodes, false);
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        Object objectValue = objectNode.execute(frame);

        if (!(objectValue instanceof PhpObject)) {
            throw new RuntimeException("Call to a member function on a non-object");
        }

        PhpObject object = (PhpObject) objectValue;
        PhpClass phpClass = object.getPhpClass();

        if (!phpClass.hasMethod(methodName)) {
            throw new RuntimeException("Call to undefined method: " + phpClass.getName() + "::" + methodName + "()");
        }

        PhpClass.MethodMetadata method = phpClass.getMethod(methodName);

        // Look up caller class if we have a class name
        PhpClass callerClass = null;
        if (callerClassName != null) {
            PhpContext context = PhpContext.get(this);
            callerClass = context.getClass(callerClassName);
        }

        // Check visibility with caller context
        if (!phpClass.isMethodAccessible(methodName, callerClass)) {
            String visibilityName = method.getVisibility().toString().toLowerCase();
            throw new RuntimeException("Cannot call " + visibilityName + " method: " + phpClass.getName() + "::" + methodName + "()");
        }

        // Prepare arguments: $this is first argument, then method parameters
        Object[] args = new Object[argumentNodes.length + 1];
        args[0] = object; // First argument is $this
        for (int i = 0; i < argumentNodes.length; i++) {
            args[i + 1] = argumentNodes[i].execute(frame);
        }

        // Call the method
        CallTarget callTarget = method.getCallTarget();
        return callTarget.call(args);
    }
}
