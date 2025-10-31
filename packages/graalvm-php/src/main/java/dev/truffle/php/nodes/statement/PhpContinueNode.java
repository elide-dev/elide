package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpContinueException;

/**
 * Node for continue statement in PHP.
 * Throws PhpContinueException to skip to the next iteration of loops.
 */
public final class PhpContinueNode extends PhpStatementNode {

    private final int level;

    public PhpContinueNode(int level) {
        this.level = level;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new PhpContinueException(level);
    }
}
