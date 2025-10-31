package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.types.PhpTypes;

/**
 * AST node for empty() language construct.
 * Checks if a variable is empty according to PHP's empty rules.
 * Returns true if the variable is unset or has an "empty" value.
 *
 * Empty values in PHP:
 * - null
 * - false
 * - 0 (integer)
 * - 0.0 (float)
 * - "" (empty string)
 * - "0" (string containing zero)
 * - empty array
 */
public final class PhpEmptyNode extends PhpExpressionNode {

    private final int variableSlot;

    public PhpEmptyNode(int variableSlot) {
        this.variableSlot = variableSlot;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            Object value = frame.getObject(variableSlot);
            return PhpTypes.isEmpty(value);
        } catch (Exception e) {
            // Variable not initialized - counts as empty
            return true;
        }
    }
}
