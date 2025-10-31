package dev.truffle.php.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a PHP object instance.
 * Stores the object's class reference and property values.
 */
@ExportLibrary(InteropLibrary.class)
public final class PhpObject implements TruffleObject {

    private final PhpClass phpClass;
    private final Map<String, Object> properties;

    public PhpObject(PhpClass phpClass) {
        this.phpClass = phpClass;
        this.properties = new HashMap<>();

        // Initialize properties with default values
        for (Map.Entry<String, PhpClass.PropertyMetadata> entry : phpClass.getProperties().entrySet()) {
            properties.put(entry.getKey(), entry.getValue().getDefaultValue());
        }
    }

    public PhpClass getPhpClass() {
        return phpClass;
    }

    /**
     * Read a property value.
     */
    public Object readProperty(String propertyName) {
        // Check if property exists in class definition
        if (!phpClass.hasProperty(propertyName)) {
            throw new RuntimeException("Undefined property: " + phpClass.getName() + "::$" + propertyName);
        }

        // Check visibility
        PhpClass.PropertyMetadata metadata = phpClass.getProperty(propertyName);
        if (!metadata.isPublic()) {
            throw new RuntimeException("Cannot access private property: " + phpClass.getName() + "::$" + propertyName);
        }

        return properties.get(propertyName);
    }

    /**
     * Write a property value.
     */
    public void writeProperty(String propertyName, Object value) {
        // Check if property exists in class definition
        if (!phpClass.hasProperty(propertyName)) {
            throw new RuntimeException("Undefined property: " + phpClass.getName() + "::$" + propertyName);
        }

        // Check visibility
        PhpClass.PropertyMetadata metadata = phpClass.getProperty(propertyName);
        if (!metadata.isPublic()) {
            throw new RuntimeException("Cannot access private property: " + phpClass.getName() + "::$" + propertyName);
        }

        properties.put(propertyName, value);
    }

    /**
     * Read property internally (from within methods, bypasses visibility).
     */
    public Object readPropertyInternal(String propertyName) {
        if (!phpClass.hasProperty(propertyName)) {
            throw new RuntimeException("Undefined property: " + phpClass.getName() + "::$" + propertyName);
        }
        return properties.get(propertyName);
    }

    /**
     * Write property internally (from within methods, bypasses visibility).
     */
    public void writePropertyInternal(String propertyName, Object value) {
        if (!phpClass.hasProperty(propertyName)) {
            throw new RuntimeException("Undefined property: " + phpClass.getName() + "::$" + propertyName);
        }
        properties.put(propertyName, value);
    }

    @Override
    public String toString() {
        return phpClass.getName() + " Object";
    }

    // Interop messages for Truffle
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return properties.keySet().toArray(new String[0]);
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return phpClass.hasProperty(member);
    }

    @ExportMessage
    Object readMember(String member) {
        return readProperty(member);
    }

    @ExportMessage
    boolean isMemberModifiable(String member) {
        return phpClass.hasProperty(member);
    }

    @ExportMessage
    boolean isMemberInsertable(@SuppressWarnings("unused") String member) {
        // PHP objects can only have properties defined in their class
        return false;
    }

    @ExportMessage
    void writeMember(String member, Object value) {
        writeProperty(member, value);
    }
}
