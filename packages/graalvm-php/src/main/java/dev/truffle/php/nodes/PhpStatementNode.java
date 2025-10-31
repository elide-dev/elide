package dev.truffle.php.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Base class for PHP statement nodes.
 *
 * Statements don't return meaningful values in PHP's imperative context,
 * so we provide a specialized execute method that returns void.
 */
public abstract class PhpStatementNode extends PhpNode {

    /**
     * Execute this statement.
     * Statements typically have side effects but don't return values.
     */
    public abstract void executeVoid(VirtualFrame frame);

    @Override
    public final Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return null;
    }
}
