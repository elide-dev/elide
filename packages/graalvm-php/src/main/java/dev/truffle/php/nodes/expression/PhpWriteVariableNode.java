package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for writing a value to a variable.
 * In PHP, assignments are expressions that return the assigned value.
 */
@NodeChild("value")
@NodeField(name = "slot", type = int.class)
public abstract class PhpWriteVariableNode extends PhpExpressionNode {

    @Child protected PhpExpressionNode value;

    protected PhpWriteVariableNode(PhpExpressionNode value) {
        this.value = value;
    }

    protected abstract int getSlot();

    public static PhpWriteVariableNode create(PhpExpressionNode value, int slot) {
        return new PhpWriteVariableNodeImpl(value, slot);
    }

    static final class PhpWriteVariableNodeImpl extends PhpWriteVariableNode {
        private final int slot;

        PhpWriteVariableNodeImpl(PhpExpressionNode value, int slot) {
            super(value);
            this.slot = slot;
        }

        @Override
        protected int getSlot() {
            return slot;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object val = value.execute(frame);
            frame.setObject(slot, val);
            return val;
        }
    }
}
