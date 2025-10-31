package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * AST node for null coalescing operator (leftExpr ?? rightExpr).
 * Returns leftExpr if it is not null, otherwise returns rightExpr.
 * Implements short-circuit evaluation - rightExpr is only evaluated if leftExpr is null.
 */
@NodeInfo(shortName = "??")
public final class PhpNullCoalescingNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode leftNode;

    @Child
    private PhpExpressionNode rightNode;

    public PhpNullCoalescingNode(PhpExpressionNode leftNode, PhpExpressionNode rightNode) {
        this.leftNode = leftNode;
        this.rightNode = rightNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object leftValue = leftNode.execute(frame);

        // If left value is not null, return it
        if (leftValue != null) {
            return leftValue;
        }

        // Otherwise, evaluate and return right value
        return rightNode.execute(frame);
    }
}
