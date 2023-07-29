module elide.core {
    requires java.base;
    requires kotlin.stdlib;

    exports elide.core.annotations;
    exports elide.core.encoding.base64;
    exports elide.core.encoding.hex;
}
