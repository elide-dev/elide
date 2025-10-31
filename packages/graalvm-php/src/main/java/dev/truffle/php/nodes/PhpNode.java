package dev.truffle.php.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import dev.truffle.php.nodes.types.PhpTypes;

/**
 * Base class for all PHP AST nodes.
 *
 * All executable nodes in PHP extend this class and implement the execute method.
 */
@TypeSystemReference(PhpTypes.class)
public abstract class PhpNode extends Node {

    /**
     * Execute this node and return the result.
     */
    public abstract Object execute(VirtualFrame frame);

    /**
     * Execute this node expecting a long result.
     * Throws UnexpectedResultException if the result is not a long.
     */
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        Object result = execute(frame);
        if (result instanceof Long) {
            return (Long) result;
        }
        throw new UnexpectedResultException(result);
    }

    /**
     * Execute this node expecting a double result.
     * Throws UnexpectedResultException if the result is not a double.
     */
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        Object result = execute(frame);
        if (result instanceof Double) {
            return (Double) result;
        }
        throw new UnexpectedResultException(result);
    }

    /**
     * Execute this node expecting a boolean result.
     * Throws UnexpectedResultException if the result is not a boolean.
     */
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        Object result = execute(frame);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        throw new UnexpectedResultException(result);
    }

    /**
     * Execute this node expecting a String result.
     * Throws UnexpectedResultException if the result is not a String.
     */
    public String executeString(VirtualFrame frame) throws UnexpectedResultException {
        Object result = execute(frame);
        if (result instanceof String) {
            return (String) result;
        }
        throw new UnexpectedResultException(result);
    }
}
