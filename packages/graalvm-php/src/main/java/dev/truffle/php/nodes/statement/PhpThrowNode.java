package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpObject;
import dev.truffle.php.runtime.PhpThrowableException;

/**
 * Node for throw statements in PHP.
 * Evaluates the exception expression and throws a PhpThrowableException
 * that propagates through the call stack until caught by a try/catch block.
 */
public final class PhpThrowNode extends PhpStatementNode {

    @Child
    private PhpExpressionNode exceptionNode;

    public PhpThrowNode(PhpExpressionNode exceptionNode) {
        this.exceptionNode = exceptionNode;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Object exception = exceptionNode.execute(frame);

        if (!(exception instanceof PhpObject)) {
            throw new RuntimeException("Can only throw objects, got: " + exception.getClass().getSimpleName());
        }

        throw new PhpThrowableException((PhpObject) exception);
    }
}
