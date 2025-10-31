package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpObject;

/**
 * AST node for calling object methods ($obj->method()).
 */
public final class PhpMethodCallNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode objectNode;

    @Children
    private final PhpExpressionNode[] argumentNodes;

    private final String methodName;

    public PhpMethodCallNode(PhpExpressionNode objectNode, String methodName, PhpExpressionNode[] argumentNodes) {
        this.objectNode = objectNode;
        this.methodName = methodName;
        this.argumentNodes = argumentNodes;
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

        if (!method.isPublic()) {
            throw new RuntimeException("Call to private method: " + phpClass.getName() + "::" + methodName + "()");
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
