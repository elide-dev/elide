/// <reference types="@cloudflare/workers-types" />
/// <reference path="./worker-apis.d.ts" />
// noinspection JSUnusedGlobalSymbols

import { WorkerEntrypoint } from "cloudflare:workers";

// Entrypoint for the worker.
export default class extends WorkerEntrypoint<Env> {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== "GET") return new Response("Method not allowed", { status: 405 });
    const url = new URL(request.url);

    // intellij plugin registry
    if (url.pathname === "/intellij") return this.fetchIntellijIndex();
    if (url.pathname.startsWith("/intellij/")) return this.fetchIntellijPlugin(url.pathname);

    return new Response("Not found", { status: 404 });
  }

  private async fetchIntellijIndex(): Promise<Response> {
    // static index for now, but we could generate one in the future, i.e. based on a database registry
    const index = await this.env.PLUGINS_BUCKET.get("intellij-plugins.xml");

    if (index == null) {
      console.error("Failed to retrieve Intellij plugin index");
      return new Response("Not found", { status: 404 });
    }

    return this.makeResponse(index);
  }

  private async fetchIntellijPlugin(path: string): Promise<Response> {
    // archives are stored in R2
    const plugin = path.substring(path.lastIndexOf("/") + 1, path.length);
    console.log(`Fetching Intellij plugin: ${plugin}`);
    const archive = await this.env.PLUGINS_BUCKET.get(`intellij/${plugin}.zip`);

    if (archive == null) return new Response("Not found", { status: 404 });
    return this.makeResponse(archive);
  }

  private makeResponse(object: R2ObjectBody): Response {
    const headers = new Headers();
    object.writeHttpMetadata(headers);
    headers.set("etag", object.httpEtag);

    return new Response(object.body, { headers });
  }
}
