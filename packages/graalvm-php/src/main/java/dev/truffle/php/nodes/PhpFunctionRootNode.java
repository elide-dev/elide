package dev.truffle.php.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.statement.PhpReturnNode;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpReference;

/**
 * Root node for PHP function execution.
 */
public final class PhpFunctionRootNode extends RootNode {

    private final String functionName;
    private final String[] parameterNames;
    private final int[] parameterSlots;
    private final int variadicParamIndex;  // -1 if no variadic parameter
    private final boolean[] referenceParameters;  // Track which parameters are by-reference
    private final boolean returnsByReference;  // Track if function returns by reference

    @Child
    private PhpStatementNode body;

    public PhpFunctionRootNode(
        PhpLanguage language,
        FrameDescriptor frameDescriptor,
        String functionName,
        String[] parameterNames,
        int[] parameterSlots,
        PhpStatementNode body,
        int variadicParamIndex
    ) {
        this(language, frameDescriptor, functionName, parameterNames, parameterSlots, body, variadicParamIndex, null, false);
    }

    public PhpFunctionRootNode(
        PhpLanguage language,
        FrameDescriptor frameDescriptor,
        String functionName,
        String[] parameterNames,
        int[] parameterSlots,
        PhpStatementNode body,
        int variadicParamIndex,
        boolean[] referenceParameters
    ) {
        this(language, frameDescriptor, functionName, parameterNames, parameterSlots, body, variadicParamIndex, referenceParameters, false);
    }

    public PhpFunctionRootNode(
        PhpLanguage language,
        FrameDescriptor frameDescriptor,
        String functionName,
        String[] parameterNames,
        int[] parameterSlots,
        PhpStatementNode body,
        int variadicParamIndex,
        boolean[] referenceParameters,
        boolean returnsByReference
    ) {
        super(language, frameDescriptor);
        this.functionName = functionName;
        this.parameterNames = parameterNames;
        this.parameterSlots = parameterSlots;
        this.body = body;
        this.variadicParamIndex = variadicParamIndex;
        this.referenceParameters = referenceParameters;
        this.returnsByReference = returnsByReference;
    }

    public boolean returnsByReference() {
        return returnsByReference;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Initialize parameters from arguments
        Object[] arguments = frame.getArguments();

        if (variadicParamIndex >= 0) {
            // Handle variadic parameter
            // Assign fixed parameters up to the variadic one
            for (int i = 0; i < variadicParamIndex && i < arguments.length; i++) {
                Object arg = arguments[i];
                // For reference parameters, arg should already be a PhpReference
                frame.setObject(parameterSlots[i], arg);
            }

            // Collect remaining arguments into an array for the variadic parameter
            PhpArray variadicArray = new PhpArray();
            for (int i = variadicParamIndex; i < arguments.length; i++) {
                variadicArray.append(arguments[i]);
            }
            frame.setObject(parameterSlots[variadicParamIndex], variadicArray);
        } else {
            // No variadic parameter - assign normally
            for (int i = 0; i < parameterSlots.length && i < arguments.length; i++) {
                Object arg = arguments[i];
                // For reference parameters, the caller should pass a PhpReference
                // For non-reference parameters, just pass the value
                frame.setObject(parameterSlots[i], arg);
            }
        }

        try {
            body.executeVoid(frame);
            return null; // Functions without explicit return return null
        } catch (PhpReturnNode.PhpReturnException e) {
            return e.getResult();
        }
    }

    @Override
    public String getName() {
        return functionName;
    }
}
