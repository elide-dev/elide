package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;

/**
 * AST node for reading static class properties.
 * Syntax: ClassName::$propertyName
 */
public final class PhpStaticPropertyAccessNode extends PhpExpressionNode {

    private final String className;
    private final String propertyName;

    public PhpStaticPropertyAccessNode(String className, String propertyName) {
        this.className = className;
        this.propertyName = propertyName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        PhpContext context = PhpContext.get(this);
        PhpClass phpClass = context.getClass(className);

        if (phpClass == null) {
            throw new RuntimeException("Class not found: " + className);
        }

        if (!phpClass.hasStaticProperty(propertyName)) {
            throw new RuntimeException("Static property " + className + "::$" + propertyName + " does not exist");
        }

        return phpClass.getStaticPropertyValue(propertyName);
    }
}
