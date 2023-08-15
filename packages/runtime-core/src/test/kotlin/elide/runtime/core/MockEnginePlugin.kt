package elide.runtime.core

import elide.runtime.core.EnginePlugin.Key

@OptIn(DelicateElideApi::class)
internal abstract class MockEnginePlugin(id: String = "TestPlugin") : EnginePlugin<Unit, Unit> {
  override val key: Key<Unit> = Key(id)
}
