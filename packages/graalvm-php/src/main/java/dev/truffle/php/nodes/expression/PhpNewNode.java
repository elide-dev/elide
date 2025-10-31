package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpObject;

/**
 * AST node for object instantiation with the 'new' keyword.
 * Creates a new object instance and calls the constructor if present.
 */
public final class PhpNewNode extends PhpExpressionNode {

    private final String className;

    @Children
    private final PhpExpressionNode[] constructorArgs;

    public PhpNewNode(String className, PhpExpressionNode[] constructorArgs) {
        this.className = className;
        this.constructorArgs = constructorArgs;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        PhpContext context = PhpContext.get(this);
        PhpClass phpClass = context.getClass(className);

        if (phpClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }

        // Create new object instance
        PhpObject object = new PhpObject(phpClass);

        // Call constructor if present (including inherited constructor)
        CallTarget constructor = phpClass.getConstructorOrInherited();
        if (constructor != null) {
            // Evaluate constructor arguments
            Object[] args = new Object[constructorArgs.length + 1];
            args[0] = object; // First argument is $this
            for (int i = 0; i < constructorArgs.length; i++) {
                args[i + 1] = constructorArgs[i].execute(frame);
            }

            // Call constructor
            constructor.call(args);
        }

        return object;
    }
}
