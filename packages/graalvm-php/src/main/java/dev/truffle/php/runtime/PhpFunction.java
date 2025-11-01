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
    private final boolean[] referenceParameters;  // Track which parameters are by-reference

    public PhpFunction(String name, CallTarget callTarget, int parameterCount, String[] parameterNames) {
        this(name, callTarget, parameterCount, parameterNames, null);
    }

    public PhpFunction(String name, CallTarget callTarget, int parameterCount, String[] parameterNames, boolean[] referenceParameters) {
        this.name = name;
        this.callTarget = callTarget;
        this.parameterCount = parameterCount;
        this.parameterNames = parameterNames;
        this.referenceParameters = referenceParameters;
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

    public boolean[] getReferenceParameters() {
        return referenceParameters;
    }

    public boolean isReferenceParameter(int index) {
        return referenceParameters != null && index < referenceParameters.length && referenceParameters[index];
    }
}
