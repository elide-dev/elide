## Elide Substrate

Defines the "substrate" build environment code which supports Elide's use in Kotlin. This includes several Kotlin
compiler plugins which are used for various purposes. The subtrate environment builds automatically before being used as
an input to the build for Elide itself.

See each plugin for more information.

**At this time, only the Redakt plugin is in use.** This plugin automatically "redacts" the string representation of any
value or class field marked with the `@Sensitive` annotation.
