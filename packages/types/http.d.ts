enum StandardHttpMethod {
  GET = "GET",
  POST = "POST",
  PUT = "PUT",
  PATCH = "PATCH",
  DELETE = "DELETE",
  OPTIONS = "OPTIONS",
  HEAD = "HEAD",
}

type HttpMethodString = "GET" | "POST" | "PUT" | "PATCH" | "DELETE" | "OPTIONS" | "HEAD"

type ElideHttpMethod = StandardHttpMethod | HttpMethodString | string

interface ElideHttpHeaders {}

interface ElideHttpRequest {
  readonly method: HttpMethodString
  readonly uri: string
  readonly version: string
  readonly headers: ElideHttpHeaders
}

type ElideHttpResponse = any

type HttpHandlerFunction = (request: ElideHttpRequest, response: ElideHttpResponse) => void
type AsyncHttpHandlerFunction = (request: ElideHttpRequest, response: ElideHttpResponse) => Promise<void>

type HttpHandler = HttpHandlerFunction | AsyncHttpHandlerFunction

type ElideHttpHandlerPath = string

interface ElideHttpRouter {
  handle(method: ElideHttpMethod, path: ElideHttpHandlerPath, handler: HttpHandler): void
}

interface ElideHttpConfig {
  port: number
  host: string

  onBind(cbk: () => void): void
}

interface ElideHttp {
  readonly router: ElideHttpRouter
  readonly config: ElideHttpConfig
  start(): void
}
