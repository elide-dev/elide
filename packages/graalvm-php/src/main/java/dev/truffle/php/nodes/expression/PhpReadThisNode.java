package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * AST node for reading the $this variable in methods.
 */
@NodeField(name = "slot", type = int.class)
public abstract class PhpReadThisNode extends PhpExpressionNode {

    protected abstract int getSlot();

    @Specialization
    protected Object readThis(VirtualFrame frame) {
        return frame.getObject(getSlot());
    }

    public static PhpReadThisNode create(int slot) {
        return PhpReadThisNodeGen.create(slot);
    }
}
