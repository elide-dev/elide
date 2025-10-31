package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpStatementNode;

/**
 * Node representing a block of statements.
 * Executes multiple statements in sequence.
 */
public final class PhpBlockNode extends PhpStatementNode {

    @Children
    private final PhpStatementNode[] statements;

    public PhpBlockNode(PhpStatementNode[] statements) {
        this.statements = statements;
    }

    @Override
    @ExplodeLoop
    public void executeVoid(VirtualFrame frame) {
        for (PhpStatementNode statement : statements) {
            statement.executeVoid(frame);
        }
    }
}
