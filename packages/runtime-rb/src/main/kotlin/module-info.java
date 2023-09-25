module elide.runtime.rb {
  requires elide.graalvm;
  
  requires kotlin.stdlib;
  requires org.graalvm.polyglot;
  
  exports elide.runtime.plugins.ruby;
}
