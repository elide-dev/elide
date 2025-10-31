package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpFunction;

/**
 * Node for function definitions in PHP.
 * Registers the function in the context for later calls.
 */
public final class PhpFunctionNode extends PhpStatementNode {

    private final String functionName;
    private final PhpFunction function;

    public PhpFunctionNode(String functionName, PhpFunction function) {
        this.functionName = functionName;
        this.function = function;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        // Function definitions in PHP are registered at parse time, not runtime
        // This node is mainly for structural purposes
        // The actual registration happens in the parser
    }

    public String getFunctionName() {
        return functionName;
    }

    public PhpFunction getFunction() {
        return function;
    }
}
