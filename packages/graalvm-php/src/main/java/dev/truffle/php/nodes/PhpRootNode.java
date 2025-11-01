package dev.truffle.php.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.runtime.PhpContext;

/**
 * Root node for PHP program execution.
 * This is the entry point for executing PHP code.
 */
public final class PhpRootNode extends RootNode {

    @Child
    private PhpStatementNode body;

    public PhpRootNode(PhpLanguage language, FrameDescriptor frameDescriptor, PhpStatementNode body) {
        super(language, frameDescriptor);
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Set the global frame so that functions can access global variables
        PhpContext context = PhpContext.get(this);
        context.getGlobalScope().setGlobalFrame(frame);

        body.executeVoid(frame);
        return 0; // PHP scripts return 0 by default
    }

    @Override
    public String getName() {
        return "PHP";
    }
}
