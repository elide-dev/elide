package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node representing a spread operator argument in function calls (...$expr).
 * The execute method returns the expression value, and the isSpread flag
 * indicates this argument should be unpacked.
 */
public final class PhpSpreadArgumentNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode expression;

    public PhpSpreadArgumentNode(PhpExpressionNode expression) {
        this.expression = expression;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return expression.execute(frame);
    }

    public boolean isSpread() {
        return true;
    }

    public PhpExpressionNode getExpression() {
        return expression;
    }
}
