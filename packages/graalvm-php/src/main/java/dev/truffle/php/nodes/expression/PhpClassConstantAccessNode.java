package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;

/**
 * Node for accessing a class constant.
 * Syntax: ClassName::CONSTANT_NAME, self::CONSTANT_NAME, parent::CONSTANT_NAME
 */
public final class PhpClassConstantAccessNode extends PhpExpressionNode {

    private final String className;
    private final String constantName;

    public PhpClassConstantAccessNode(String className, String constantName) {
        this.className = className;
        this.constantName = constantName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        PhpContext context = PhpContext.get(this);

        // Get the class
        PhpClass phpClass = context.getClass(className);
        if (phpClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }

        // Get the constant value
        Object value = phpClass.getConstant(constantName);
        if (value == null) {
            throw new RuntimeException("Undefined class constant: " + className + "::" + constantName);
        }

        return value;
    }
}
