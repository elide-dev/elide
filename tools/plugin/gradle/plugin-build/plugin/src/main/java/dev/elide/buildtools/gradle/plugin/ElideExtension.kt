package dev.elide.buildtools.gradle.plugin

import org.gradle.api.Project
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class ElideExtension @Inject constructor(project: Project) {
    companion object {
        const val DEFAULT_OUTPUT_FILE = "template-example.txt"
    }

    private val objects = project.objects

    // Example of a property that is mandatory. The task will
    // fail if this property is not set as is annotated with @Optional.
//    val message: Property<String> = objects.property(String::class.java)

    // Example of a property that is optional.
//    val tag: Property<String> = objects.property(String::class.java)

    // Example of a property with a default set with .convention
//    val outputFile: RegularFileProperty = objects.fileProperty().convention(
//        project.layout.buildDirectory.file(DEFAULT_OUTPUT_FILE)
//    )
}
