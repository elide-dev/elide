module elide.base {
  requires kotlin.stdlib;
  requires org.slf4j;

  exports elide.annotations ;
  exports elide.runtime ;
  exports elide.runtime.jvm ;
  exports elide.util ;
}
