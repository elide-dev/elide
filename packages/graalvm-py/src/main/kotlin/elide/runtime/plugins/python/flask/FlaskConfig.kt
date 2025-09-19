/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.plugins.python.flask

import elide.runtime.core.DelicateElideApi

/** Configuration for the Flask compatibility plugin. */
@DelicateElideApi public class FlaskConfig internal constructor() {
  /** Feature flag to enable or disable the Flask shim. Enabled by default. */
  public var enabled: Boolean = true
}

