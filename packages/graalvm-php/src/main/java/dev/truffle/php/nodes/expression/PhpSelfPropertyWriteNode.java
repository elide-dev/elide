package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;

/**
 * AST node for writing static properties using self:: keyword.
 *
 * Syntax: self::$propertyName = value;
 *
 * This node writes to static properties of the current class.
 */
public final class PhpSelfPropertyWriteNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode valueNode;

    private final String propertyName;
    private final String currentClassName; // The class from which self:: is being called

    public PhpSelfPropertyWriteNode(String propertyName, PhpExpressionNode valueNode, String currentClassName) {
        this.propertyName = propertyName;
        this.valueNode = valueNode;
        this.currentClassName = currentClassName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        PhpContext context = PhpContext.get(this);

        // Get the current class
        PhpClass currentClass = context.getClass(currentClassName);
        if (currentClass == null) {
            throw new RuntimeException("Cannot use self:: in undefined class: " + currentClassName);
        }

        // Check if the property exists and is static
        if (!currentClass.hasStaticProperty(propertyName)) {
            throw new RuntimeException("Access to undeclared static property: " + currentClassName + "::$" + propertyName);
        }

        // Evaluate the value
        Object value = valueNode.execute(frame);

        // Write the static property value
        currentClass.setStaticPropertyValue(propertyName, value);

        // Return the value (for chaining assignments)
        return value;
    }
}
