export default {
  async fetch(request: Request): Promise<Response> {
    return new Response("Hello from Elide server!", { status: 200 });
  }
}

