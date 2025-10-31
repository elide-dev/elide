package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpArray;

/**
 * Node for array literals in PHP.
 * Supports both indexed and associative arrays.
 */
public final class PhpArrayLiteralNode extends PhpExpressionNode {

    @Children
    private final PhpExpressionNode[] keys;

    @Children
    private final PhpExpressionNode[] values;

    private final boolean isAssociative;

    public PhpArrayLiteralNode(PhpExpressionNode[] values) {
        this.keys = null;
        this.values = values;
        this.isAssociative = false;
    }

    public PhpArrayLiteralNode(PhpExpressionNode[] keys, PhpExpressionNode[] values) {
        this.keys = keys;
        this.values = values;
        this.isAssociative = true;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        PhpArray array = new PhpArray();

        if (isAssociative) {
            for (int i = 0; i < keys.length; i++) {
                Object key = keys[i].execute(frame);
                Object value = values[i].execute(frame);
                array.put(key, value);
            }
        } else {
            for (int i = 0; i < values.length; i++) {
                Object value = values[i].execute(frame);
                array.append(value);
            }
        }

        return array;
    }
}
