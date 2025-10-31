package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for logical AND (&&) in PHP.
 * Uses short-circuit evaluation.
 */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpLogicalAndNode extends PhpExpressionNode {

    @Child protected PhpExpressionNode left;
    @Child protected PhpExpressionNode right;

    protected PhpLogicalAndNode(PhpExpressionNode left, PhpExpressionNode right) {
        this.left = left;
        this.right = right;
    }

    public static PhpLogicalAndNode create(PhpExpressionNode left, PhpExpressionNode right) {
        return new PhpLogicalAndNodeImpl(left, right);
    }

    static final class PhpLogicalAndNodeImpl extends PhpLogicalAndNode {
        PhpLogicalAndNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
            super(left, right);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object leftVal = left.execute(frame);

            // Short-circuit: if left is false, don't evaluate right
            if (!isTruthy(leftVal)) {
                return false;
            }

            Object rightVal = right.execute(frame);
            return isTruthy(rightVal);
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
