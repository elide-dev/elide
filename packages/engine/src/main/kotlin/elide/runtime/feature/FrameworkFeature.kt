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
@file:Suppress("SpreadOperator")

package elide.runtime.feature

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import java.io.IOException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.JarURLConnection
import elide.runtime.Logger
import elide.runtime.Logging

/**
 * # Framework: Feature
 *
 * Registers a GraalVM "feature" implementation, which describes to the VM and compiler how to access reflective values
 * needed for operation of apps built with Elide. Each framework implements a description for a different unit of
 * functionality.
 */
public interface FrameworkFeature : Feature {
  /**
   * @return Logger to use for this feature.
   */
  public fun logging(): Logger {
    return Logging.Companion.of(FrameworkFeature::class)
  }

  /**
   * @return Description for this feature to display in the console.
   */
  override fun getDescription(): String

  /** @inheritDoc */
  override fun getURL(): String = "https://elide.dev"

  /**
   * Returns the method of a class or fails if it is not present.
   */
  public fun getMethodOrFail(clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method {
    return requireNotNull(clazz.getDeclaredMethod(methodName, *params))
  }

  /**
   * Registers a class for reflective construction via its default constructor.
   */
  public fun registerForReflectiveInstantiation(access: Feature.FeatureAccess, className: String) {
    val clazz = access.findClassByName(className)
    if (clazz != null) {
      RuntimeReflection.register(clazz)
      RuntimeReflection.registerForReflectiveInstantiation(clazz)
    } else {
      logging().warning(
        "Failed to find $className on the classpath for reflective instantiation."
      )
    }
  }

  /**
   * Registers all constructors of a class for reflection.
   */
  public fun registerConstructorsForReflection(access: Feature.FeatureAccess, name: String) {
    val clazz = access.findClassByName(name)
    if (clazz != null) {
      RuntimeReflection.register(clazz)
      RuntimeReflection.register(*clazz.declaredConstructors)
    } else {
      logging().warning(
        "Failed to find $name on the classpath for reflection."
      )
    }
  }

  /**
   * Registers an entire class for reflection use.
   */
  public fun registerClassForReflection(access: Feature.FeatureAccess, name: String) {
    val clazz = access.findClassByName(name)
    if (clazz != null) {
      RuntimeReflection.register(clazz)
      RuntimeReflection.register(*clazz.declaredConstructors)
      RuntimeReflection.register(*clazz.declaredFields)
      RuntimeReflection.register(*clazz.declaredMethods)
    } else {
      logging().warning(
        "Failed to find $name on the classpath for reflection."
      )
    }
  }

  /**
   * Registers the transitive class hierarchy of the provided `className` for reflection.
   *
   *
   * The transitive class hierarchy contains the class itself and its transitive set of
   * *non-private* nested subclasses.
   */
  public fun registerClassHierarchyForReflection(access: Feature.FeatureAccess, className: String) {
    val clazz = access.findClassByName(className)
    if (clazz != null) {
      registerClassForReflection(access, className)
      for (nestedClass: Class<*> in clazz.declaredClasses) {
        if (!Modifier.isPrivate(nestedClass.modifiers)) {
          registerClassHierarchyForReflection(access, nestedClass.name)
        }
      }
    } else {
      logging().warning(
        "Failed to find $className on the classpath for reflection."
      )
    }
  }

  /**
   * Registers a class for unsafe reflective field access.
   */
  public fun registerForUnsafeFieldAccess(access: Feature.FeatureAccess, className: String, vararg fields: String) {
    val clazz = access.findClassByName(className)
    if (clazz != null) {
      RuntimeReflection.register(clazz)
      for (fieldName: String in fields) {
        try {
          RuntimeReflection.register(clazz.getDeclaredField(fieldName))
        } catch (ex: NoSuchFieldException) {
          logging().warning("Failed to register field $fieldName for class $className: '${ex.message}'")
        }
      }
    } else {
      logging().warning(
        "Failed to find " + className +
          " on the classpath for unsafe fields access registration."
      )
    }
  }

  /**
   * Registers all the classes under the specified package for reflection.
   */
  public fun registerPackageForReflection(access: Feature.FeatureAccess, packageName: String) {
    val classLoader = Thread.currentThread().contextClassLoader
    val path = packageName.replace('.', '/')
    val resources = classLoader.getResources(path)
    while (resources.hasMoreElements()) {
      val url = resources.nextElement()
      val connection = url.openConnection()
      if (connection is JarURLConnection) {
        val classes = findClassesInJar(connection, packageName)
        for (className: String in classes) {
          registerClassHierarchyForReflection(access, className)
        }
      }
    }
  }

  /**
   * Find a list of classes for the provided [packageName] within the provided [urlConnection].
   *
   * @param urlConnection JAR connection to traverse.
   * @param packageName Package name to search for.
   * @return Matching set of classes.
   */
  @Throws(IOException::class)
  public fun findClassesInJar(urlConnection: JarURLConnection, packageName: String): List<String> {
    val result: MutableList<String> = ArrayList()
    val jarFile = urlConnection.jarFile
    val entries = jarFile.entries()
    while (entries.hasMoreElements()) {
      val entry = entries.nextElement()
      val entryName = entry.name
      if (entryName.endsWith(".class")) {
        val javaClassName = entryName
          .replace(".class", "")
          .replace('/', '.')
        if (javaClassName.startsWith(packageName)) {
          result.add(javaClassName)
        }
      }
    }
    return result
  }
}
