package elide.runtime.intrinsics.secrets

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.API
import elide.vm.annotations.Polyglot

/** @author Lauri Heino <datafox> */
@API public interface SecretsAPI : ProxyObject {
  @Polyglot public fun get(
    name: Value,
  ): Value
}
