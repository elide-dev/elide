export default {
  async fetch(request) {
    return new Response("Hello from Elide server (mts)!", { status: 200 });
  }
}

