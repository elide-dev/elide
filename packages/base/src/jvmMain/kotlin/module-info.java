module elide.base {
    requires java.base;
    requires kotlin.stdlib;
    requires io.micronaut.inject;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires org.slf4j;

    exports elide.annotations;
    exports elide.annotations.base;
    exports elide.annotations.data;
    exports elide.util;
    exports elide.runtime;
    exports elide.runtime.jvm;
}
