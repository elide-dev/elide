package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpReference;

/**
 * Node for array access in PHP (e.g., $arr[0] or $arr["key"]).
 */
public final class PhpArrayAccessNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode arrayNode;

    @Child
    private PhpExpressionNode indexNode;

    public PhpArrayAccessNode(PhpExpressionNode arrayNode, PhpExpressionNode indexNode) {
        this.arrayNode = arrayNode;
        this.indexNode = indexNode;
    }

    public PhpExpressionNode getArrayNode() {
        return arrayNode;
    }

    public PhpExpressionNode getIndexNode() {
        return indexNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object arrayObj = arrayNode.execute(frame);
        Object index = indexNode.execute(frame);

        if (!(arrayObj instanceof PhpArray)) {
            throw new RuntimeException("Cannot use [] on non-array");
        }

        PhpArray array = (PhpArray) arrayObj;
        Object value = array.get(index);

        // Unwrap PhpReference if present
        // This happens when array elements are stored as references
        // (e.g., after foreach with &$value)
        if (value instanceof PhpReference) {
            return ((PhpReference) value).getValue();
        }

        return value;
    }
}
