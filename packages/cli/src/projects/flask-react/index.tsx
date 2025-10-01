import { renderToString } from "react-dom/server";
import React from "react";

type SnippetKey = "bash" | "python" | "tsx";

const BASH_SNIPPET = `$ elide app.py`;

const PYTHON_SNIPPET = `
from elide import Flask, react, request

# create and configure an app
app = Flask(__name__)
home = react("index.tsx")

# basic app routing
@app.route("/")
def hello_world():
    user_agent = request.headers["User-Agent"]
    return home(user_agent=user_agent)
`.trim();

const REACT_SNIPPET = `
export default async function render(props): Promise<string> {
  return renderToString(
    <App
      nowISO={new Date().toISOString()}
      ua={props.user_agent ?? ""}
      reqId={Date.now()}
      pid={process.pid}
    />,
  );
}
`.trim();

const SNIPPETS: Array<{
  key: SnippetKey;
  title: string;
  subtitle: string;
  language: string;
  code: string;
}> = [
  {
    key: "bash",
    title: "Run in one line",
    subtitle:
      "Launch the server without any additional steps: no virtual environments, no special commands",
    language: "bash",
    code: BASH_SNIPPET,
  },
  {
    key: "python",
    title: "Use your existing Python server code",
    subtitle: "Serve React markup from the Flask apps you already have",
    language: "python",
    code: PYTHON_SNIPPET,
  },
  {
    key: "tsx",
    title: "Tap into the React ecosystem",
    subtitle: "Render React components server-side with Elide",
    language: "typescript",
    code: REACT_SNIPPET,
  },
];

function App(props: {
  nowISO: string;
  ua: string;
  reqId: string;
  pid: number;
}) {
  return (
    <html lang="en">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>Pure React SSR · No Client JS</title>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link
          rel="preconnect"
          href="https://fonts.gstatic.com"
          crossOrigin=""
        />
        <link
          href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&family=Roboto:wght@400;500;700&family=Roboto+Condensed:wght@700&display=swap"
          rel="stylesheet"
        />
        <link rel="stylesheet" href="/static/styles.css" />
        <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/highlight.min.js"></script>
        <script>hljs.highlightAll();</script>
      </head>
      <body>
        <header>
          <a className="hero" href="/" aria-label="Home">
            <img
              className="logo"
              alt="Elide logo"
              src="/static/elide-logo.svg"
            />
            <span>Powered by Elide</span>
          </a>
          <h1>React SSR + Flask delivered by Elide</h1>
          <p className="tagline">
            This app renders entirely on the server, it is written using Flask
            and is served by Elide's new polyglot HTTP engine
          </p>
        </header>

        <main>
          <section className="grid">
            <div className="card">
              <h2>Request information</h2>
              <p className="lead">
                This section is generated on the server for each request
              </p>
              <ul>
                <li>
                  Render time: <strong>{props.nowISO}</strong>
                </li>
                <li>
                  Request ID: <strong>{props.reqId}</strong>
                </li>
                <li>
                  Process PID: <strong>{props.pid}</strong>
                </li>
                <li>
                  User-Agent: <strong>{props.ua || "(unknown)"}</strong>
                </li>
              </ul>
            </div>
          </section>

          <section className="code-sections">
            {SNIPPETS.map((snippet) => (
              <div className="code-card" key={snippet.key}>
                <h3>{snippet.title}</h3>
                <p className="snippet-subtitle">{snippet.subtitle}</p>
                <pre>
                  <code className={`language-${snippet.language}`}>
                    {snippet.code}
                  </code>
                </pre>
              </div>
            ))}
          </section>
        </main>

        <footer>
          Built with <span className="kbd">React 18</span> and Flask on Elide.
        </footer>
      </body>
    </html>
  );
}

export default async function render({ get_user_agent }): Promise<string> {
  return renderToString(
    <App
      nowISO={new Date().toISOString()}
      ua={get_user_agent() ?? ""}
      reqId={Date.now()}
      pid={process.pid}
    />,
  );
}
