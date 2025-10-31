package dev.truffle.php.runtime;

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * Exception for continue statement in PHP.
 * Used to skip to the next iteration of loops (while, for, foreach).
 */
public final class PhpContinueException extends ControlFlowException {

    private final int level;

    public PhpContinueException(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
