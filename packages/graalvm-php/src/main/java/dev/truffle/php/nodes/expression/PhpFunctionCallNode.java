package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpFunction;

/**
 * Node for function calls in PHP.
 */
public final class PhpFunctionCallNode extends PhpExpressionNode {

    private final String functionName;

    @Children
    private final PhpExpressionNode[] arguments;

    public PhpFunctionCallNode(String functionName, PhpExpressionNode[] arguments) {
        this.functionName = functionName;
        this.arguments = arguments;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        PhpContext context = PhpContext.get(this);
        PhpFunction function = context.getFunction(functionName);

        if (function == null) {
            throw new RuntimeException("Undefined function: " + functionName);
        }

        // Evaluate arguments
        Object[] argumentValues = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argumentValues[i] = arguments[i].execute(frame);
        }

        // Call the function
        CallTarget callTarget = function.getCallTarget();
        return callTarget.call(argumentValues);
    }
}
