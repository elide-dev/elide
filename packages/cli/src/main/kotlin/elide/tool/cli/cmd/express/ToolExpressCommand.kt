package elide.tool.cli.cmd.express

import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.Logging
import elide.runtime.intrinsics.js.express.Express
import elide.tool.cli.GuestLanguage
import elide.tool.cli.ToolState
import elide.tool.cli.cmd.AbstractSubcommand
import org.graalvm.polyglot.Source
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Phaser

/** Express.js entrypoint for Elide on the command-line. */
@Command(
  name = "serve",
  aliases = ["express"],
  description = ["%nRun an express.js app"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  synopsisHeading = "",
  customSynopsis = [
    "",
    " Usage:  elide @|bold,fg(cyan) serve|@ [OPTIONS] FILE",
  ]
)
@Singleton internal class ToolExpressCommand : AbstractSubcommand<ToolState>() {
  private val logging by lazy { Logging.of(ToolExpressCommand::class) }
  
  @Inject private lateinit var express: Express

  /** File to run within the VM. */
  @Parameters(
    index = "0",
    arity = "1",
    paramLabel = "FILE",
    description = ["File to run as entrypoint."],
  )
  internal var entrypoint: String? = null
  
  private fun readEntrypoint(): Source {
    val script = File(checkNotNull(entrypoint) { "Entrypoint must not be null" })
    logging.debug("Reading script source at path '$entrypoint'")
    
    check(script.exists()) {"Script file does not exist"}
    logging.trace("Script check: File exists")
    
    check(script.canRead()) { "Script file cannot be read" }
    logging.trace("Script check: File is readable")
    
    check(script.isFile) { "Script file is not a regular file" }
    logging.trace("Script check: File is a regular file")

    // type check: first, check file extension
    check(script.extension == ENTRY_POINT_EXTENSION) { "Only javascript files are supported for the entry point" }
    logging.trace("Script check: File extension matches")

    return Source.newBuilder(ENTRY_POINT_LANGUAGE, script)
      .encoding(StandardCharsets.UTF_8)
      .internal(false)
      .build()
  }

  override fun initializeVM(base: ToolState): Boolean {
    // override so the VM factory is initialized by the base class
    vmFactory.acquireVM(GuestLanguage.JS)
    return true
  }
  
  override fun invoke(context: ToolContext<ToolState>) {
    logging.debug("Express command invoked")
    
    logging.debug("Reading entrypoint source")
    val source = readEntrypoint() 
    
    // synchronization helper
    val phaser = Phaser(1)
    
    withVM(
      context,
      userBundles = emptyList(),
      systemBundles = emptyList(),
      hostIO = false,
    ) { vm ->
      logging.debug("Entered VM execution context")
      
      // initialize the Express intrinsic
      express.initialize(vm, phaser)

      // parse the source
      val parsed = runCatching {
        logging.debug("Parsing entrypoint source")
        vm.parse(source)
      }.getOrElse { cause ->
        logging.error("Failed to parse entrypoint source", cause)
        throw cause
      }
      
      // sanity check
      if(!parsed.canExecute()) {
        logging.error("Parsed entrypoint is not executable, aborting")
        return@withVM
      }
      
      // execute the script
      logging.debug("Executing parsed source")
      parsed.executeVoid()
      logging.debug("Finished entrypoint execution")
    }
    
    // wait for all tasks to arrive
    logging.debug("Waiting for long-lived tasks to arrive")
    phaser.arriveAndAwaitAdvance()
    logging.debug("Exiting")
  }
  
  private companion object {
    const val ENTRY_POINT_EXTENSION = "js"
    const val ENTRY_POINT_LANGUAGE = "js"
  }
}
