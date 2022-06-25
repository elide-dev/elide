//[jvm](../../../index.md)/[elide.server.rpc](../index.md)/[ServiceHealthManager](index.md)/[terminalShutdown](terminal-shutdown.md)

# terminalShutdown

[jvm]\
fun [terminalShutdown](terminal-shutdown.md)()

Notify the central service health system that the API service is experiencing a total and terminal shutdown, which should result in negative-status calls for all services queried on the health service. **This state is not recoverable,** and should only be used for system shutdown events.
