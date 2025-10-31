package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * AST node for string interpolation.
 * Evaluates a series of string literals and expressions, concatenating them together.
 *
 * Example: "Hello $name!" is parsed as:
 * - Literal "Hello "
 * - Variable $name
 * - Literal "!"
 */
public final class PhpStringInterpolationNode extends PhpExpressionNode {

    @Children
    private final PhpExpressionNode[] parts;

    public PhpStringInterpolationNode(PhpExpressionNode[] parts) {
        this.parts = parts;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        if (parts.length == 0) {
            return "";
        }

        if (parts.length == 1) {
            return toString(parts[0].execute(frame));
        }

        // Multiple parts - build string
        StringBuilder result = new StringBuilder();
        for (PhpExpressionNode part : parts) {
            Object value = part.execute(frame);
            result.append(toString(value));
        }
        return result.toString();
    }

    @TruffleBoundary
    private String toString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Long || value instanceof Double || value instanceof Boolean) {
            return value.toString();
        }
        // For objects and arrays, use PHP's string conversion
        return value.toString();
    }
}
