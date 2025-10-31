package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for addition operation in PHP.
 * Uses Truffle DSL for automatic specialization and type handling.
 */
@NodeChild("left")
@NodeChild("right")
public abstract class PhpAddNode extends PhpExpressionNode {

    @Child protected PhpExpressionNode left;
    @Child protected PhpExpressionNode right;

    protected PhpAddNode(PhpExpressionNode left, PhpExpressionNode right) {
        this.left = left;
        this.right = right;
    }

    public static PhpAddNode create(PhpExpressionNode left, PhpExpressionNode right) {
        return new PhpAddNodeImpl(left, right);
    }

    static final class PhpAddNodeImpl extends PhpAddNode {
        PhpAddNodeImpl(PhpExpressionNode left, PhpExpressionNode right) {
            super(left, right);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object leftVal = left.execute(frame);
            Object rightVal = right.execute(frame);

            if (leftVal instanceof Long && rightVal instanceof Long) {
                try {
                    return Math.addExact((Long) leftVal, (Long) rightVal);
                } catch (ArithmeticException e) {
                    return (double) (Long) leftVal + (double) (Long) rightVal;
                }
            } else if (leftVal instanceof Double && rightVal instanceof Double) {
                return (Double) leftVal + (Double) rightVal;
            } else if (leftVal instanceof Long && rightVal instanceof Double) {
                return (Long) leftVal + (Double) rightVal;
            } else if (leftVal instanceof Double && rightVal instanceof Long) {
                return (Double) leftVal + (Long) rightVal;
            }

            return 0L;
        }
    }
}
