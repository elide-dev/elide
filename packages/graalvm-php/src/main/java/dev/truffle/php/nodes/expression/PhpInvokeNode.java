package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpClosure;
import dev.truffle.php.runtime.PhpObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Node for invoking objects and closures as functions ($obj(), $closure()).
 * Handles PhpClosure objects and objects with __invoke magic method.
 */
public final class PhpInvokeNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode callableNode;

    @Children
    private final PhpExpressionNode[] argumentNodes;

    public PhpInvokeNode(PhpExpressionNode callableNode, PhpExpressionNode[] argumentNodes) {
        this.callableNode = callableNode;
        this.argumentNodes = argumentNodes;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        Object callableValue = callableNode.execute(frame);

        // Check if it's a PhpClosure
        if (callableValue instanceof PhpClosure) {
            PhpClosure closure = (PhpClosure) callableValue;
            CallTarget callTarget = closure.getCallTarget();

            // Prepare arguments: captured values first, then call arguments
            Object[] capturedValues = closure.getCapturedValues();
            List<Object> allArgs = new ArrayList<>();

            // Add captured values
            for (Object captured : capturedValues) {
                allArgs.add(captured);
            }

            // Evaluate and add call arguments, handling spread operators
            for (int i = 0; i < argumentNodes.length; i++) {
                PhpExpressionNode argNode = argumentNodes[i];

                // Check if this is a spread argument
                if (argNode instanceof PhpSpreadArgumentNode) {
                    Object value = argNode.execute(frame);
                    if (value instanceof PhpArray) {
                        PhpArray array = (PhpArray) value;
                        List<Object> keys = array.keys();
                        for (Object key : keys) {
                            allArgs.add(array.get(key));
                        }
                    } else {
                        throw new RuntimeException("Cannot use spread operator on non-array value in closure call");
                    }
                } else {
                    allArgs.add(argNode.execute(frame));
                }
            }

            // Call the closure
            return callTarget.call(allArgs.toArray());
        }

        // Check if it's a PhpObject with __invoke method
        if (callableValue instanceof PhpObject) {
            PhpObject object = (PhpObject) callableValue;
            PhpClass phpClass = object.getPhpClass();

            if (phpClass.hasMethod("__invoke")) {
                PhpClass.MethodMetadata invokeMethod = phpClass.getMethod("__invoke");
                CallTarget callTarget = invokeMethod.getCallTarget();

                // Prepare arguments: $this is first argument, then method parameters
                Object[] args = new Object[argumentNodes.length + 1];
                args[0] = object; // First argument is $this
                for (int i = 0; i < argumentNodes.length; i++) {
                    args[i + 1] = argumentNodes[i].execute(frame);
                }

                // Call the __invoke method
                return callTarget.call(args);
            } else {
                throw new RuntimeException("Object of class " + phpClass.getName() + " is not callable (missing __invoke method)");
            }
        }

        throw new RuntimeException("Cannot call non-object as function");
    }
}
