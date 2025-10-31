package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpBreakException;

/**
 * Node for break statement in PHP.
 * Throws PhpBreakException to exit from loops.
 */
public final class PhpBreakNode extends PhpStatementNode {

    private final int level;

    public PhpBreakNode(int level) {
        this.level = level;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new PhpBreakException(level);
    }
}
