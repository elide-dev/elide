package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.types.PhpTypes;

/**
 * AST node for ternary conditional operator (condition ? trueValue : falseValue).
 * Implements short-circuit evaluation - only the selected branch is evaluated.
 */
@NodeInfo(shortName = "?:")
public final class PhpTernaryNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode conditionNode;

    @Child
    private PhpExpressionNode trueNode;

    @Child
    private PhpExpressionNode falseNode;

    public PhpTernaryNode(PhpExpressionNode conditionNode, PhpExpressionNode trueNode, PhpExpressionNode falseNode) {
        this.conditionNode = conditionNode;
        this.trueNode = trueNode;
        this.falseNode = falseNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object conditionValue = conditionNode.execute(frame);

        // Convert condition to boolean using PHP truthiness rules
        boolean condition = PhpTypes.toBoolean(conditionValue);

        if (condition) {
            return trueNode.execute(frame);
        } else {
            return falseNode.execute(frame);
        }
    }
}
