/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.plugins.python.flask

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.plugins.python.Python
import elide.runtime.core.evaluate

/**
 * Engine plugin that injects a minimal Flask-compatible shim for GraalPy.
 *
 * This plugin:
 * - Injects a host bridge object as `__host__` using the standard bindings DSL.
 * - Prepares for loading a Python shim package (elide_flask) via VFS or preamble (future step).
 */
@DelicateElideApi public class FlaskPlugin private constructor(
  internal val config: FlaskConfig,
  internal val host: HostBridge,
) {
  public companion object Plugin : EnginePlugin<FlaskConfig, FlaskPlugin> {
    private const val PLUGIN_ID: String = "Flask"
    override val key: Key<FlaskPlugin> = Key(PLUGIN_ID)

    override fun install(scope: InstallationScope, configuration: FlaskConfig.() -> Unit): FlaskPlugin {
      val cfg = FlaskConfig().apply(configuration)
      val host = HostBridge()

      if (cfg.enabled) {
        // Contribute to the Python plugin's bindings using the sanctioned configuration DSL.
        scope.configuration.configure(Python) {
          bindings {
            put("__host__", host)
          }
        }

        // Evaluate a minimal Python shim that provides `elide_flask` with Flask, request, Response.
        scope.lifecycle.on(ContextInitialized) { ctx ->
          val shim = """
            |# Minimal Flask-compatible shim for Elide on GraalPy
            |import sys, types, json
            |
            |_handlers = {}
            |
            |# Ensure `Elide` is accessible from Python by resolving it from JS if needed
            |try:
            |  Elide
            |except NameError:
            |  try:
            |    import polyglot
            |    Elide = polyglot.eval("js", "Elide")
            |  except Exception:
            |    Elide = None
            |
            |class Response:
            |  def __init__(self, body, status=200, headers=None):
            |    self.status = int(status)
            |    self.headers = dict(headers or {})
            |    if isinstance(body, (dict, list)):
            |      self.body = json.dumps(body)
            |      self.headers.setdefault('Content-Type', 'application/json')
            |    else:
            |      self.body = str(body) if body is not None else ''
            |
            |class _RequestProxy(object):
            |  def _snap(self):
            |    try:
            |      return __host__.current_request() or {}
            |    except Exception:
            |      return {}
            |  @property
            |  def method(self):
            |    return (self._snap().get('method') or '').upper()
            |  @property
            |  def path(self):
            |    return self._snap().get('path') or '/'
            |  @property
            |  def headers(self):
            |    return self._snap().get('headers') or {}
            |  @property
            |  def args(self):
            |    return self._snap().get('args') or {}
            |  @property
            |  def data(self):
            |    return self._snap().get('data') or ''
            |  def get_json(self, silent=True):
            |    try:
            |      d = self.data
            |      return json.loads(d) if isinstance(d, str) and d else None
            |    except Exception:
            |      if not silent:
            |        raise
            |      return None
            |
            |request = _RequestProxy()
            |
            |def _mk_handler_id(fn):
            |  return str(id(fn))
            |
            |def _wrap(fn):
            |  def _handler(req, resp, ctx):
            |    # Build a simple request snapshot for request proxy
            |    snap = {
            |      'method': getattr(req, 'method', None),
            |      'path': getattr(req, 'uri', None) or getattr(req, 'url', None),
            |      'headers': {},
            |      'args': {},
            |      'data': ''
            |    }
            |    try:
            |      # Headers API may expose as JS FetchHeaders later; keep minimal for now
            |      pass
            |    except Exception:
            |      pass
            |    try:
            |      __host__._set_current_request(snap)
            |    except Exception:
            |      pass
            |
            |    rv = fn()
            |    status = 200
            |    headers = {}
            |    body = ''
            |
            |    if isinstance(rv, Response):
            |      status = rv.status
            |      headers = dict(rv.headers)
            |      body = rv.body
            |    elif isinstance(rv, tuple) and len(rv) == 2:
            |      body, status = rv
            |      if isinstance(body, (dict, list)):
            |        body = json.dumps(body)
            |        headers.setdefault('Content-Type', 'application/json')
            |      else:
            |        body = str(body)
            |      status = int(status)
            |    else:
            |      if isinstance(rv, (dict, list)):
            |        headers.setdefault('Content-Type', 'application/json')
            |        body = json.dumps(rv)
            |      else:
            |        body = '' if rv is None else str(rv)
            |
            |    # Apply headers then send
            |    try:
            |      for k,v in headers.items():
            |        resp.header(str(k), str(v))
            |    except Exception:
            |      pass
            |    resp.send(int(status), body)
            |    return True
            |  return _handler
            |
            |class Flask(object):
            |  def __init__(self, name):
            |    self.name = name
            |  def route(self, rule, methods=None):
            |    methods = methods or ['GET']
            |    def _decorator(fn):
            |      hid = _mk_handler_id(fn)
            |      _handlers[hid] = fn
            |      try:
            |        __host__.register_route(rule, [m.upper() for m in methods], hid)
            |      except Exception:
            |        pass
            |      # Register with Elide router so Netty can dispatch
            |      for m in methods:
            |        Elide.http.router.handle(str(m).upper(), str(rule), _wrap(fn))
            |      return fn
            |    return _decorator
            |
            |# Create a real module and export symbols
            |mod = types.ModuleType('elide_flask')
            |mod.Flask = Flask
            |mod.request = request
            |mod.Response = Response
            |sys.modules['elide_flask'] = mod
            |""".trimMargin()
          ctx.evaluate(Python, shim, name = "elide_flask.py")
        }
      }

      return FlaskPlugin(cfg, host)
    }
  }
}

