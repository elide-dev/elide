//[server](../../index.md)/[elide.server.controller](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [BaseController](-base-controller/index.md) | [jvm]<br>abstract class [BaseController](-base-controller/index.md) : [ElideController](-elide-controller/index.md)<br>Abstract base implementation of built-in Elide controller abstracts. |
| [ElideController](-elide-controller/index.md) | [jvm]<br>interface [ElideController](-elide-controller/index.md)<br>Describes the top-level expected interface for Elide-based controllers; any base class which inherits from this one may be used as a controller, and activated/deactivated with Micronaut annotations (see: `@Controller`). |
| [PageController](-page-controller/index.md) | [jvm]<br>abstract class [PageController](-page-controller/index.md) : [BaseController](-base-controller/index.md)<br>Defines the built-in concept of a `Page`-type handler, which is capable of performing SSR, serving static assets, and handling page-level RPC calls. |
| [PageWithProps](-page-with-props/index.md) | [jvm]<br>abstract class [PageWithProps](-page-with-props/index.md)&lt;[State](-page-with-props/index.md)&gt; : [PageController](-page-controller/index.md)<br>Extends [PageController](-page-controller/index.md) with support for page-level [State](-page-with-props/index.md), produced via the [props](-page-with-props/props.md) method; computed state is shared with VM render executions, and can additionally be injected into the page for use by frontend code (typically to hydrate a server-rendered UI). |
| [StatusEnabledController](-status-enabled-controller/index.md) | [jvm]<br>interface [StatusEnabledController](-status-enabled-controller/index.md) |
