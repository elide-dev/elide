package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpObject;

/**
 * Node for the instanceof operator.
 *
 * Syntax: $obj instanceof ClassName
 *
 * Returns true if the object is an instance of the specified class, any of its parent classes,
 * or if the class implements the specified interface.
 */
public final class PhpInstanceofNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode objectNode;

    private final String className;

    public PhpInstanceofNode(PhpExpressionNode objectNode, String className) {
        this.objectNode = objectNode;
        this.className = className;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = objectNode.execute(frame);

        // If the value is not a PhpObject, instanceof returns false
        if (!(value instanceof PhpObject)) {
            return false;
        }

        PhpObject phpObject = (PhpObject) value;
        PhpClass objectClass = phpObject.getPhpClass();

        // Check if the object's class matches the target class name
        if (objectClass.getName().equals(className)) {
            return true;
        }

        // Check parent classes (inheritance chain)
        PhpClass currentClass = objectClass.getParentClass();
        while (currentClass != null) {
            if (currentClass.getName().equals(className)) {
                return true;
            }
            currentClass = currentClass.getParentClass();
        }

        // Check if the class implements the interface
        if (objectClass.implementsInterface(className)) {
            return true;
        }

        return false;
    }
}
