package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClosure;

/**
 * Node that creates a PHP closure.
 * Evaluates captured variable expressions and creates a PhpClosure object.
 */
public final class PhpClosureNode extends PhpExpressionNode {

    private final CallTarget callTarget;
    private final String[] parameterNames;

    @Children
    private final PhpExpressionNode[] capturedExpressions;  // Expressions to evaluate for captured variables

    public PhpClosureNode(
        CallTarget callTarget,
        String[] parameterNames,
        PhpExpressionNode[] capturedExpressions
    ) {
        this.callTarget = callTarget;
        this.parameterNames = parameterNames;
        this.capturedExpressions = capturedExpressions;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        // Evaluate all captured variable expressions
        Object[] capturedValues = new Object[capturedExpressions.length];
        for (int i = 0; i < capturedExpressions.length; i++) {
            capturedValues[i] = capturedExpressions[i].execute(frame);
        }

        return new PhpClosure(callTarget, parameterNames, capturedValues);
    }
}
