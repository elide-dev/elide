package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for string concatenation in PHP (the . operator).
 */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpConcatNode extends PhpExpressionNode {

    @Child protected PhpExpressionNode left;
    @Child protected PhpExpressionNode right;

    protected PhpConcatNode(PhpExpressionNode left, PhpExpressionNode right) {
        this.left = left;
        this.right = right;
    }

    public static PhpConcatNode create(PhpExpressionNode left, PhpExpressionNode right) {
        return new PhpConcatNodeImpl(left, right);
    }

    static final class PhpConcatNodeImpl extends PhpConcatNode {
        PhpConcatNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
            super(left, right);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object leftVal = left.execute(frame);
            Object rightVal = right.execute(frame);

            // Convert both values to strings and concatenate
            String leftStr = String.valueOf(leftVal);
            String rightStr = String.valueOf(rightVal);

            return leftStr + rightStr;
        }
    }
}
