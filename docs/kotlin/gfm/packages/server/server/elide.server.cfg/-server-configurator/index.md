//[server](../../../index.md)/[elide.server.cfg](../index.md)/[ServerConfigurator](index.md)

# ServerConfigurator

[jvm]\
@Requires(notEnv = [&quot;test&quot;])

@ContextConfigurer

class [ServerConfigurator](index.md) : ApplicationContextConfigurer

Configures Micronaut on behalf of an Elide application with default configuration state.

The developer may override these properties via their own configuration sources, which can be placed on the classpath as resources, loaded via Kubernetes configuration, or loaded via custom implementations of PropertySource / PropertySourcePropertyResolver.

On behalf of the developer, the following application components are configured:

- 
   **Netty**: Workers, native transports, threading, allocation, event loops
- 
   **SSL**: Self-signed certs for development, modern suite of TLS ciphers and protocols
- 
   **HTTP**: HTTP/2, compression thresholds and modes, HTTP->HTTPS redirect
- 
   **Access Log**: Shows the HTTP access log via `stdout` during development
- 
   **DI Container**: Eager initialization of [Logic](../../../../../packages/base/base/elide.annotations/-logic/index.md) objects

No action is needed to enable the above components; the DI container loads and applies this configuration automatically at application startup.

## Constructors

| | |
|---|---|
| [ServerConfigurator](-server-configurator.md) | [jvm]<br>fun [ServerConfigurator](-server-configurator.md)() |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [configure](configure.md) | [jvm]<br>open override fun [configure](configure.md)(builder: ApplicationContextBuilder) |
| [getOrder](../../elide.server.http/-request-context-filter/index.md#785826419%2FFunctions%2F-1343588467) | [jvm]<br>open fun [getOrder](../../elide.server.http/-request-context-filter/index.md#785826419%2FFunctions%2F-1343588467)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
