package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.RootNode;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpFunctionRootNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.nodes.expression.PhpReadVariableNode;

/**
 * Node for return statements in PHP.
 * Throws a control flow exception to exit the function.
 */
public final class PhpReturnNode extends PhpStatementNode {

    @Child
    private PhpExpressionNode valueNode;

    public PhpReturnNode(PhpExpressionNode valueNode) {
        this.valueNode = valueNode;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Object value = null;

        if (valueNode != null) {
            // Check if we're in a function that returns by reference
            RootNode rootNode = getRootNode();
            boolean returnsByReference = rootNode instanceof PhpFunctionRootNode &&
                                         ((PhpFunctionRootNode) rootNode).returnsByReference();

            if (returnsByReference && valueNode instanceof PhpReadVariableNode) {
                // For return-by-reference, we need to return the PhpReference object itself
                // instead of the unwrapped value
                int slot = ((PhpReadVariableNode) valueNode).getSlot();
                value = frame.getObject(slot);
            } else {
                // Normal return - evaluate the expression (which unwraps references)
                value = valueNode.execute(frame);
            }
        }

        throw new PhpReturnException(value);
    }

    /**
     * Exception used to implement return statement control flow.
     */
    public static final class PhpReturnException extends ControlFlowException {
        private final Object result;

        public PhpReturnException(Object result) {
            this.result = result;
        }

        public Object getResult() {
            return result;
        }
    }
}
