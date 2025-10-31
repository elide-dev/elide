package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpTrait;

/**
 * AST node for trait definition statements.
 * Registers the trait in the context during execution.
 *
 * <p>Traits define reusable code components that can be composed into classes.
 * This node is created during parsing and executes to register the trait
 * in the PHP context, making it available for use in class definitions.</p>
 */
public final class PhpTraitNode extends PhpStatementNode {

    private final PhpTrait phpTrait;

    public PhpTraitNode(PhpTrait phpTrait) {
        this.phpTrait = phpTrait;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        // Register the trait in the context
        PhpContext context = PhpContext.get(this);
        context.registerTrait(phpTrait);
    }

    /**
     * Get the trait associated with this node.
     * Used by the parser during trait composition.
     */
    public PhpTrait getPhpTrait() {
        return phpTrait;
    }
}
