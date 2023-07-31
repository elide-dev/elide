module elide.cli {
    requires java.base;
    requires java.logging;
    requires jdk.jshell;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;

    requires io.micronaut.inject;
    requires io.micronaut.picocli.picocli;

    requires org.graalvm.sdk;
    requires org.graalvm.truffle;

    requires ch.qos.logback.classic;
    requires info.picocli;
    requires info.picocli.jansi.graalvm;
    requires org.slf4j;
    requires org.jline.console;
    requires org.jline.style;
    requires org.jline.terminal;
    requires jul.to.slf4j;

    requires elide.core;
    requires elide.base;
    requires elide.graalvm;
    requires org.jline.reader;
    requires ch.qos.logback.core;
    requires io.micronaut.core;
    requires org.jline.builtins;

    exports elide.tool.cli;
    exports elide.tool.cli.cfg;
}