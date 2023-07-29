module elide.model {
    requires java.base;
    requires kotlin.stdlib;

    requires kotlinx.datetime;
    requires kotlinx.serialization.core;
    requires kotlinx.serialization.protobuf;

    requires com.google.protobuf;

    requires elide.core;
    requires elide.base;
}
