package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;

/**
 * AST node for writing static class properties.
 * Syntax: ClassName::$propertyName = value
 */
public final class PhpStaticPropertyWriteNode extends PhpExpressionNode {

    private final String className;
    private final String propertyName;

    @Child
    private PhpExpressionNode valueNode;

    public PhpStaticPropertyWriteNode(String className, String propertyName, PhpExpressionNode valueNode) {
        this.className = className;
        this.propertyName = propertyName;
        this.valueNode = valueNode;
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

        Object value = valueNode.execute(frame);
        phpClass.setStaticPropertyValue(propertyName, value);
        return value;
    }
}
