# Copyright (c) 2023 Elide Ventures, LLC.
#
# Licensed under the MIT license (the "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   https://opensource.org/license/mit/
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under the License.

module AppEnv
  # Fetches the application-specific environment variables during static
  # initialization. This is meant to replace and shadow the host's environment.
  elide_app_environment = Truffle::Interop.import("elide_app_environment")
  @private_env_map = elide_app_environment.call || {}

  class << self
    # Retrieves the value of the given environment variable key.
    #
    # @param key [String] The environment variable key.
    # @return [String, nil] The value of the environment variable, or nil if it doesn't exist.
    def [](key)
      @private_env_map[key]
    end

    # Sets the value for the given environment variable key.
    #
    # @param key [String] The environment variable key.
    # @param value [String] The value to set for the given key.
    # @return [String] The value that was set.
    def []=(key, value)
      @private_env_map[key] = value
    end

    # Fetches the value of the given environment variable key, providing
    # optional default behaviors in case the key doesn't exist.
    #
    # @param key [String] The environment variable key.
    # @param args [Array] Optional values or blocks to handle the absence of the key.
    # @yield Provides a block to handle the absence of the key.
    # @return [String] The value of the environment variable or the result of the block or default value.
    def fetch(key, *args, &block)
      @private_env_map.fetch(key, *args, &block)
    end

    # Checks if the given key exists in the environment variables.
    #
    # @param key [String] The environment variable key to check.
    # @return [Boolean] True if the key exists, false otherwise.
    def key?(key)
      @private_env_map.key?(key)
    end
  end
end

# Override the default Ruby ENV object with AppEnv.
Object.send(:remove_const, :ENV)
ENV = AppEnv
