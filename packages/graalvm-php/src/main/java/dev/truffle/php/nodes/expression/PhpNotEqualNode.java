package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for not-equal comparison (!=) in PHP.
 */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpNotEqualNode extends PhpExpressionNode {

    @Child protected PhpExpressionNode left;
    @Child protected PhpExpressionNode right;

    protected PhpNotEqualNode(PhpExpressionNode left, PhpExpressionNode right) {
        this.left = left;
        this.right = right;
    }

    public static PhpNotEqualNode create(PhpExpressionNode left, PhpExpressionNode right) {
        return new PhpNotEqualNodeImpl(left, right);
    }

    static final class PhpNotEqualNodeImpl extends PhpNotEqualNode {
        PhpNotEqualNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
            super(left, right);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object leftVal = left.execute(frame);
            Object rightVal = right.execute(frame);

            if (leftVal == null && rightVal == null) {
                return false;
            } else if (leftVal == null || rightVal == null) {
                return true;
            } else if (leftVal instanceof Long && rightVal instanceof Long) {
                return !leftVal.equals(rightVal);
            } else if (leftVal instanceof Double && rightVal instanceof Double) {
                return !leftVal.equals(rightVal);
            } else if (leftVal instanceof Long && rightVal instanceof Double) {
                return ((Long) leftVal).doubleValue() != (Double) rightVal;
            } else if (leftVal instanceof Double && rightVal instanceof Long) {
                return (Double) leftVal != ((Long) rightVal).doubleValue();
            } else if (leftVal instanceof Boolean && rightVal instanceof Boolean) {
                return !leftVal.equals(rightVal);
            } else if (leftVal instanceof String && rightVal instanceof String) {
                return !leftVal.equals(rightVal);
            }

            return !leftVal.equals(rightVal);
        }
    }
}
