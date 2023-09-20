module elide.runtime.rb {
  requires elide.runtime.core;
  
  requires kotlin.stdlib;
  requires org.graalvm.polyglot;
  
  exports elide.runtime.plugins.ruby;
}
