#  Copyright (c) 2023 Elide Ventures, LLC.
#
#  Licensed under the MIT license (the "License"); you may not use this file except in compliance
#  with the License. You may obtain a copy of the License at
#
#    https://opensource.org/license/mit/
#
#  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
#  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations under the License.
#
#  Licensed under the MIT license (the "License"); you may not use this file except in compliance
#  with the License. You may obtain a copy of the License at
#
#    https://opensource.org/license/mit/
#
#  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
#  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations under the License.

# noinspection PyPep8Naming
class _Elide_ApplicationEnvironment(object):

    """Handler for resolving application-level environment, and withholding system environment, as needed."""

    __app_environ = {}
    __virtual_env = {}

    def __init__(self):
        try:
            import polyglot
            data = (polyglot.import_value("elide_app_environment") or (lambda: {}))()
            self.__app_environ = {x: data[x] for x in data}
        except Exception:
            pass  # silent fail

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
