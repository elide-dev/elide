package dev.truffle.php.runtime;

import com.oracle.truffle.api.CallTarget;

/**
 * Represents a PHP function.
 */
public final class PhpFunction {

    private final String name;
    private final CallTarget callTarget;
    private final int parameterCount;
    private final String[] parameterNames;

    public PhpFunction(String name, CallTarget callTarget, int parameterCount, String[] parameterNames) {
        this.name = name;
        this.callTarget = callTarget;
        this.parameterCount = parameterCount;
        this.parameterNames = parameterNames;
    }

    public String getName() {
        return name;
    }

    public CallTarget getCallTarget() {
        return callTarget;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }
}
