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

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package elide.tool.cli.cmd.tool.javadoc

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import jdk.javadoc.internal.tool.JavadocToolProvider
import picocli.CommandLine
import java.io.PrintWriter
import java.net.URI
import jakarta.inject.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tool.Argument
import elide.tool.Arguments
import elide.tool.Environment
import elide.tool.Inputs
import elide.tool.MutableArguments
import elide.tool.Outputs
import elide.tool.Tool
import elide.tool.asArgumentString
import elide.tool.cli.cmd.tool.AbstractGenericTool
import elide.tool.cli.cmd.tool.AbstractTool
import elide.tool.cli.cmd.tool.DelegatedToolCommand
import elide.tool.cli.cmd.tool.javadoc.JavadocTool.JavadocInputs

// Name of the `javadoc` tool.
private const val JAVADOC = "javadoc"

// Description to show for `javadoc`.
private const val JAVADOCTOOL_DESCRIPTION = "Renders Java code into documentation."

// Tool description.
private val javadoc = Tool.describe(
  name = JAVADOC,
  label = "Javadoc",
  version = System.getProperty("java.version"),
  docs = URI.create("https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/index.html"),
  description = JAVADOCTOOL_DESCRIPTION,
  helpText = """
    Usage:
        elide javadoc <elide options> -- [options] [packagenames] [sourcefiles] [@files]

    where possible elide options include:
      (None at this time.)

    where options include:
        @<file>       Read options and filenames from file
        --add-modules <module>(,<module>)*
                      Root modules to resolve in addition to the initial modules,
                      or all modules on the module path if <module> is
                      ALL-MODULE-PATH.
        -bootclasspath <path>
                      Override location of platform class files used for non-modular
                      releases
        -breakiterator
                      Compute first sentence with BreakIterator
        --class-path <path>, -classpath <path>, -cp <path>
                      Specify where to find user class files
        -doclet <class>
                      Generate output via alternate doclet
        -docletpath <path>
                      Specify where to find doclet class files
        --enable-preview
                      Enable preview language features. To be used in conjunction with
                      either -source or --release.
        -encoding <name>
                      Source file encoding name
        -exclude <pkglist>
                      Specify a list of packages to exclude
        --expand-requires (transitive|all)
                      Instructs the tool to expand the set of modules to be
                      documented. By default, only the modules given explicitly on
                      the command line will be documented. A value of "transitive"
                      will additionally include all "requires transitive"
                      dependencies of those modules. A value of "all" will include
                      all dependencies of those modules.
        -extdirs <dirlist>
                      Override location of installed extensions
        --help, -help, -?, -h
                      Display command-line options and exit
        --help-extra, -X
                      Print a synopsis of nonstandard options and exit
        -J<flag>      Pass <flag> directly to the runtime system
        --limit-modules <module>(,<module>)*
                      Limit the universe of observable modules
        -locale <name>
                      Locale to be used, e.g. en_US or en_US_WIN
        --module <module>(,<module>)*
                      Document the specified module(s)
        --module-path <path>, -p <path>
                      Specify where to find application modules
        --module-source-path <path>
                      Specify where to find input source files for multiple modules
        -package
                      Show package/protected/public types and members. For
                      named modules, show all packages and all module details.
        -private
                      Show all types and members. For named modules,
                      show all packages and all module details.
        -protected
                      Show protected/public types and members (default). For
                      named modules, show exported packages and the module's API.
        -public
                      Show only public types and members. For named modules,
                      show exported packages and the module's API.
        -quiet        Do not display status messages
        --release <release>
                      Provide source compatibility with specified release
        --show-members <value>
                      Specifies which members (fields, methods, or constructors) will be
                      documented, where value can be one of "public", "protected",
                      "package" or "private". The default is "protected", which will
                      show public and protected members, "public" will show only
                      public members, "package" will show public, protected and
                      package members and "private" will show all members.
        --show-module-contents <value>
                      Specifies the documentation granularity of module
                      declarations. Possible values are "api" or "all".
        --show-packages <value>
                      Specifies which module packages will be documented. Possible
                      values are "exported" or "all" packages.
        --show-types <value>
                      Specifies which types (classes, interfaces, etc.) will be
                      documented, where value can be one of "public", "protected",
                      "package" or "private". The default is "protected", which will
                      show public and protected types, "public" will show only
                      public types, "package" will show public, protected and
                      package types and "private" will show all types.
        --source <release>, -source <release>
                      Provide source compatibility with specified release
        --source-path <path>, -sourcepath <path>
                      Specify where to find source files
        -subpackages <subpkglist>
                      Specify subpackages to recursively load
        --system <jdk>
                      Override location of system modules used for modular releases
        --upgrade-module-path <path>
                      Override location of upgradeable modules
        -verbose      Output messages about what Javadoc is doing
        --version     Print version information
        -Werror       Report an error if any warnings occur
    
    Provided by the Standard doclet:
        --add-script <file>
                      Add a script file to the generated documentation
        --add-stylesheet <file>
                      Add a stylesheet file to the generated documentation
        --allow-script-in-comments
                      Allow JavaScript in documentation comments, and options
                      whose value is html-code
        -author       Include @author paragraphs
        -bottom <html-code>
                      Include bottom text for each page
        -charset <charset>
                      Charset for cross-platform viewing of generated documentation
        -d <directory>
                      Destination directory for output files
        -docencoding <name>
                      Specify the character encoding for the output
        -docfilessubdirs
                      Enables deep copying of 'doc-files' directories. Subdirectories and all
                      contents are recursively copied to the destination
        -doctitle <html-code>
                      Include title for the overview page
        -excludedocfilessubdir <name>,<name>,...
                      Exclude any 'doc-files' subdirectories with given name.
                      ':' can also be used anywhere in the argument as a separator.
        -footer <html-code>
                      This option is no longer supported and reports a warning
        -group <name> <g1>,<g2>...
                      Group specified elements together in overview page.
                      ':' can also be used anywhere in the argument as a separator.
        -header <html-code>
                      Include header text for each page
        -helpfile <file>
                      Specifies a file containing the text that will be displayed when the
                      help link in the navigation bar is clicked
        -html5        Generate HTML 5 output. This option is no longer required.
        --javafx, -javafx
                      Enable JavaFX functionality
        -keywords     Include HTML meta tags with package, class and member info
        -link <url>   Create links to javadoc output at <url>
        --link-modularity-mismatch (warn|info)
                      Report external documentation with wrong modularity with either
                      a warning or informational message. The default behaviour is to
                      report a warning.
        -linkoffline <url1> <url2>
                      Link to docs at <url1> using package list at <url2>
        --link-platform-properties <url>
                      Link to platform documentation URLs declared in properties file at <url>
        -linksource   Generate source in HTML
        --main-stylesheet <file>, -stylesheetfile <file>
                      File to change style of the generated documentation
        -nocomment    Suppress description and tags, generate only declarations
        -nodeprecated
                      Do not include @deprecated information
        -nodeprecatedlist
                      Do not generate deprecated list
        --no-fonts    Do not include standard web fonts in generated documentation
        -nohelp       Do not generate help link
        -noindex      Do not generate index
        -nonavbar     Do not generate navigation bar
        --no-platform-links
                      Do not generate links to the platform documentation
        -noqualifier <name1>,<name2>,...
                      Exclude the list of qualifiers from the output.
                      ':' can also be used anywhere in the argument as a separator.
        -nosince      Do not include @since information
        -notimestamp  Do not include hidden time stamp
        -notree       Do not generate class hierarchy
        --override-methods (detail|summary)
                      Document overridden methods in the detail or summary sections.
                      The default is 'detail'.
        -overview <file>
                      Read overview documentation from HTML file
        -serialwarn   Reports compile-time warnings for missing '@serial' tags
        --since <release>(,<release>)*
                      Document new and deprecated API in the specified releases
        --since-label <text>
                      Provide text to use in the heading of the "New API" page
        --snippet-path <path>
                      The path for external snippets
        -sourcetab <tab length>
                      Specify the number of spaces each tab takes up in the source
        --spec-base-url
                      Specify a base URL for relative URLs in @spec tags
        -splitindex   Split index into one file per letter
        -tag <name>:<locations>:<header>
                      Specifies a custom tag with a single argument
        -taglet       The fully qualified name of Taglet to register
        -tagletpath   The path to Taglets
        -top <html-code>
                      Include top text for each page
        -use          Create class and package usage pages
        -version      Include @version paragraphs
        -windowtitle <text>
                      Browser window title for the documentation
    
    GNU-style options may use = instead of whitespace to separate the name of an
    option from its value
  """.trimIndent()
)

// Argument names which require a value following, or separated by `=`.
private val argNamesThatExpectValues = sortedSetOf(
  // Javadoc Parameters
  "--add-modules",
  "-bootclasspath",
  "--class-path", "-classpath", "-cp",
  "-doclet",
  "-docletpath",
  "-encoding",
  "-exclude",
  "--expand-requires",
  "-extdirs",
  "--legal-notices",
  "--limit-modules",
  "--module",
  "--module-path", "-p",
  "--module-source-path",
  "--release",
  "--show-members",
  "--show-module-contents",
  "--show-packages",
  "--show-types",
  "--source", "-source",
  "--source-path", "-sourcepath",
  "-subpackages",
  "--system",
  "--upgrade-module-path",

  // Doclet Parameters (Standard)
  "--add-script",
  "--add-stylesheet",
  "-bottom",
  "-charset",
  "-d",
  "-docencoding",
  "-doctitle",
  "-excludedocfilessubdir",
  "-footer",
  "-group",
  "-header",
  "-helpfile",
  "-link",
  "--link-modularity-mismatch",
  "-linkoffline",
  "--link-platform-properties",
  "--main-stylesheet", "-stylesheetfile",
  "-noqualifier",
  "--override-methods",
  "-overview",
  "--since",
  "--since-label",
  "--snippet-path",
  "-sourcetab",
  "--spec-base-url",
  "-taglet",
  "-top",
  "-windowtitle",
)

/**
 * # Javadoc Tool
 *
 * Implements an [AbstractTool] adapter to `javadoc`. Arguments are passed to the tool verbatim from the command-line.
 */
@ReflectiveAccess @Introspected class JavadocTool private constructor (
  args: Arguments,
  env: Environment,
  override val inputs: JavadocInputs,
  override val outputs: JavadocOutputs,
): AbstractGenericTool<JavadocToolProvider, JavadocInputs, JavadocTool.JavadocOutputs>(info = javadoc.extend(
  args,
  env,
).using(
  inputs = inputs,
  outputs = outputs.flatten(),
)) {
  // Javadoc tool.
  private val tool by lazy { JavadocToolProvider() }

  // Logging.
  private val javadocLogger by lazy { Logging.of(JavadocTool::class) }

  override fun createTool(): JavadocToolProvider = tool
  override val taskDescription: String get() = "Javadoc render"
  override val toolLogger: Logger get() = javadocLogger
  override fun toolRun(out: PrintWriter, err: PrintWriter, vararg args: String): Int = createTool().run(out, err, *args)

  /**
   * Javadoc tool inputs.
   *
   * Implements understanding of inputs for `javadoc`.
   */
  sealed interface JavadocInputs: Inputs.Files {
    /**
     * Provided when no inputs are available.
     */
    data object NoInputs: JavadocInputs
  }

  /**
   * Javadoc tool outputs.
   *
   * Implements understanding of javadoc tool outputs.
   */
  sealed interface JavadocOutputs {
    /**
     * Flatten into an [Outputs] type.
     *
     * @return Outputs value.
     */
    fun flatten(): Outputs

    /**
     * Provided when no outputs are available.
     */
    data object NoOutputs: JavadocOutputs, Outputs.None {
      override fun flatten(): Outputs = this
    }
  }

  override fun amendArgs(args: MutableArguments) {
    args.filter {
      when (it) {
        is Argument.KeyValueArg -> it.name == "--legal-notices" && it.value != "none"
        is Argument.StringArg -> it.asArgumentString().let { str ->
          str.startsWith("--legal-notices") && !str.endsWith("=none")
        }
        else -> false
      }
    }.let {
      if (it.isNotEmpty()) {
        embeddedToolError(
          javadoc,
          "Elide's use of Javadoc doesn't support legal notices yet. Please omit the '--legal-notices' argument.",
        )
      }
    }
    // force the legal-notices argument to be none
    (args as MutableArguments.MutableArgumentList).add(
      Argument.of(
        "--legal-notices" to "none",
      )
    )
  }

  @CommandLine.Command(
    name = JAVADOC,
    description = [JAVADOCTOOL_DESCRIPTION],
    mixinStandardHelpOptions = false,
  )
  @Singleton
  @ReflectiveAccess
  @Introspected
  class JavadocCliTool: DelegatedToolCommand<JavadocTool>(javadoc) {
    @CommandLine.Spec override lateinit var spec: CommandLine.Model.CommandSpec

    override fun configure(args: Arguments, environment: Environment): JavadocTool = gatherArgs(
      argNamesThatExpectValues,
      args,
    ).let { _ ->
      JavadocTool(
        args = args,
        env = environment,
        inputs = JavadocInputs.NoInputs,
        outputs = JavadocOutputs.NoOutputs,
      )
    }
  }
}
