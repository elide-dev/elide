package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpStatementNode;

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
        Object value = valueNode != null ? valueNode.execute(frame) : null;
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
