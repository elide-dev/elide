package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpNodeFactory;
import dev.truffle.php.runtime.PhpReference;

/**
 * Node for PHP postfix increment operator on properties.
 * Syntax: $obj->property++
 *
 * Returns the current value and increments the property.
 */
public final class PhpPropertyPostIncrementNode extends PhpExpressionNode {

    @Child
    private PhpPropertyAccessNode propertyAccessNode;

    public PhpPropertyPostIncrementNode(PhpPropertyAccessNode propertyAccessNode) {
        this.propertyAccessNode = propertyAccessNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Read the current value
        Object currentValue = propertyAccessNode.execute(frame);

        // Unwrap if it's a PhpReference
        Object actualValue = currentValue;
        if (currentValue instanceof PhpReference) {
            actualValue = ((PhpReference) currentValue).getValue();
        }

        // Convert to number and increment
        long currentNum;
        if (actualValue instanceof Long) {
            currentNum = (Long) actualValue;
        } else if (actualValue instanceof Double) {
            currentNum = ((Double) actualValue).longValue();
        } else if (actualValue instanceof String) {
            try {
                currentNum = Long.parseLong((String) actualValue);
            } catch (NumberFormatException e) {
                currentNum = 0;
            }
        } else {
            currentNum = 0;
        }

        long newValue = currentNum + 1;

        // Get the object and property name to write back
        PhpExpressionNode objectNode = propertyAccessNode.getObjectNode();
        String propertyName = propertyAccessNode.getPropertyName();
        String currentClassName = propertyAccessNode.getCurrentClassName();

        // Create a write node to update the property
        PhpExpressionNode writeNode = new PhpPropertyWriteNode(
            objectNode,
            propertyName,
            new PhpIntegerLiteralNode(newValue),
            currentClassName
        );

        // Execute the write
        writeNode.execute(frame);

        // Return the original value (postfix)
        return currentNum;
    }
}
