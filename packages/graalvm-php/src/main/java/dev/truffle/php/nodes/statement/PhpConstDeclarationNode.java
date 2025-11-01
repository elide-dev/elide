package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpContext;

/**
 * Statement node for defining a constant using the `const` keyword.
 * Syntax: const CONSTANT_NAME = value;
 */
public final class PhpConstDeclarationNode extends PhpStatementNode {

    private final String constantName;
    @Child private PhpExpressionNode valueNode;

    public PhpConstDeclarationNode(String constantName, PhpExpressionNode valueNode) {
        this.constantName = constantName;
        this.valueNode = valueNode;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        // Evaluate the value expression
        Object value = valueNode.execute(frame);

        // Define the constant in the context
        PhpContext context = PhpContext.get(this);
        context.defineConstant(constantName, value);
    }
}
