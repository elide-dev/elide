module elide.cli {
    requires java.base;
    requires java.logging;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;

    requires org.graalvm.sdk;
    requires org.graalvm.truffle;

    requires ch.qos.logback.classic;
    requires info.picocli;
    requires org.slf4j;
    requires org.jline.console;
    requires org.jline.style;
    requires org.jline.terminal;

    requires elide.core;
    requires elide.base;
    requires elide.graalvm;

    exports elide.tool.cli;
    exports elide.tool.cli.cfg;
}