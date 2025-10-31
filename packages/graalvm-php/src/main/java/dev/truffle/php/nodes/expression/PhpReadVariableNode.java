package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for reading a variable value.
 * In PHP, variables are prefixed with $.
 */
@NodeField(name = "slot", type = int.class)
public abstract class PhpReadVariableNode extends PhpExpressionNode {

    protected abstract int getSlot();

    public static PhpReadVariableNode create(int slot) {
        return new PhpReadVariableNodeImpl(slot);
    }

    static final class PhpReadVariableNodeImpl extends PhpReadVariableNode {
        private final int slot;

        PhpReadVariableNodeImpl(int slot) {
            this.slot = slot;
        }

        @Override
        protected int getSlot() {
            return slot;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                return frame.getObject(slot);
            } catch (FrameSlotTypeException e) {
                return null;
            }
        }
    }
}
