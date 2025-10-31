package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;

/**
 * AST node for class definition statements.
 * Registers the class in the context during execution.
 */
public final class PhpClassNode extends PhpStatementNode {

    private final PhpClass phpClass;

    public PhpClassNode(PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        // Register the class in the context
        PhpContext context = PhpContext.get(this);
        context.registerClass(phpClass);
    }
}
