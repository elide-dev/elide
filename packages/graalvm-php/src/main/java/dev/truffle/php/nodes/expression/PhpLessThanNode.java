package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for less-than comparison (<) in PHP.
 */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpLessThanNode extends PhpExpressionNode {

    @Child protected PhpExpressionNode left;
    @Child protected PhpExpressionNode right;

    protected PhpLessThanNode(PhpExpressionNode left, PhpExpressionNode right) {
        this.left = left;
        this.right = right;
    }

    public static PhpLessThanNode create(PhpExpressionNode left, PhpExpressionNode right) {
        return new PhpLessThanNodeImpl(left, right);
    }

    static final class PhpLessThanNodeImpl extends PhpLessThanNode {
        PhpLessThanNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
            super(left, right);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object leftVal = left.execute(frame);
            Object rightVal = right.execute(frame);

            if (leftVal instanceof Long && rightVal instanceof Long) {
                return (Long) leftVal < (Long) rightVal;
            } else if (leftVal instanceof Double && rightVal instanceof Double) {
                return (Double) leftVal < (Double) rightVal;
            } else if (leftVal instanceof Long && rightVal instanceof Double) {
                return (Long) leftVal < (Double) rightVal;
            } else if (leftVal instanceof Double && rightVal instanceof Long) {
                return (Double) leftVal < (Long) rightVal;
            } else if (leftVal instanceof String && rightVal instanceof String) {
                return ((String) leftVal).compareTo((String) rightVal) < 0;
            }

            return false;
        }
    }
}
