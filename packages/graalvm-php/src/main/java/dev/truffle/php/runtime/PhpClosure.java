package dev.truffle.php.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * Represents a PHP closure (anonymous function).
 * Closures can capture variables from their enclosing scope.
 */
@ExportLibrary(InteropLibrary.class)
public final class PhpClosure implements TruffleObject {

    private final CallTarget callTarget;
    private final String[] parameterNames;
    private final int parameterCount;
    private final Object[] capturedValues;  // Values captured via use clause

    public PhpClosure(CallTarget callTarget, String[] parameterNames, Object[] capturedValues) {
        this.callTarget = callTarget;
        this.parameterNames = parameterNames;
        this.parameterCount = parameterNames.length;
        this.capturedValues = capturedValues;
    }

    public CallTarget getCallTarget() {
        return callTarget;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public Object[] getCapturedValues() {
        return capturedValues;
    }

    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments) {
        return callTarget.call(arguments);
    }

    @Override
    public String toString() {
        return "Closure";
    }
}
