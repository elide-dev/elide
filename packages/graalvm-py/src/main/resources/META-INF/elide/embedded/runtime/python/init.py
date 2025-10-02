#  Copyright (c) 2022-2025 Elide Technologies, Inc.
#
#  Licensed under the MIT license (the "License"); you may not use this file except in compliance
#  with the License. You may obtain a copy of the License at
#
#    https://opensource.org/license/mit/
#
#  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
#  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations under the License.

request = None
abort = None
url_for = None
make_response = None

def _private_symbol_name(name):
    return "__Elide_%s__" % name

def __init__interop():
  import sys
  import polyglot
  from types import ModuleType

  POLYGLOT_MODULE = "polyglot"
  POLYGLOT_DECORATOR = "poly"
  BIND_DECORATOR = "bind"

  FLASK_ENTRY = "FlaskIntrinsic"
  FLASK_GLOBAL = "Flask"
  FLASK_ABORT = "abort"
  FLASK_REQUEST = "request"
  FLASK_URL_FOR = "url_for"
  FLASK_MAKE_RESPONSE = "make_response"
  FLASK_REDIRECT = "redirect"
  FLASK_REACT = "react"

  MODULE_NAME = "elide"

  registered_py_symbols = {}
  def bind_factory(name = None):
    def binder(obj):
      symbol = name or obj.__name__
      if symbol in registered_py_symbols:
        raise ValueError(f"Symbol '{symbol}' already bound for polyglot access.")

      # register the symbol
      polyglot.export_value(
        symbol,
        obj
      )
      return obj
    return binder

  def bind_decorator(obj):
    return bind_factory()(obj)

  def polyglot_decorator(name = None):
    return bind_factory(name)

  def flask_binding():
    return polyglot.import_value(_private_symbol_name(FLASK_ENTRY))

  def react(source_path: str):
    flask = flask_binding()
    entrypoint = flask.react_template(source_path)

    def render(**params):
      return entrypoint(params)

    return render

  def redirect(target):
    response = flask_binding().make_response()
    response.status = 302
    response.headers["Location"] = target
    return response

  def url_for(endpoint, **variables):
    return flask_binding().url_for(endpoint, variables)

  def flask_factory(name):
    elide_flask = flask_binding()
    _flask_class = elide_flask

    class FlaskBridge:
      def __init__(self, name: str):
        self.bridge = _flask_class(name)

      def route(self, rule: str, **options):
        def handler_wrapper(handler):
          methods = options.get("methods", ["GET"])
          if hasattr(handler, "_elide_flask_handler"):
            # already wrapped, just register it
            self.bridge.route(handler.__name__, rule, methods, handler)
            return handler

          # not wrapped yet
          def dispatcher(args):
            return handler(**args)

          dispatcher._elide_flask_handler = True
          self.bridge.route(handler.__name__, rule, methods, dispatcher)

          return dispatcher
        return handler_wrapper

      def head(self, rule: str):
        return self.route(rule, methods=["HEAD"])

      def options(self, rule: str):
        return self.route(rule, methods=["OPTIONS"])

      def get(self, rule: str):
        return self.route(rule, methods=["GET"])

      def post(self, rule: str):
        return self.route(rule, methods=["POST"])

      def put(self, rule: str):
        return self.route(rule, methods=["PUT"])

      def patch(self, rule: str):
        return self.route(rule, methods=["PATCH"])

      def delete(self, rule: str):
        return self.route(rule, methods=["DELETE"])

      def bind(self):
        self.bridge.bind()

    return FlaskBridge(name)

  class ElideModule(ModuleType):
    """Module for Elide integration with Python."""

    def __init__(self):
      super(ElideModule, self).__init__(MODULE_NAME)

      self.__dict__.update({
        "__package__": MODULE_NAME,
        "__path__": [],
        "__doc__": "Elide Python interoperability module",
        BIND_DECORATOR: bind_decorator,
        POLYGLOT_MODULE: polyglot,
      })

    def __getattr__(self, name):
      if name == BIND_DECORATOR:
        return bind_decorator
      if name == POLYGLOT_MODULE:
        return polyglot
      if name == POLYGLOT_DECORATOR:
        return polyglot_decorator
      if name == FLASK_GLOBAL:
        return flask_factory
      if name == FLASK_ABORT:
        return flask_binding().request.abort
      if name == FLASK_REQUEST:
        return flask_binding().request
      if name == FLASK_URL_FOR:
        return url_for
      if name == FLASK_MAKE_RESPONSE:
        return flask_binding().make_response
      if name == FLASK_REDIRECT:
        return redirect
      if name == FLASK_REACT:
        return react
      if name == "wsgi":
        return flask_binding().wsgi
      raise AttributeError(f"module '{MODULE_NAME}' has no attribute '{name}'")

    def __dir__(self):
      return [BIND_DECORATOR, POLYGLOT_MODULE, POLYGLOT_DECORATOR, FLASK_GLOBAL]

  elide_module = ElideModule()
  sys.modules[MODULE_NAME] = elide_module
  __init_elide_env()

def __init_elide_env():
  import polyglot
  APP_ENV = "app_env"

  # noinspection PyPep8Naming
  class _Elide_ApplicationEnvironment(object):
      """Handler for resolving application-level environment, and withholding system environment, as needed."""

      __app_environ = {}
      __virtual_env = {}

      def __init__(self):
          data = polyglot.import_value(_private_symbol_name(APP_ENV))
          if data is not None:
              self.__app_environ = {x: data[x] for x in data}

      def contains_key(self, item):
          return item in self.__app_environ or item in self.__virtual_env

      def all_keys(self):
          merged = set([x for x in self.__app_environ])
          for x in self.__virtual_env:
              merged.add(x)
          return merged

      def __contains__(self, item):
          return self.contains_key(item)

      def __getitem__(self, item):
          return self.__virtual_env.get(item) or self.__app_environ.__getitem__(item)

      def __setitem__(self, item, value):
          self.__virtual_env[item] = value

      def __repr__(self):
          return "Environ(%s)" % ", ".join(self.all_keys())

      def __dir__(self):
          return [
              "get",
              "contains_key",
              "all_keys",
              "__getitem__",
              "__setitem__",
              "__repr__",
              "__contains__",
              "__dir__",
          ]

      def get(self, item, default=None):
          if item in self:
              return self[item]
          return default

      @classmethod
      def __patch(cls, singleton):
          """Patch the OS environment component, if it has not been patched yet."""
          import os

          if not getattr(cls, "_patched", False):
              os.environ = singleton
              setattr(cls, "_patched", True)

      @classmethod
      def __singleton(cls):
          """Resolve or create the application environment singleton."""
          if getattr(cls, "_singleton", None) is None:
              singleton = _Elide_ApplicationEnvironment()
              setattr(cls, "_singleton", singleton)
          return getattr(cls, "_singleton")

      @classmethod
      def install(cls):
          """Install the monkey-patched environment handler."""
          cls.__patch(cls.__singleton())

  # Install unconditionally.
  _Elide_ApplicationEnvironment.install()

__init__interop()
