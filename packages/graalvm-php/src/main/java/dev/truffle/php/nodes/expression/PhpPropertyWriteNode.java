package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpContext;
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
    private final String callerClassName; // The name of the class from which this access is being made (null for external)

    // Constructor with caller class name
    public PhpPropertyWriteNode(PhpExpressionNode objectNode, String propertyName,
                                PhpExpressionNode valueNode, String callerClassName) {
        this.objectNode = objectNode;
        this.propertyName = propertyName;
        this.valueNode = valueNode;
        this.callerClassName = callerClassName;
    }

    // Legacy constructor for backward compatibility
    public PhpPropertyWriteNode(PhpExpressionNode objectNode, String propertyName,
                                PhpExpressionNode valueNode, boolean isInternal) {
        this.objectNode = objectNode;
        this.propertyName = propertyName;
        this.valueNode = valueNode;
        this.callerClassName = null; // Treated as external access
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object objectValue = objectNode.execute(frame);

        if (!(objectValue instanceof PhpObject)) {
            throw new RuntimeException("Trying to set property of non-object");
        }

        PhpObject object = (PhpObject) objectValue;
        Object value = valueNode.execute(frame);

        // Look up caller class if we have a class name
        PhpClass callerClass = null;
        if (callerClassName != null) {
            PhpContext context = PhpContext.get(this);
            callerClass = context.getClass(callerClassName);
        }

        // Use visibility checking with callerClass
        object.writeProperty(propertyName, value, callerClass);

        return value;
    }
}
