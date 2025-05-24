/// <reference types="bun-types" />
/// <reference types="@cloudflare/workers-types" />
/// <reference path="./worker-apis.d.ts" />
// noinspection JSUnusedGlobalSymbols

import { WorkerEntrypoint } from "cloudflare:workers"

// Max attempts for a message before it is dropped.
const MAX_ATTEMPTS = 3

// Whether to emit debug logs.
const DEBUG = true

// Whether to record events to Analytics Engine.
const RECORD_EVENTS = true

// Version header.
const ELIDE_VERSION_HEADER = "x-elide-version"

// Platform header.
const ELIDE_PLATFORM_HEADER = "x-elide-platform"

/**
 * Enumerates types of events.
 */
enum EventType {
  /**
   * Elide ran a thing; this is the most common event type, and is emitted for runtime executions.
   */
  Run = 'run',

  /**
   * Hard-crash event (this does not include user errors).
   */
  Crash = 'crash',
}

/**
 * Telemetry event metadata; consistent structure for all events.
 */
type EventMetadata = {
  /**
   * Version of Elide; extracted from HTTP headers.
   */
  version: string,

  /**
   * Platform tag for this distribution of elide.
   */
  platform: string,

  /**
   * Whether Elide is operating in debug mode.
   */
  debug?: boolean,

  /**
   * Timings recorded by the telemetry receiver and frontend.
   */
  timing?: {
    received?: number,
    queued?: number,
    processed?: number,
  },
}

/**
 * Base payload type which event types inherit from.
 */
type BaseEventPayload = {
  /**
   * Type of event.
   */
  type: EventType,
}

/**
 * Inner event payload for a queued event; this payload is provided by the client.
 */
type EventPayload<T> = BaseEventPayload & T

/**
 * Defines a `run`-type event.
 */
type RunEventPayload = BaseEventPayload & {
  type: EventType.Run,
  mode?: 'run' | 'test' | 'server',
  exitCode?: number,
  duration?: number,
}

/**
 * Defines a `crash`-type event.
 */
type CrashEventPayload = BaseEventPayload & {
  type: EventType.Crash,
}

/**
 * Union of all event payload types.
 */
type AnyEventPayload = RunEventPayload | CrashEventPayload

/**
 * Structure of a queued telemetry event.
 */
type QueuedEvent = {
  /**
   * Version of the event schema; `1` is the current version.
   */
  v: 1,

  /**
   * Event data associated with this object.
   */
  e: EventPayload<AnyEventPayload>,

  /**
   * Generic metadata about this event; recorded by the telemetry receiver.
   */
  m: EventMetadata,
}

/**
 * Build an event request into a `QueuedEvent` object.
 *
 * @param req HTTP request.
 * @param payload Event payload; this is provided by the client.
 * @param start Starting timestamp.
 * @return Promise for a queued event.
 */
async function buildEvent(req: Request, payload: EventPayload<AnyEventPayload>, start: number): Promise<QueuedEvent> {
  const version = req.headers.get(ELIDE_VERSION_HEADER)
  const platform = req.headers.get(ELIDE_PLATFORM_HEADER)
  if (!version) {
    throw new Response(`Missing required header: ${ELIDE_VERSION_HEADER}`, { status: 400 })
  }
  if (!platform) {
    throw new Response(`Missing required header: ${ELIDE_PLATFORM_HEADER}`, { status: 400 })
  }
  return {
    v: 1,
    e: payload,
    m: {
      version,
      platform,
      timing: {
        received: start,
        queued: performance.now(),
      },
    }
  }
}

/**
 * Build an event request into a `QueuedEvent` object, and enqueue it.
 *
 * @param env Environment object; this is passed to the worker.
 * @param req HTTP request.
 * @param payload Event payload; this is provided by the client.
 * @param start Immediate start timestamp indicating when this request was received.
 * @param ctx Execution context; this is passed to the worker.
 * @return Promise for a queued event.
 */
async function buildEnqueue(
  env: Env,
  req: Request,
  payload: EventPayload<AnyEventPayload>,
  start: number,
  ctx: ExecutionContext,
): Promise<QueuedEvent> {
  const ev = await buildEvent(req, payload, start)
  ctx.waitUntil(env.QUEUE_TELEMETRY.send(ev))
  return ev
}

/**
 * Prepare an event being received over the queue.
 *
 * @param ev Queued event.
 * @return Transformed queued event, as applicable.
 */
async function prepareEvent(ev: QueuedEvent): Promise<QueuedEvent> {
  return {
    ...ev,
    m: {
      ...ev.m,
      timing: {
        ...ev.m.timing,
        processed: performance.now(),
      }
    }
  }
}

function eventToDatapoint(ev: QueuedEvent): any {
  const { version, platform } = ev.m
  const eventType = ev.e.type || "unknown"
  const datapoint: any = {
    indexes: [version],
    doubles: [],
    blobs: [`type:${eventType}`, platform],
  }
  switch (eventType) {
    case EventType.Run: {
      const payload = ev.e as RunEventPayload
      const { exitCode, duration, mode } = payload
      if (exitCode !== undefined) {
        datapoint.blobs.push(`exit:${exitCode}`)
        datapoint.blobs.push(exitCode === 0 ? 'success' : 'err')
      }
      if (duration !== undefined && duration) {
        datapoint.doubles.push(duration)
      }
      if (mode !== undefined && mode) {
        datapoint.blobs.push(`mode:${mode}`)
      }
      break;
    }
    case EventType.Crash: {
      break;
    }
  }
  return datapoint
}

/**
 * Record an event being received over the queue.
 *
 * @param env Environment object; this is passed to the worker.
 * @param ev Queued event.
 * @return Promise which concludes once the event has been recorded.
 */
async function recordEvent(env: Env, ev: QueuedEvent): Promise<void> {
  const eventDatapoint = eventToDatapoint(ev)
  if (DEBUG) {
    console.log(`Recording event as JSON: ${JSON.stringify(eventDatapoint, null, 2)}`)
  }
  if (RECORD_EVENTS) {
    try {
      env.ANALYTICS.writeDataPoint(eventDatapoint)
    } catch (err) {
      console.error(`Failed to record event: ${err}`)
      throw err
    }
  }
}

/**
 * Prepare and then record an event being received over the queue.
 *
 * @param ev Queued event.
 * @param ctx Execution context; this is passed to the worker.
 * @param env Environment object; this is passed to the worker.
 * @return The queued event, as a result, after any transformations.
 */
async function prepareRecordEvent(ev: Message<QueuedEvent>, ctx: ExecutionContext, env: Env): Promise<QueuedEvent> {
  const prepared = await prepareEvent(ev.body)
  ctx.waitUntil(recordEvent(env, prepared))
  return prepared
}

// Entrypoint for the worker.
export default class extends WorkerEntrypoint<Env> {
  constructor(
    ctx: ExecutionContext,
    private workerEnv: Env,
  ) {
    super(ctx, workerEnv)
  }

  /**
   * Telemetry `fetch`; handles incoming HTTPs traffic. Events are created and yield to the telemetry queue; an HTTP 202
   * response is provided immediately.
   *
   * @param request HTTP request.
   * @return HTTP 202 response, assuming a well-formed event; otherwise, HTTP 400 or similar error codes.
   */
  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url)
    const start = performance.now()
    if (DEBUG) {
      console.log(`Processing request: ${request.method} ${url.pathname}`, {
        start,
      })
    }
    let payload: EventPayload<AnyEventPayload>;
    switch (request.method) {
      case 'GET':
        if (url.pathname === '/' || url.pathname === '/health') {
          return new Response(null, { status: 204 })
        }
        break;
      case 'POST': {
        // decode json event payload
        try {
          payload = await request.json() as EventPayload<AnyEventPayload>
        } catch (err) {
          console.error(`Failed to decode JSON: ${err}`)
          return new Response("Invalid JSON", { status: 400 })
        }
        if (DEBUG) {
          console.log(`Received event: ${JSON.stringify(payload)}`)
        }
        break;
      }
      default:
        return new Response("Method not allowed", { status: 405 })
    }
    const routing = {
      "/v1/event:submit": async () => {
        const op = buildEnqueue(this.workerEnv, request, payload, start, this.ctx)
        this.ctx.waitUntil(op)
        if (DEBUG) {
          console.log(`Event accepted; returning 202.`)
        }
        return new Response(null, { status: 202, statusText: "Accepted" })
      }
    }
    try {
      const handler = routing[url.pathname]
      if (!handler) {
        return new Response("Not found", { status: 404 })
      }
      return handler()
    } catch (err) {
      if (err instanceof Error) {
        console.error("Failed to handle request:", err)
        return new Response(err.message, { status: 500 })
      } else if (err instanceof Response) {
        console.error(`Error response: ${err.status} ${err.statusText}`)
        return err
      } else {
        console.error(`Unknown error: ${err}`)
        return new Response("Unknown error", { status: 500 })
      }
    }

  }

  /**
   * Handle a batch of queued events; the queue buffers events until a configured threshold, and then delivers a batch
   * here, so they can be committed to analytics storage.
   *
   * @param batch Batch of queued events.
   * @return Promise which concludes once the batch has been processed.
   */
  async queue(batch: MessageBatch<QueuedEvent>): Promise<void> {
    let promises: Promise<QueuedEvent>[] = []
    let allErrors: Error[] = []
    if (DEBUG) {
      console.log(`Processing telemetry batch of ${batch.messages.length} messages`)
    }

    batch.messages.forEach((message) => {
      if (DEBUG) {
        console.log(`- Processing message '${message.id}'`)
      }
      try {
        promises.push(prepareRecordEvent(message, this.ctx, this.env))
        if (DEBUG) {
          console.log(`- Message '${message.id}' prepared for recording`)
        }
      } catch (err) {
        allErrors.push(err as Error)
        if (message.attempts <= MAX_ATTEMPTS) {
          console.warn(`Retrying event '${message.id}' (${message.attempts} attempts)`)
          message.retry()
        } else {
          console.error(`Exceeded attempts for message '${message.id}'; sending ACK to drop`)
          message.ack()
        }
      }
    })
    if (allErrors.length > 0) {
      console.error(`Errors while processing batch (${allErrors.length}): ${allErrors.map((e) => e.message).join(", ")}`)
    } else {
      if (DEBUG) {
        console.log(`Awaiting settlement of all processed messages`)
      }
      await Promise.all(promises)
      batch.ackAll()
      if (DEBUG) {
        console.log(`All messages acknowledged`)
      }
    }
  }
}
