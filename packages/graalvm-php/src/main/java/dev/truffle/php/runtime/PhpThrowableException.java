package dev.truffle.php.runtime;

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * Exception for PHP throw statement.
 * Carries a PHP exception object and propagates through the call stack
 * until caught by a matching catch block.
 */
public final class PhpThrowableException extends ControlFlowException {

    private final PhpObject exceptionObject;

    public PhpThrowableException(PhpObject exceptionObject) {
        this.exceptionObject = exceptionObject;
    }

    public PhpObject getExceptionObject() {
        return exceptionObject;
    }
}
