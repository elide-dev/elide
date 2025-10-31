package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpArray;
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

        // Check if method exists
        if (!phpClass.hasMethod(methodName)) {
            // Try __callStatic magic method
            if (phpClass.hasMethod("__callStatic")) {
                return invokeCallStaticMagicMethod(phpClass, frame);
            }
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

    /**
     * Invoke the __callStatic magic method with method name and arguments array.
     */
    @ExplodeLoop
    private Object invokeCallStaticMagicMethod(PhpClass phpClass, VirtualFrame frame) {
        PhpClass.MethodMetadata callStaticMethod = phpClass.getMethod("__callStatic");
        CallTarget callTarget = callStaticMethod.getCallTarget();

        // Create PHP array with arguments
        PhpArray argsArray = new PhpArray();
        for (int i = 0; i < argumentNodes.length; i++) {
            Object argValue = argumentNodes[i].execute(frame);
            argsArray.append(argValue);
        }

        // __callStatic receives: method name (string), arguments (PhpArray)
        return callTarget.call(methodName, argsArray);
    }
}
