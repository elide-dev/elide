package elide.runtime.intrinsics.js.express

import elide.runtime.intrinsics.js.ServerAgent

/**
 * Interface used as the root `express` module, which exports a function as default value. Invoking the function
 * returns a new [ExpressApp].
 */
public interface Express : ServerAgent
