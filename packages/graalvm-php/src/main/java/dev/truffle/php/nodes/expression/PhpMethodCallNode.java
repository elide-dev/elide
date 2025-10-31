package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpArray;
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

        // Check if method exists
        if (!phpClass.hasMethod(methodName)) {
            // Try __call magic method
            if (phpClass.hasMethod("__call")) {
                return invokeCallMagicMethod(object, phpClass, frame);
            }
            throw new RuntimeException("Call to undefined method: " + phpClass.getName() + "::" + methodName + "()");
        }

        PhpClass.MethodMetadata method = phpClass.getMethod(methodName);

        // Look up caller class if we have a class name
        PhpClass callerClass = null;
        if (callerClassName != null) {
            PhpContext context = PhpContext.get(this);
            callerClass = context.getClass(callerClassName);

            // If caller class not found, check if it's a trait
            // Traits methods are composed into classes, so treat the target class as the caller
            if (callerClass == null && context.getTrait(callerClassName) != null) {
                // Caller is a trait - use the target class as caller for visibility checks
                // This allows trait methods to call each other's private methods
                callerClass = phpClass;
            }
        }

        // Check visibility with caller context
        if (!phpClass.isMethodAccessible(methodName, callerClass)) {
            // Try __call magic method for inaccessible methods
            if (phpClass.hasMethod("__call")) {
                return invokeCallMagicMethod(object, phpClass, frame);
            }
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

    /**
     * Invoke the __call magic method with method name and arguments array.
     */
    @ExplodeLoop
    private Object invokeCallMagicMethod(PhpObject object, PhpClass phpClass, VirtualFrame frame) {
        PhpClass.MethodMetadata callMethod = phpClass.getMethod("__call");
        CallTarget callTarget = callMethod.getCallTarget();

        // Create PHP array with arguments
        PhpArray argsArray = new PhpArray();
        for (int i = 0; i < argumentNodes.length; i++) {
            Object argValue = argumentNodes[i].execute(frame);
            argsArray.append(argValue);
        }

        // __call receives: $this, method name (string), arguments (PhpArray)
        return callTarget.call(object, methodName, argsArray);
    }
}
