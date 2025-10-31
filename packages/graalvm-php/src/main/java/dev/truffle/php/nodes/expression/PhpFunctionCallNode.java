package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpFunction;

import java.util.ArrayList;
import java.util.List;

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

        // Check for built-in functions first
        CallTarget builtinCallTarget = context.getBuiltin(functionName);
        CallTarget callTarget;

        if (builtinCallTarget != null) {
            callTarget = builtinCallTarget;
        } else {
            // Check for user-defined functions
            PhpFunction function = context.getFunction(functionName);
            if (function == null) {
                throw new RuntimeException("Undefined function: " + functionName);
            }
            callTarget = function.getCallTarget();
        }

        // Evaluate arguments and expand spread operators
        List<Object> expandedArgs = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            PhpExpressionNode argNode = arguments[i];

            // Check if this is a spread argument
            if (argNode instanceof PhpSpreadArgumentNode) {
                // Execute the expression and unpack it if it's an array
                Object value = argNode.execute(frame);
                if (value instanceof PhpArray) {
                    PhpArray array = (PhpArray) value;
                    // Add all elements from the array
                    List<Object> keys = array.keys();
                    for (Object key : keys) {
                        expandedArgs.add(array.get(key));
                    }
                } else {
                    throw new RuntimeException("Cannot use spread operator on non-array value in function " + functionName);
                }
            } else {
                // Regular argument
                expandedArgs.add(argNode.execute(frame));
            }
        }

        // Call the function with expanded arguments
        return callTarget.call(expandedArgs.toArray());
    }
}
