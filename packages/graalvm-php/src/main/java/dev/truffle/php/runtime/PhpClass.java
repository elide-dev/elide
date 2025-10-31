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

    public PhpClass(String name, Map<String, PropertyMetadata> properties,
                    Map<String, MethodMetadata> methods, CallTarget constructor) {
        this.name = name;
        this.properties = new HashMap<>(properties);
        this.methods = new HashMap<>(methods);
        this.constructor = constructor;
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
        return methods.containsKey(methodName);
    }

    public MethodMetadata getMethod(String methodName) {
        return methods.get(methodName);
    }

    public boolean hasProperty(String propertyName) {
        return properties.containsKey(propertyName);
    }

    public PropertyMetadata getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    /**
     * Metadata about a class property.
     */
    public static final class PropertyMetadata {
        private final String name;
        private final boolean isPublic;
        private final Object defaultValue;

        public PropertyMetadata(String name, boolean isPublic, Object defaultValue) {
            this.name = name;
            this.isPublic = isPublic;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public boolean isPublic() {
            return isPublic;
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
        private final boolean isPublic;
        private final CallTarget callTarget;
        private final String[] parameterNames;

        public MethodMetadata(String name, boolean isPublic, CallTarget callTarget, String[] parameterNames) {
            this.name = name;
            this.isPublic = isPublic;
            this.callTarget = callTarget;
            this.parameterNames = parameterNames;
        }

        public String getName() {
            return name;
        }

        public boolean isPublic() {
            return isPublic;
        }

        public CallTarget getCallTarget() {
            return callTarget;
        }

        public String[] getParameterNames() {
            return parameterNames;
        }
    }
}
