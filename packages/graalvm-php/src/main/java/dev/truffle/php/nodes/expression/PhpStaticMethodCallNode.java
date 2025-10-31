package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;

/**
 * AST node for calling static class methods.
 * Syntax: ClassName::methodName(args)
 */
public final class PhpStaticMethodCallNode extends PhpExpressionNode {

    private final String className;
    private final String methodName;

    @Children
    private final PhpExpressionNode[] argumentNodes;

    public PhpStaticMethodCallNode(String className, String methodName, PhpExpressionNode[] argumentNodes) {
        this.className = className;
        this.methodName = methodName;
        this.argumentNodes = argumentNodes;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        PhpContext context = PhpContext.get(this);
        PhpClass phpClass = context.getClass(className);

        if (phpClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }

        if (!phpClass.hasMethod(methodName)) {
            throw new RuntimeException("Static method " + className + "::" + methodName + " does not exist");
        }

        PhpClass.MethodMetadata method = phpClass.getMethod(methodName);

        if (!method.isStatic()) {
            throw new RuntimeException("Method " + className + "::" + methodName + " is not static");
        }

        // Evaluate arguments
        Object[] arguments = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            arguments[i] = argumentNodes[i].execute(frame);
        }

        CallTarget callTarget = method.getCallTarget();
        return callTarget.call(arguments);
    }
}
