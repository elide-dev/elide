package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpBreakException;
import dev.truffle.php.runtime.PhpContinueException;

/**
 * Node for while loops in PHP.
 */
public final class PhpWhileNode extends PhpStatementNode {

    @Child
    private LoopNode loopNode;

    public PhpWhileNode(PhpExpressionNode condition, PhpStatementNode body) {
        this.loopNode = Truffle.getRuntime().createLoopNode(new PhpWhileRepeatingNode(condition, body));
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        loopNode.execute(frame);
    }

    private static final class PhpWhileRepeatingNode extends Node implements RepeatingNode {

        @Child
        private PhpExpressionNode condition;

        @Child
        private PhpStatementNode body;

        PhpWhileRepeatingNode(PhpExpressionNode condition, PhpStatementNode body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (!evaluateConditionAsBoolean(frame)) {
                return false;
            }

            try {
                body.executeVoid(frame);
            } catch (PhpContinueException e) {
                // Continue to next iteration
                return true;
            } catch (PhpBreakException e) {
                // Exit loop
                return false;
            }
            return true;
        }

        private boolean evaluateConditionAsBoolean(VirtualFrame frame) {
            Object value = condition.execute(frame);
            return isTruthy(value);
        }

        private boolean isTruthy(Object value) {
            if (value == null) {
                return false;
            } else if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof Long) {
                return (Long) value != 0;
            } else if (value instanceof Double) {
                return (Double) value != 0.0;
            } else if (value instanceof String) {
                String str = (String) value;
                return !str.isEmpty() && !str.equals("0");
            }
            return true;
        }
    }
}
