module elide.ssr {
    requires java.base;
    requires kotlin.stdlib;
    requires kotlinx.serialization.core;
    requires io.micronaut.core;
    requires io.micronaut.http;

    requires org.graalvm.sdk;

    requires elide.base;

    exports elide.ssr;
    exports elide.ssr.annotations;
    exports elide.ssr.type;
    exports elide.vm.annotations;
}