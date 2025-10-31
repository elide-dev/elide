package dev.truffle.php.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import dev.truffle.php.PhpLanguage;

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
        body.executeVoid(frame);
        return 0; // PHP scripts return 0 by default
    }

    @Override
    public String getName() {
        return "PHP";
    }
}
