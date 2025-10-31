package dev.truffle.php.runtime;

import com.oracle.truffle.api.CallTarget;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a PHP class definition.
 * Stores metadata about the class including properties, methods, and constructor.
 */
public final class PhpClass {

    private final String name;
    private final Map<String, PropertyMetadata> properties;
    private final Map<String, MethodMetadata> methods;
    private final CallTarget constructor;
    private final Map<String, Object> staticPropertyValues;  // Storage for static property values
    private PhpClass parentClass;  // Parent class for inheritance (nullable)

    public PhpClass(String name, Map<String, PropertyMetadata> properties,
                    Map<String, MethodMetadata> methods, CallTarget constructor) {
        this.name = name;
        this.properties = new HashMap<>(properties);
        this.methods = new HashMap<>(methods);
        this.constructor = constructor;
        this.staticPropertyValues = new HashMap<>();
        this.parentClass = null;

        // Initialize static properties with their default values
        for (Map.Entry<String, PropertyMetadata> entry : properties.entrySet()) {
            PropertyMetadata prop = entry.getValue();
            if (prop.isStatic()) {
                staticPropertyValues.put(entry.getKey(), prop.getDefaultValue());
            }
        }
    }

    public PhpClass getParentClass() {
        return parentClass;
    }

    public void setParentClass(PhpClass parentClass) {
        this.parentClass = parentClass;
    }

    public String getName() {
        return name;
    }

    public Map<String, PropertyMetadata> getProperties() {
        return properties;
    }

    public Map<String, MethodMetadata> getMethods() {
        return methods;
    }

    public CallTarget getConstructor() {
        return constructor;
    }

    public boolean hasMethod(String methodName) {
        if (methods.containsKey(methodName)) {
            return true;
        }
        // Check parent class if present
        if (parentClass != null) {
            return parentClass.hasMethod(methodName);
        }
        return false;
    }

    public MethodMetadata getMethod(String methodName) {
        MethodMetadata method = methods.get(methodName);
        if (method != null) {
            return method;
        }
        // Check parent class if present
        if (parentClass != null) {
            return parentClass.getMethod(methodName);
        }
        return null;
    }

    public boolean hasProperty(String propertyName) {
        if (properties.containsKey(propertyName)) {
            return true;
        }
        // Check parent class if present
        if (parentClass != null) {
            return parentClass.hasProperty(propertyName);
        }
        return false;
    }

    public PropertyMetadata getProperty(String propertyName) {
        PropertyMetadata property = properties.get(propertyName);
        if (property != null) {
            return property;
        }
        // Check parent class if present
        if (parentClass != null) {
            return parentClass.getProperty(propertyName);
        }
        return null;
    }

    /**
     * Get all properties including inherited ones.
     * Properties are returned in inheritance order: parent first, then child.
     * Child properties with same name override parent properties.
     */
    public Map<String, PropertyMetadata> getAllProperties() {
        Map<String, PropertyMetadata> allProps = new HashMap<>();

        // Start with parent properties (if parent exists)
        if (parentClass != null) {
            allProps.putAll(parentClass.getAllProperties());
        }

        // Add/override with this class's properties
        allProps.putAll(properties);

        return allProps;
    }

    /**
     * Get the constructor for this class, or inherited from parent.
     */
    public CallTarget getConstructorOrInherited() {
        if (constructor != null) {
            return constructor;
        }
        // Check parent class if present
        if (parentClass != null) {
            return parentClass.getConstructorOrInherited();
        }
        return null;
    }

    public Object getStaticPropertyValue(String propertyName) {
        return staticPropertyValues.get(propertyName);
    }

    public void setStaticPropertyValue(String propertyName, Object value) {
        staticPropertyValues.put(propertyName, value);
    }

    public boolean hasStaticProperty(String propertyName) {
        PropertyMetadata prop = properties.get(propertyName);
        return prop != null && prop.isStatic();
    }

    /**
     * Check if a method is accessible from the given caller class context.
     *
     * @param methodName The method to check
     * @param callerClass The class from which the access is being made (null = external access)
     * @return true if accessible, false otherwise
     */
    public boolean isMethodAccessible(String methodName, PhpClass callerClass) {
        MethodMetadata method = getMethod(methodName);
        if (method == null) {
            return false;
        }

        PhpClass definingClass = getMethodDefiningClass(methodName);
        return isAccessible(method.getVisibility(), definingClass, callerClass);
    }

    /**
     * Get the class where a method is defined (walks inheritance chain).
     */
    private PhpClass getMethodDefiningClass(String methodName) {
        // Check if defined in this class
        if (methods.containsKey(methodName)) {
            return this;
        }
        // Check parent classes
        PhpClass current = parentClass;
        while (current != null) {
            if (current.getMethods().containsKey(methodName)) {
                return current;
            }
            current = current.getParentClass();
        }
        return this; // Fallback to current class
    }

    /**
     * Check if a member with the given visibility is accessible from the caller class.
     */
    private boolean isAccessible(Visibility visibility, PhpClass definingClass, PhpClass callerClass) {
        switch (visibility) {
            case PUBLIC:
                return true;
            case PROTECTED:
                // Protected is accessible from the same class or subclasses
                if (callerClass == null) {
                    return false; // External access not allowed
                }
                return isSameOrSubclass(callerClass, definingClass);
            case PRIVATE:
                // Private is only accessible from the exact same class
                if (callerClass == null) {
                    return false; // External access not allowed
                }
                return callerClass == definingClass;
            default:
                return false;
        }
    }

    /**
     * Check if testClass is the same as or a subclass of baseClass.
     */
    private boolean isSameOrSubclass(PhpClass testClass, PhpClass baseClass) {
        PhpClass current = testClass;
        while (current != null) {
            if (current == baseClass) {
                return true;
            }
            current = current.getParentClass();
        }
        return false;
    }

    /**
     * Metadata about a class property.
     */
    public static final class PropertyMetadata {
        private final String name;
        private final Visibility visibility;
        private final boolean isStatic;
        private final Object defaultValue;

        public PropertyMetadata(String name, Visibility visibility, boolean isStatic, Object defaultValue) {
            this.name = name;
            this.visibility = visibility;
            this.isStatic = isStatic;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public Visibility getVisibility() {
            return visibility;
        }

        // Legacy method for backwards compatibility
        public boolean isPublic() {
            return visibility == Visibility.PUBLIC;
        }

        public boolean isStatic() {
            return isStatic;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    /**
     * Metadata about a class method.
     */
    public static final class MethodMetadata {
        private final String name;
        private final Visibility visibility;
        private final boolean isStatic;
        private final CallTarget callTarget;
        private final String[] parameterNames;

        public MethodMetadata(String name, Visibility visibility, boolean isStatic, CallTarget callTarget, String[] parameterNames) {
            this.name = name;
            this.visibility = visibility;
            this.isStatic = isStatic;
            this.callTarget = callTarget;
            this.parameterNames = parameterNames;
        }

        public String getName() {
            return name;
        }

        public Visibility getVisibility() {
            return visibility;
        }

        // Legacy method for backwards compatibility
        public boolean isPublic() {
            return visibility == Visibility.PUBLIC;
        }

        public boolean isStatic() {
            return isStatic;
        }

        public CallTarget getCallTarget() {
            return callTarget;
        }

        public String[] getParameterNames() {
            return parameterNames;
        }
    }
}
