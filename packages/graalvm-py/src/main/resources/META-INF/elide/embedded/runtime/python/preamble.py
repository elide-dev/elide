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

def __init__interop():
  import sys
  import polyglot
  import importlib.abc
  import importlib.util
  from types import ModuleType

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

  flask_global = polyglot.import_value("_ElideFlask")

  POLYGLOT_MODULE = "polyglot"
  POLYGLOT_DECORATOR = "poly"
  BIND_DECORATOR = "bind"
  FLASK_GLOBAL = "Flask"
  MODULE_NAME = "elide"

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
        return flask_global
      raise AttributeError(f"module '{MODULE_NAME}' has no attribute '{name}'")

    def __dir__(self):
      return [BIND_DECORATOR, POLYGLOT_MODULE, POLYGLOT_DECORATOR]

  class ElideModuleFinder(importlib.abc.MetaPathFinder):
    def find_spec(self, fullname, _path, _target = None):
      if fullname == MODULE_NAME:
        loader = ElideModuleLoader()
        return importlib.util.spec_from_loader(fullname, loader)
      return None

  class ElideModuleLoader(importlib.abc.Loader):
    def create_module(self, _spec):
      return elide_module

    def exec_module(self, module):
      pass

  elide_module = ElideModule()
  sys.meta_path.insert(0, ElideModuleFinder())
  sys.modules[MODULE_NAME] = elide_module

__init__interop()
