module elide.graalvm {
    requires java.base;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires kotlin.stdlib;
    requires kotlin.reflect;
    requires kotlinx.coroutines.core;
    requires kotlinx.coroutines.jdk9;
    requires kotlinx.datetime;
    requires kotlinx.serialization.core;
    requires kotlinx.serialization.json;

    requires org.graalvm.sdk;
    requires org.graalvm.truffle;

    requires elide.core;
    requires elide.base;
    requires elide.ssr;

    exports elide.runtime.intrinsics.js;
    exports elide.runtime.gvm;
    exports elide.runtime.gvm.internals;
    exports elide.runtime.gvm.internals.intrinsics.js.base64;
    exports elide.runtime.gvm.internals.intrinsics.js.console;
    exports elide.runtime.gvm.internals.intrinsics.js.crypto;
    exports elide.runtime.gvm.internals.intrinsics.js.express;
    exports elide.runtime.gvm.internals.intrinsics.js.url;
    exports elide.runtime.gvm.vfs;
}
