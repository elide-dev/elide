package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for division operation in PHP.
 * PHP division always returns a float if there's a remainder.
 */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpDivNode extends PhpExpressionNode {

    @Child protected PhpExpressionNode left;
    @Child protected PhpExpressionNode right;

    protected PhpDivNode(PhpExpressionNode left, PhpExpressionNode right) {
        this.left = left;
        this.right = right;
    }

    public static PhpDivNode create(PhpExpressionNode left, PhpExpressionNode right) {
        return new PhpDivNodeImpl(left, right);
    }

    static final class PhpDivNodeImpl extends PhpDivNode {
        PhpDivNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
            super(left, right);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object leftVal = left.execute(frame);
            Object rightVal = right.execute(frame);

            if (leftVal instanceof Long && rightVal instanceof Long) {
                long leftLong = (Long) leftVal;
                long rightLong = (Long) rightVal;
                if (rightLong == 0) {
                    return Double.POSITIVE_INFINITY;
                }
                if (leftLong % rightLong == 0) {
                    return leftLong / rightLong;
                }
                return (double) leftLong / (double) rightLong;
            } else if (leftVal instanceof Double && rightVal instanceof Double) {
                return (Double) leftVal / (Double) rightVal;
            } else if (leftVal instanceof Long && rightVal instanceof Double) {
                return (Long) leftVal / (Double) rightVal;
            } else if (leftVal instanceof Double && rightVal instanceof Long) {
                return (Double) leftVal / (Long) rightVal;
            }

            return Double.NaN;
        }
    }
}
