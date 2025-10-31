package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpObject;

/**
 * AST node for reading object properties ($obj->property).
 */
public final class PhpPropertyAccessNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode objectNode;

    private final String propertyName;
    private final boolean isInternal; // true if accessed from within methods ($this->prop)

    public PhpPropertyAccessNode(PhpExpressionNode objectNode, String propertyName, boolean isInternal) {
        this.objectNode = objectNode;
        this.propertyName = propertyName;
        this.isInternal = isInternal;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object objectValue = objectNode.execute(frame);

        if (!(objectValue instanceof PhpObject)) {
            throw new RuntimeException("Trying to get property of non-object");
        }

        PhpObject object = (PhpObject) objectValue;

        // Use internal access if this is from within a method (bypasses visibility)
        if (isInternal) {
            return object.readPropertyInternal(propertyName);
        } else {
            return object.readProperty(propertyName);
        }
    }
}
