/// <reference types="bun-types" />
/// <reference types="@cloudflare/workers-types" />
/// <reference path="./worker-apis.d.ts" />
// noinspection JSUnusedGlobalSymbols

import { WorkerEntrypoint } from "cloudflare:workers"

// Regex matching for a valid Elide version; versions look like `1.0.0` or
// `1.0.0-alpha1`.
const versionRegex = /^([0-9{1,2}.]{5,10})-?(alpha|beta|rc|snapshot)?([0-9{1,2}])$/

// Regex matching for a valid platform tag; platform tags look like\
// `linux-amd64`.
const platformTagRegex = /^(linux|macos|windows)-(amd64|arm64)$/

// Special version string indicating the latest version.
const LATEST = "latest"

// Default-latest-version if none is resolvable.
const DEFAULT_LATEST_VERSION = "1.0.0-beta9"

// Base URL for GitHub downloads.
const GITHUB_BASE = "https://github.com/elide-dev/elide/releases/download"

// Cache control to return for downloads.
const DOWNLOAD_CACHE_CONTROL = "public, max-age=900, s-maxage=86400, immutable"

// Cache control to return for downloads which are not immutable.
const DOWNLOAD_CACHE_CONTROL_NONIMMUTABLE = "public, max-age=900, s-maxage=86400"

/**
 * Decides which backend to use for serving an Elide binary.
 *
 * `r2` serves the bin from R2, while `ghs` serves the bin from GitHub's release
 * asset storage.
 *
 * R2 should be used for most cases, as it is broadly faster outside of GitHub's
 * own networks. GitHub's release storage should be used when the installation
 * is happening from GHA.
 */
enum ServingBackend {
  // Serve the bin from R2.
  r2 = "r2",

  // Serve the bin from GitHub.
  ghs = "ghs",
}

/**
 * Operating systems supported by Elide.
 */
enum OperatingSystem {
  linux = "linux",
  macos = "macos",
  windows = "windows",
}

/**
 * Architectures supported by Elide.
 */
enum Architecture {
  amd64 = "amd64",
  arm64 = "arm64",
}

/**
 * Archive formats supported for Elide downloads.
 */
enum ArchiveFormat {
  zip = "zip",
  tgz = "tgz",
  txz = "txz",
}

/**
 * Parameters describing a requested Elide download.
 *
 * These include:
 *
 * - `backend` (Required): The storage backend to serve from.
 * - `format` (Required): The archive format to serve.
 * - `version` (Required): Specific requested version, as applicable.
 * - `platform`: The requested architecture/OS pair, as applicable; this is also
 *   referred to as the "platform tag." Examples include: `linux-amd64`,
 *   `macos-amd64`, `macos-aarch64`.
 */
type ElideInstallParams = {
  // Which storage backend to serve from.
  backend: ServingBackend

  // The archive format to serve.
  format: ArchiveFormat

  // The requested Elide version.
  version: string

  // The platform string requested for this download.
  platform?: string

  // The requested OS for this download.
  os?: OperatingSystem

  // The requested architecture for this download.
  arch?: Architecture
}

// Default platform to serve if one cannot be inferred from the request.
const DEFAULT_PLATFORM = {
  os: OperatingSystem.linux,
  arch: Architecture.amd64,
}

// Domain which should activate the GitHub backend.
const ghaDownloadDomain = "gha.elide.zip"
const ghaDownloadDomainAlt = "gha.elide.dev"

// Determine the serving backend to use based on the subdomain or user-agent.
function servingBackend(domain: string, ua: string): ServingBackend {
  switch (domain.trim().toLowerCase()) {
    case ghaDownloadDomain:
      return ServingBackend.ghs
    case ghaDownloadDomainAlt:
      return ServingBackend.ghs
    default:
      if (ua.trim().toLowerCase().indexOf("github") !== -1) {
        return ServingBackend.ghs
      }
      return ServingBackend.r2
  }
}

// Resolve the latest installable version of Elide.
async function resolveLatestVersion(): Promise<string> {
  return DEFAULT_LATEST_VERSION
}

// Validate a requested version.
async function validateVersion(token: string): Promise<string> {
  if (token === LATEST) {
    return LATEST
  }
  if (versionRegex.test(token)) {
    return token
  }
  throw new Error(`Invalid version requested: ${token}; must be of the form 'x.y.z'`)
}

// Parse a platform tag into its constituent parts, or throw.
function parsePlatformTag(tag: string): { os: OperatingSystem; arch: Architecture } {
  const token = tag.trim().toLowerCase()
  let os: OperatingSystem
  let arch: Architecture
  const portions = token.split("-")
  if (portions.length !== 2) {
    throw new Error(`Invalid platform tag: ${token}`)
  }

  switch (portions[0].trim().toLowerCase()) {
    case OperatingSystem.linux:
      os = OperatingSystem.linux
      break
    case OperatingSystem.macos:
      os = OperatingSystem.macos
      break
    case OperatingSystem.windows:
      os = OperatingSystem.windows
      break
    default:
      console.warn(`Could not infer OS from platform tag: ${portions[0]}`)
      os = OperatingSystem.linux
      break
  }
  switch (portions[1].trim().toLowerCase()) {
    case Architecture.amd64:
    case "x86_64":
    case "x86-64":
      arch = Architecture.amd64
      break
    case Architecture.arm64:
    case "aarch64":
      arch = Architecture.arm64
      break
    default:
      // default to x86_64 if we cannot infer the architecture
      console.warn(`Could not infer architecture from platform tag: ${portions[1]}`)
      arch = Architecture.amd64
      break
  }
  if (!os || !arch) {
    console.warn(`Failed to parse from platform tag: '${token}'`)
    return DEFAULT_PLATFORM
  }
  return {
    os,
    arch,
  }
}

// Attempt to infer a platform tag from the provided User-Agent string, or return null.
function parsePlatformFormUa(ua: string): { os: OperatingSystem; arch: Architecture } {
  const token = ua.trim().toLowerCase()
  let os: OperatingSystem
  let arch: Architecture
  if (token.indexOf("linux") !== -1) {
    os = OperatingSystem.linux
  } else if (token.indexOf("macos") !== -1) {
    os = OperatingSystem.macos
  } else if (token.indexOf("windows") !== -1) {
    os = OperatingSystem.windows
  } else {
    console.log("Failed to infer OS from UA")
    return DEFAULT_PLATFORM
  }
  if (token.indexOf("arm64") !== -1 || token.indexOf("aarch64") !== null) {
    arch = Architecture.arm64
  } else if (token.indexOf("amd64") !== -1 || token.indexOf("x86_64") !== null) {
    arch = Architecture.amd64
  } else {
    console.log("Failed to infer arch from UA")
    return DEFAULT_PLATFORM
  }
  if (!arch || !os) {
    throw new Error("Failed to infer platform from UA; this should be unreachable")
  }
  return {
    arch,
    os,
  }
}

// Check installation parameters for sanity.
function checkParams(params: ElideInstallParams): ElideInstallParams {
  const { version, platform, os, arch } = params
  if (!version) throw new Response("Invalid version", { status: 400 })
  if (!platform) throw new Response("Invalid platform", { status: 400 })
  if (!os) throw new Response("Invalid OS", { status: 400 })
  if (!arch) throw new Response("Invalid architecture", { status: 400 })
  return params
}

// Select the appropriate archive format to serve.
function selectFormat(url: URL, os?: OperatingSystem): ArchiveFormat {
  if (os) {
    switch (os) {
      case OperatingSystem.windows:
        return ArchiveFormat.zip
      case OperatingSystem.macos:
        return ArchiveFormat.txz
      case OperatingSystem.linux:
        return ArchiveFormat.tgz
    }
  }
  const filename = url.pathname.split("/").pop() || ""
  if (filename.endsWith(".zip")) {
    return ArchiveFormat.zip
  }
  if (filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
    return ArchiveFormat.tgz
  }
  if (filename.endsWith(".tar.xz") || filename.endsWith(".txz")) {
    return ArchiveFormat.txz
  }
  // default to tgz
  return ArchiveFormat.tgz
}

// Extracts installation parameters from the parsed request URL, or throws.
async function extractParams(url: URL, request: Request): Promise<ElideInstallParams> {
  const userAgent = request.headers.get("User-Agent") || ""
  const backend = servingBackend(url.hostname, userAgent)
  const segments = url.pathname.split("/")
  const versionIfPresent = segments.find(it => versionRegex.test(it))
  const platformIfPresent = segments.find(it => platformTagRegex.test(it))

  let requestedPlatform: string
  let requestedVersion: string
  let requestedOs: OperatingSystem
  let requestedArch: Architecture

  if (versionIfPresent) {
    requestedVersion = await validateVersion(versionIfPresent)
  } else {
    requestedVersion = await resolveLatestVersion()
  }
  if (platformIfPresent) {
    const { os, arch } = parsePlatformTag(platformIfPresent)
    requestedPlatform = platformIfPresent
    requestedOs = os
    requestedArch = arch
  } else {
    const { os, arch } = parsePlatformFormUa(userAgent)
    requestedOs = os
    requestedArch = arch
    requestedPlatform = `${os}-${arch}`
  }
  return checkParams({
    backend,
    os: requestedOs,
    arch: requestedArch,
    version: requestedVersion,
    platform: requestedPlatform,
    format: selectFormat(url, requestedOs) || ArchiveFormat.tgz,
  })
}

// Serve an Elide installation download from R2 directly, streamed through the worker.
async function serveDownloadFromR2(
  env: Env,
  url: URL,
  request: Request,
  params: ElideInstallParams,
): Promise<Response> {
  const { version, platform, format } = params
  // path in r2
  const path = `cli/v1/snapshot/${platform}/${version}/elide-${version}-${platform}.${format}`
  console.log(`Serving download from R2: '${path}'`, { url, request })

  // fetch and 404 if not found
  const object = await env.STORAGE.get(path)
  if (object === null) {
    console.warn(`Path '${path}' not found in R2; returning 404`)
    return new Response("Not Found", { status: 404 })
  }

  // write caching headers, etag, and other metadata from r2
  const headers = new Headers()
  object.writeHttpMetadata(headers)
  headers.set("ETag", object.httpEtag)
  headers.set("Cache-Control", DOWNLOAD_CACHE_CONTROL)
  headers.set("Content-Disposition", `attachment; filename="elide-${version}-${platform}.${format}"`)

  return new Response(object.body, {
    headers,
  })
}

// Serve an Elide installation download from GitHub, via a redirect.
async function serveDownloadFromGitHub(url: URL, request: Request, params: ElideInstallParams): Promise<Response> {
  const { version, platform, format } = params
  const sampleUrl = `${GITHUB_BASE}/${version}/elide-${version}-${platform}.${format}`
  console.log(`Serving redirect to GitHub download: '${sampleUrl}'`, { url, request })
  const headers = new Headers()
  headers.set("Location", sampleUrl)
  headers.set("Cache-Control", DOWNLOAD_CACHE_CONTROL_NONIMMUTABLE)

  return new Response(null, {
    status: 302, // Found
    statusText: "Found",
    headers,
  })
}

// Satisfy checked download parameters.
async function serveOrSendDownload(
  env: Env,
  url: URL,
  request: Request,
  params: ElideInstallParams,
): Promise<Response> {
  const { backend } = params
  if (backend === ServingBackend.ghs) {
    return serveDownloadFromGitHub(url, request, params)
  }
  return serveDownloadFromR2(env, url, request, params)
}

// Record request/response stats to Analytics Engine.
async function record(
  env: Env,
  start: number,
  url: URL,
  request: Request,
  response: Promise<Response> | Response,
  success: boolean,
  cached: boolean,
  params?: ElideInstallParams,
): Promise<void> {
  const end = performance.now()
  const duration = end - start
  const resp = response instanceof Response ? response : await response
  const status = resp ? resp.status : 500

  console.log(`Completed with status ${status}; duration: ${duration}ms`, {
    success,
    cached,
    url,
    request,
  })

  const version = params?.version || "version-unknown"
  const platform = params?.platform || "platform-unknown"
  const backend = params?.backend || ServingBackend.r2

  env.ANALYTICS.writeDataPoint({
    indexes: [version],
    doubles: [duration],
    blobs: [platform, backend, success ? "success" : "fail", cached ? "cached" : "fresh"],
  })
}

// Entrypoint for the worker.
export default class extends WorkerEntrypoint<Env> {
  constructor(
    ctx: ExecutionContext,
    private workerEnv: Env,
  ) {
    super(ctx, workerEnv)
  }

  async fetch(request: Request): Promise<Response> {
    // 0. take a timestamp.
    const start = performance.now()

    // 1. always parse the URL.
    const url = new URL(request.url)

    // 2. try to match against the http cache.
    // (not implemented yet)
    const cache = await caches.open("default")
    const cached = await cache.match(request.url)

    // 3. if the http cache matched, serve it directly.
    if (cached) {
      console.log("Match cached response; returning", { url, request })
      this.ctx.waitUntil(record(this.env, start, url, request, cached, true, true))
      return cached
    }

    // special case: if the pathname starts with `/cli/`, let it through, it is
    // a legacy download URL.
    if (url.pathname.startsWith("/cli/")) {
      const obj = await this.env.STORAGE.get(url.pathname.slice(1))
      if (obj === null) {
        console.warn(`Storage URL not found: '${url.pathname}'; returning 404`)
        return new Response("Not Found", { status: 404 })
      }

      const headers = new Headers()
      obj.writeHttpMetadata(headers)
      headers.set("ETag", obj.httpEtag)
      if (url.pathname.endsWith(".zip") || url.pathname.endsWith(".tgz") || url.pathname.endsWith(".txz")) {
        headers.set("Cache-Control", DOWNLOAD_CACHE_CONTROL)
      } else {
        headers.set("Cache-Control", DOWNLOAD_CACHE_CONTROL_NONIMMUTABLE)
      }
      headers.set("Content-Disposition", `attachment; filename="${obj.key.split("/").pop()}"`)
      const resp = new Response(obj.body, { headers })
      this.ctx.waitUntil(
        Promise.all([record(this.env, start, url, request, resp, true, false), cache.put(url, resp.clone())]),
      )
      return resp
    }

    try {
      // 4. try to extract URL params, or fall back to the UA, or throw. this
      //    will resolve the "latest" version of elide if needed.
      const params = await extractParams(url, request)

      // 5. serve the download or a redirect.
      console.log("Serving download", params)
      const response = await serveOrSendDownload(this.workerEnv, url, request, params)

      if (response && response.status < 400) {
        // 6. successful responses should be cached.
        console.log("Enqueuing for cached access")
        this.ctx.waitUntil(cache.put(url, response.clone()))
      }
      // 7. all responses should be recorded for metrics.
      this.ctx.waitUntil(record(this.env, start, url, request, response, true, false, params))
      return response
    } catch (err) {
      if (err instanceof Response) {
        this.ctx.waitUntil(record(this.env, start, url, request, err, false, false))
        return err
      }
      throw err
    }
  }
}
