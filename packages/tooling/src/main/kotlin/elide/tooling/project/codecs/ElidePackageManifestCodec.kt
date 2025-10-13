/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.tooling.project.codecs

import org.pkl.config.java.ConfigEvaluatorBuilder
import org.pkl.config.java.mapper.Conversion
import org.pkl.config.java.mapper.Converter
import org.pkl.config.java.mapper.ValueMapper
import org.pkl.config.kotlin.forKotlin
import org.pkl.config.kotlin.to
import org.pkl.core.ModuleSource
import org.pkl.core.PClassInfo
import org.pkl.core.PObject
import org.pkl.core.SecurityManager
import org.pkl.core.module.ModuleKey
import org.pkl.core.module.ModuleKeyFactory
import org.pkl.core.module.ResolvedModuleKey
import org.pkl.core.resource.ResourceReader
import org.pkl.core.util.IoUtils
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import elide.tooling.deps.PackageSpec
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.codecs.PackageManifestCodec.ManifestBuildState
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.*
import elide.tooling.web.Browsers

@ManifestCodec(ProjectEcosystem.Elide)
public class ElidePackageManifestCodec : PackageManifestCodec<ElidePackageManifest> {
  // Enables resolution of resources from the `elide:` protocol scheme.
  public class ElidePklResourceReader: ResourceReader {
    override fun getUriScheme(): String = "elide"
    override fun isGlobbable(): Boolean = true
    override fun hasHierarchicalUris(): Boolean = true

    override fun read(p0: URI): Optional<in Any> {
      TODO("Not yet implemented: Read Elide pkl resource")
    }
  }

  public class ElideBuiltinModuleKey (private val uri: URI): ModuleKey, ResolvedModuleKey {
    private companion object {
      private const val PKL_BUILTIN_PREFIX = "/META-INF/elide/pkl/"
    }
    override fun getUri(): URI = uri
    override fun getOriginal(): ModuleKey = this
    override fun resolve(p0: SecurityManager): ResolvedModuleKey = this
    override fun hasHierarchicalUris(): Boolean = false
    override fun isGlobbable(): Boolean = false

    override fun loadSource(): String {
      val relativePath = uri.toString().removePrefix("elide:")
      return IoUtils.readClassPathResourceAsString(
        ElidePackageManifestCodec::class.java,
        "$PKL_BUILTIN_PREFIX$relativePath",
      ).also {
        requireNotNull(it.ifBlank { null }) {
          "Failed to load builtin Pkl module: $uri"
        }
      }.lineSequence().map {
        // fix: rewrite imports to be internal
        if (it.startsWith("import ")) {
          val importPath = it.substringAfter("import \"")
          "import \"elide:$importPath"
        } else {
          it
        }
      }.joinToString("\n")
    }
  }

  // Enables module loading for the `elide:` protocol scheme.
  public class ElidePklModuleKeyFactory: ModuleKeyFactory {
    override fun create(uri: URI): Optional<ModuleKey> {
      if (uri.scheme == "elide") {
        return ElideBuiltinModuleKey(uri).let { Optional.of(it) }
      }
      return Optional.empty()
    }
  }

  private fun interface StrConverter<T>: Converter<String, T> {
    override fun convert(value: String, mapper: ValueMapper): T & Any {
      return fromString(value)
    }

    fun fromString(value: String): T & Any
  }

  private fun interface ListConverter<I, T>: Converter<List<*>, T> {
    @Suppress("UNCHECKED_CAST")
    override fun convert(value: List<*>, mapper: ValueMapper): T & Any {
      return fromString(value as List<I>)
    }

    fun fromString(value: List<I>): T & Any
  }

  private fun interface LongConverter<T>: Converter<Long, T> {
    override fun convert(value: Long, mapper: ValueMapper): T & Any {
      return fromInt(value)
    }

    fun fromInt(value: Long): T & Any
  }

  private val valueMapper by lazy {
    ValueMapper.preconfigured()
      .toBuilder()
      .forKotlin()
      .addConversion(
        // convert string npm package specifications
        Conversion.of(PClassInfo.String, NpmPackage::class.java, StrConverter {
          NpmPackage.parse(it)
        })
      ).addConversion(
        // convert string maven package specifications
        Conversion.of(PClassInfo.String, MavenPackage::class.java, StrConverter {
          MavenPackage.parse(it)
        })
      ).addConversion(
        // convert string maven uris to repositories
        Conversion.of(PClassInfo.String, MavenRepository::class.java, StrConverter {
          MavenRepository.parse(it)
        })
      ).addConversion(
        // convert string pip deps
        Conversion.of(PClassInfo.String, PipPackage::class.java, StrConverter {
          PackageSpec.PipPackageSpec.parse(it).asPipPackage()
        })
      ).addConversion(
        // convert int jvm target specs to jvm target
        Conversion.of(PClassInfo.Int, JvmTarget::class.java, LongConverter {
          JvmTarget.NumericJvmTarget(it.toUInt())
        })
      ).addConversion(
        // convert string jvm target specs to jvm target
        Conversion.of(PClassInfo.String, JvmTarget::class.java, StrConverter {
          JvmTarget.StringJvmTarget(it)
        })
      ).addConversion(
        // convert string to source set spec
        Conversion.of(PClassInfo.String, SourceSet::class.java, StrConverter {
          SourceSet.parse(it)
        })
      ).addConversion(
        // convert string to gradle catalog spec
        Conversion.of(PClassInfo.String, GradleCatalog::class.java, StrConverter {
          GradleCatalog.parse(it)
        })
      ).addConversion(
        // convert string to list of file paths
        Conversion.of(PClassInfo.String, List::class.java, StrConverter {
          listOf(it)
        })
      ).addConversion(
        // convert optimization mode enums
        Conversion.of(PClassInfo.String, OptimizationLevel::class.java, StrConverter {
          OptimizationLevel.resolve(it)
        })
      ).addConversion(
        // convert image type enums
        Conversion.of(PClassInfo.String, NativeImageType::class.java, StrConverter {
          NativeImageType.resolve(it)
        })
      ).addConversion(
        // convert image type enums
        Conversion.of(PClassInfo.String, Browsers::class.java, StrConverter {
          Browsers.parse(it)
        })
      ).addConversion(
        // convert image type enums
        Conversion.of(PClassInfo.List, Browsers::class.java, ListConverter<String, Browsers> { them ->
          Browsers.parse(them)
        })
      ).addConverterFactory { info, _ ->
        when (info.qualifiedName) {
          "elide.jvm#Jar" -> Optional.of(Converter { value: PObject, mapper ->
            mapper.map(value, Jar::class.java)
          })

          "elide.nativeImage#NativeImage" -> Optional.of(Converter { value: PObject, mapper ->
            mapper.map(value, NativeImage::class.java)
          })

          "elide.containers#ContainerImage" -> Optional.of(Converter { value: PObject, mapper ->
            mapper.map(value, ContainerImage::class.java)
          })

          "elide.web#StaticSite" -> Optional.of(Converter { value: PObject, mapper ->
            mapper.map(value, StaticSite::class.java)
          })

          else -> Optional.empty()
        }
      }
      .build()
  }

  override fun defaultPath(): Path = Path("$DEFAULT_NAME.$DEFAULT_EXTENSION")

  override fun supported(path: Path): Boolean {
    return path.nameWithoutExtension == DEFAULT_NAME && path.extension == DEFAULT_EXTENSION
  }

  private fun propKey(name: String): String = "elide.build.state.$name"

  private fun safeBuildEnv(): Map<String, String> = System.getenv()

  private fun buildStateProperties(state: ManifestBuildState): Map<String, String> = buildMap {
    put(propKey("release"), state.isRelease.toString())
    put(propKey("debug"), state.isDebug.toString())
  }

  override fun parse(source: InputStream, state: ManifestBuildState): ElidePackageManifest {
    return source.bufferedReader().use {
      it.lineSequence().mapIndexed { index, line ->
        // fix: special case elide's import of its own pkl project structure.
        when {
          // TODO this is hacky
          index != 0 -> line
          else -> if (line != "amends \"./packages/cli/src/main/pkl/Project.pkl\"") line else {
            "amends \"elide:Project.pkl\""
          }
        }
      }.joinToString("\n")
    }.let { text ->
      ConfigEvaluatorBuilder
        .preconfigured()
        .setEnvironmentVariables(emptyMap())  // zero-out build-time environment
        .setEnvironmentVariables(safeBuildEnv())
        .apply {
          evaluatorBuilder.resourceReaders.add(ElidePklResourceReader())
          evaluatorBuilder.moduleKeyFactories.add(ElidePklModuleKeyFactory())
          evaluatorBuilder.addExternalProperties(buildStateProperties(state))
        }
        .build()
        .setValueMapper(valueMapper)
        .forKotlin()
        .use { it.evaluate(ModuleSource.text(text)) }
        .to<ElidePackageManifest>()
    }
  }

  override fun write(manifest: ElidePackageManifest, output: OutputStream) {
    error("Writing elide manifests is not yet supported")
  }

  override fun fromElidePackage(source: ElidePackageManifest): ElidePackageManifest = source.copy()
  override fun toElidePackage(source: ElidePackageManifest): ElidePackageManifest = source.copy()

  private companion object {
    const val DEFAULT_EXTENSION = "pkl"
    const val DEFAULT_NAME = "elide"
  }
}
