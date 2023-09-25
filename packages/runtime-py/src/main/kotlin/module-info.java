module elide.runtime.py {
  requires elide.graalvm;
  
  requires kotlin.stdlib;
  requires org.graalvm.polyglot;
  
  exports elide.runtime.plugins.python;
}
