package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpContext;

/**
 * Node for the echo statement in PHP.
 * Echo can output one or more expressions, separated by commas.
 */
public final class PhpEchoNode extends PhpStatementNode {

    @Children
    private final PhpExpressionNode[] expressions;

    public PhpEchoNode(PhpExpressionNode[] expressions) {
        this.expressions = expressions;
    }

    @Override
    @ExplodeLoop
    public void executeVoid(VirtualFrame frame) {
        PhpContext context = PhpContext.get(this);

        for (PhpExpressionNode expr : expressions) {
            Object value = expr.execute(frame);
            String output = convertToString(value);
            context.getOutput().print(output);
        }
        context.getOutput().flush();
    }

    /**
     * Convert a PHP value to its string representation.
     */
    private String convertToString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "";
        }
        return value.toString();
    }
}
