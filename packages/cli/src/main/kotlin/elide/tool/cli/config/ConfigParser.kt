package elide.tool.cli.config

import org.pkl.config.java.ConfigEvaluator
import org.pkl.config.kotlin.forKotlin
import org.pkl.config.kotlin.to
import org.pkl.core.ModuleSource
import java.nio.file.Path
import elide.tool.config.Project

object ConfigParser {
  fun parse(file: Path): Project {
    val source = ModuleSource.path(file)
    return ConfigEvaluator.preconfigured()
      .forKotlin()
      .use { it.evaluate(source).to<Project>() }
  }
}
