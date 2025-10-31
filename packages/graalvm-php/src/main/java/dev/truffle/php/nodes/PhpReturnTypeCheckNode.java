package dev.truffle.php.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpTypeHint;

/**
 * Node that wraps a return statement to perform runtime type checking.
 */
public final class PhpReturnTypeCheckNode extends PhpStatementNode {

    @Child private PhpStatementNode originalReturn;
    private final PhpTypeHint typeHint;
    private final String functionName;
    private final String currentClassName;

    public PhpReturnTypeCheckNode(PhpStatementNode originalReturn, PhpTypeHint typeHint, String functionName, String currentClassName) {
        this.originalReturn = originalReturn;
        this.typeHint = typeHint;
        this.functionName = functionName;
        this.currentClassName = currentClassName;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        // This is a bit tricky - we need to intercept the return value
        // For now, we'll let the return execute and validate in PhpFunctionRootNode
        originalReturn.executeVoid(frame);
    }

    public PhpTypeHint getTypeHint() {
        return typeHint;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getCurrentClassName() {
        return currentClassName;
    }
}
