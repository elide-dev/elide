package dev.truffle.php.runtime;

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * Exception for break statement in PHP.
 * Used to exit from loops (while, for, foreach).
 */
public final class PhpBreakException extends ControlFlowException {

    private final int level;

    public PhpBreakException(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
