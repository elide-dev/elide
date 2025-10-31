package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpObject;

/**
 * AST node for writing object properties ($obj->property = value).
 */
public final class PhpPropertyWriteNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode objectNode;

    @Child
    private PhpExpressionNode valueNode;

    private final String propertyName;
    private final boolean isInternal; // true if accessed from within methods ($this->prop)

    public PhpPropertyWriteNode(PhpExpressionNode objectNode, String propertyName,
                                PhpExpressionNode valueNode, boolean isInternal) {
        this.objectNode = objectNode;
        this.propertyName = propertyName;
        this.valueNode = valueNode;
        this.isInternal = isInternal;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object objectValue = objectNode.execute(frame);

        if (!(objectValue instanceof PhpObject)) {
            throw new RuntimeException("Trying to set property of non-object");
        }

        PhpObject object = (PhpObject) objectValue;
        Object value = valueNode.execute(frame);

        // Use internal access if this is from within a method (bypasses visibility)
        if (isInternal) {
            object.writePropertyInternal(propertyName, value);
        } else {
            object.writeProperty(propertyName, value);
        }

        return value;
    }
}
