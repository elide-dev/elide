package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;

/**
 * Node for accessing a parent class constant.
 * Syntax: parent::CONSTANT_NAME
 */
public final class PhpParentConstantAccessNode extends PhpExpressionNode {

    private final String constantName;
    private final String currentClassName;

    public PhpParentConstantAccessNode(String constantName, String currentClassName) {
        this.constantName = constantName;
        this.currentClassName = currentClassName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        PhpContext context = PhpContext.get(this);

        // Get the current class
        PhpClass currentClass = context.getClass(currentClassName);
        if (currentClass == null) {
            throw new RuntimeException("Class not found: " + currentClassName);
        }

        // Get the parent class
        PhpClass parentClass = currentClass.getParentClass();
        if (parentClass == null) {
            throw new RuntimeException("Class " + currentClassName + " has no parent class");
        }

        // Get the constant value from parent
        Object value = parentClass.getConstant(constantName);
        if (value == null) {
            throw new RuntimeException("Undefined class constant: " + parentClass.getName() + "::" + constantName);
        }

        return value;
    }
}
