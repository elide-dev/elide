module elide.runtime.js {
  requires elide.runtime.core;
  
  requires kotlin.stdlib;
  requires org.graalvm.sdk;
  
  exports elide.runtime.plugins.js;
}