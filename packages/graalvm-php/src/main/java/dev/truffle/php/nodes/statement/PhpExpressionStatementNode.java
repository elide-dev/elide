package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpStatementNode;

/**
 * Wrapper node that turns an expression into a statement.
 * Used for statements like "$x = 5;" where the assignment is an expression.
 */
public final class PhpExpressionStatementNode extends PhpStatementNode {

    @Child
    private PhpExpressionNode expression;

    public PhpExpressionStatementNode(PhpExpressionNode expression) {
        this.expression = expression;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        expression.execute(frame);
    }
}
