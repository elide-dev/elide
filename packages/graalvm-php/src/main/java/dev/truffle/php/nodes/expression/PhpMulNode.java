package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for multiplication operation in PHP.
 */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpMulNode extends PhpExpressionNode {

    @Child protected PhpExpressionNode left;
    @Child protected PhpExpressionNode right;

    protected PhpMulNode(PhpExpressionNode left, PhpExpressionNode right) {
        this.left = left;
        this.right = right;
    }

    public static PhpMulNode create(PhpExpressionNode left, PhpExpressionNode right) {
        return new PhpMulNodeImpl(left, right);
    }

    static final class PhpMulNodeImpl extends PhpMulNode {
        PhpMulNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
            super(left, right);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object leftVal = left.execute(frame);
            Object rightVal = right.execute(frame);

            if (leftVal instanceof Long && rightVal instanceof Long) {
                try {
                    return Math.multiplyExact((Long) leftVal, (Long) rightVal);
                } catch (ArithmeticException e) {
                    return (double) (Long) leftVal * (double) (Long) rightVal;
                }
            } else if (leftVal instanceof Double && rightVal instanceof Double) {
                return (Double) leftVal * (Double) rightVal;
            } else if (leftVal instanceof Long && rightVal instanceof Double) {
                return (Long) leftVal * (Double) rightVal;
            } else if (leftVal instanceof Double && rightVal instanceof Long) {
                return (Double) leftVal * (Long) rightVal;
            }

            return 0L;
        }
    }
}
