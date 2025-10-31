package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpObject;

/**
 * AST node for reading object properties ($obj->property).
 */
public final class PhpPropertyAccessNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode objectNode;

    private final String propertyName;
    private final String callerClassName; // The name of the class from which this access is being made (null for external)

    // Constructor with caller class name
    public PhpPropertyAccessNode(PhpExpressionNode objectNode, String propertyName, String callerClassName) {
        this.objectNode = objectNode;
        this.propertyName = propertyName;
        this.callerClassName = callerClassName;
    }

    // Legacy constructor for backward compatibility
    public PhpPropertyAccessNode(PhpExpressionNode objectNode, String propertyName, boolean isInternal) {
        this.objectNode = objectNode;
        this.propertyName = propertyName;
        this.callerClassName = null; // Treated as external access
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object objectValue = objectNode.execute(frame);

        if (!(objectValue instanceof PhpObject)) {
            throw new RuntimeException("Trying to get property of non-object");
        }

        PhpObject object = (PhpObject) objectValue;

        // Look up caller class if we have a class name
        PhpClass callerClass = null;
        if (callerClassName != null) {
            PhpContext context = PhpContext.get(this);
            callerClass = context.getClass(callerClassName);
        }

        // Use visibility checking with callerClass
        return object.readProperty(propertyName, callerClass);
    }
}
