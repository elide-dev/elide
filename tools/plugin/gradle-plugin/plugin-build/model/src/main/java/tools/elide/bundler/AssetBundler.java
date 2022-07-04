/*
 * Copyright © 2022, The Elide Framework Authors. All rights reserved.
 *
 * The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
 * are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
 * this code in object or source form requires and implies consent and agreement to that license in principle and
 * practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
 * Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
 * Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
 * by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
 * is strictly forbidden except in adherence with assigned license requirements.
 */
package tools.elide.bundler;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.template.soy.data.SanitizedContent;
import com.google.template.soy.data.UnsafeSanitizedContentOrdainer;
import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import com.nixxcode.jvmbrotli.enc.Encoder;
import kotlin.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import tools.elide.assets.AssetBundle;
import tools.elide.assets.AssetBundle.StyleBundle;
import tools.elide.assets.AssetBundle.ScriptBundle;
import tools.elide.crypto.HashAlgorithm;
import tools.elide.data.CompressedData;
import tools.elide.data.CompressionMode;
import tools.elide.data.DataContainer;
import tools.elide.data.DataFingerprint;
import tools.elide.page.Context.Styles.Stylesheet;
import tools.elide.page.Context.Scripts.JavaScript;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import static java.lang.String.format;
import static tools.elide.assets.AssetBundle.DigestSettings;


/** Command-line tool for generating protobuf asset manifest files. */
@Command(
  name = "asset_bundler",
  mixinStandardHelpOptions = true,
  version = "asset_bundler v" + AssetBundler.version,
  footer = "Copyright (c) 2022, The Elide Framework Authors",
  addMethodSubcommands = false,
  description = (
    "\nGenerate a Protobuf asset manifest for use with an Elide application. This manifest may then be " +
    "interpreted by runtime-side code to automatically manage and load application assets.\n"))
@SuppressWarnings({"unused", "UnstableApiUsage", "SameParameterValue", "DuplicatedCode"})
public class AssetBundler implements Callable<Integer> {
  /** Current version for the tool. */
  static final int version = 1;

  /** URL prefix value for dynamic asset serving. */
  static final String dynamicAssetPrefix = "/_/asset";

  /** Local indicator of availability for Brotli. */
  static final boolean brotliAvailable;

  /** Default number of characters to take from a hash. */
  private static final int DEFAULT_HASH_LENGTH = 8;

  /** Default number of rounds to run the digest. */
  private static final int DEFAULT_HASH_ROUNDS = 1;

  /** Whether to include variants which are in-efficient (i.e. larger than the content itself). */
  private static final boolean ELIDE_INEFFICIENT_VARIANTS = true;

  /** Default format to use when writing the manifest. */
  private static final ManifestFormat DEFAULT_FORMAT = ManifestFormat.TEXT;

  /** Default algorithm to use when digesting chunks. */
  private static final DigestAlgorithm DEFAULT_ALGORITHM = DigestAlgorithm.SHA256;

  /** Private log pipe, addressed to this class. */
  private static final Logger logger = LoggerFactory.getLogger(AssetBundler.class);

  /** Whether to load Brotli libraries and use them for compression. */
  private static final boolean brotliEnabled = false;

  /** Executor to use for async calls. */
  private static final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
    Executors.newFixedThreadPool(3));

  static {
    boolean isBrotliAvailable = false;
    if (brotliEnabled) {
        try {
            isBrotliAvailable = (
                BrotliLoader.isBrotliAvailable()
            );
        } catch (RuntimeException rxe) {
            // brotli is not available
            isBrotliAvailable = false;
        }
    }
    brotliAvailable = isBrotliAvailable;
  }

  // -- Generic Options -- //

  /** Output file to write to, or `-` for standard out. */
  @Option(names = {"-o", "--output"},
          required = true,
          description = "Output target (file) for the manifest. Pass the token `-` to emit to standard out.")
  private @Nullable String output;

  /** Digest algorithm to use for chunks. */
  @Option(names = {"-d", "--digest"},
          description = "Hash algo to use for chunk naming. Options: ${COMPLETION-CANDIDATES}. " +
                        "Default value: ${DEFAULT-VALUE}.")
  private @Nonnull DigestAlgorithm digest = DEFAULT_ALGORITHM;

  /** Format to write the manifest in. */
  @Option(names = {"-f", "--format"},
          description = "Format to write the manifest in. Options: ${COMPLETION-CANDIDATES}. " +
                        "Default value: ${DEFAULT-VALUE}.")
  private @Nonnull ManifestFormat format = DEFAULT_FORMAT;

  /** Number of characters to use in each chunk hash. */
  @Option(names = {"--digest-length"},
          defaultValue = "" + DEFAULT_HASH_LENGTH,
          description = "Number of characters to take from each chunk hash. Default value: ${DEFAULT-VALUE}.")
  private @Nonnull Integer digestLength = DEFAULT_HASH_LENGTH;

  /** Number of rounds to employ when calculating digests. */
  @Option(names = {"--digest-rounds"},
          defaultValue = "" + DEFAULT_HASH_ROUNDS,
          description = "Number of rounds to employ when calculating digests. Default value: ${DEFAULT-VALUE}.")
  private @Nonnull Integer digestRounds = DEFAULT_HASH_ROUNDS;

  /** Whether we should expect valid rewrite maps, and process them, or not. */
  @Option(names = {"--rewrite-maps"},
          negatable = true,
          description = "Turn on rewrite map support.")
  private @Nonnull Boolean rewriteMaps = false;

  /** Whether we are embedding assets in the bundle. */
  @Option(names = {"--embed"},
          negatable = true,
          description = "Turn on content embedding.")
  private @Nonnull Boolean embedAssets = false;

  /** Whether we should add pre-compressed variants of embedded assets. */
  @Option(names = {"--precompress"},
          negatable = true,
          description = "Turn on content pre-compression.")
  private @Nonnull Boolean enablePrecompression = false;

  /** Whether we should add pre-compressed variants of embedded assets. */
  @Option(names = {"--variants"},
          split = ",",
          description = "Pre-compressed asset variants to generate. Specify as comma-separated values. " +
                        "Options: ${COMPLETION-CANDIDATES}. Default set: ${DEFAULT-VALUE}.")
  private @Nonnull Set<VariantCompression> compressionModes = EnumSet.copyOf(Arrays.asList(
    VariantCompression.IDENTITY,
    VariantCompression.GZIP));

  /** Whether we are compiling in verbose output mode. */
  @Option(names = {"-v", "--verbose"},
          description = "Turn on verbose log output.")
  private @Nonnull Boolean verbose = false;

  /** Whether we are compiling in quiet output mode. */
  @Option(names = {"-q", "--quiet"},
          description = "Turn off non-essential log output.")
  private @Nonnull Boolean quiet = false;

  /** Whether we are compiling in production mode. */
  @Option(names = {"--opt"},
          description = "Turn on production mode.")
  private @Nonnull Boolean production = false;

  /** Whether we are compiling in debug mode. */
  @Option(names = {"--dbg"},
          description = "Turn on debug mode.")
  private @Nonnull Boolean debug = false;

  // -- Input Targets -- //

  /** CSS modules to include in the manifest. */
  @Option(names = {"--css"},
          description = "Specify a CSS module for the manifest. This is done in the format " +
                        "`\"module.path:file1.css file2.css\"`, with source maps generally being kept inline, and " +
                        "double-quoting applied when spaces are needed.")
  private @Nullable List<CssModule> cssModules;

  /** JS (script) modules to include in the manifest. */
  @Option(names = {"--js"},
          description = "Specify a JS module for the manifest. This is done in the format " +
                        "`\"module.path:file1.js file1.js.map file2.js file2.js.map\"`, with double-quoting applied " +
                        "when spaces are needed.")
  private @Nullable List<JsModule> jsModules;

  // -- Enumerations -- //

  /** Enumerates exit codes and their meanings. */
  public enum FailureCode {
    GENERIC(1, "An unknown failure occurred."),

    TIMEOUT(2, "An operation timed out.");

    /** Exit code to use when this failure is encountered. */
    private final @Nonnull Integer code;

    /** Message to emit when this failure is encountered. */
    private final @Nonnull String message;

    FailureCode(int code, @Nonnull String message) {
      this.code = code;
      this.message = message;
    }

    /** @return Exit code assigned to this failure. */
    public @Nonnull Integer getCode() {
      return code;
    }

    /** @return Error message assigned to this failure. */
    public @Nonnull String getMessage() {
      return message;
    }
  }

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes, int maxLength) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  /** Enumerates supported digest algorithms. */
  public enum DigestAlgorithm {
    MD5,
    SHA1,
    SHA256,
    SHA512,
    SHA3_224,
    SHA3_256,
    SHA3_512;

    /** @return Protocol enumerated value for this hash algorithm. */
    HashAlgorithm toEnum() {
      switch (this) {
        case MD5: return HashAlgorithm.MD5;
        case SHA1: return HashAlgorithm.SHA1;
        case SHA256: return HashAlgorithm.SHA256;
        case SHA512: return HashAlgorithm.SHA512;
      }
      throw new IllegalArgumentException(format("Unsupported algorithm: %s", this.name()));
    }

    /** @return Java name of the algorithm. */
    String algorithm() {
      switch (this) {
        case MD5: return "MD5";
        case SHA1: return "SHA-1";
        case SHA256: return "SHA-256";
        case SHA512: return "SHA-512";
      }
      throw new IllegalArgumentException(format("Unsupported algorithm: %s", this.name()));
    }
  }

  /** Enumerates supported pre-compression algorithm choices. */
  public enum VariantCompression {
    IDENTITY,
    GZIP,
    BROTLI;

    /** @return Protocol enumerated value for this hash algorithm. */
    CompressionMode toEnum() {
      switch (this) {
        case IDENTITY: return CompressionMode.IDENTITY;
        case GZIP: return CompressionMode.GZIP;
        case BROTLI: return CompressionMode.BROTLI;
      }
      throw new IllegalArgumentException(format("Unsupported compression mode: %s", this.name()));
    }
  }

  /** Enumerates supported manifest formats. */
  public enum ManifestFormat {
    TEXT,
    JSON,
    BINARY
  }

  // -- Asset Classes -- //

  /** Encapsulates shared functionality between asset module types. */
  private abstract static class BaseAssetModule {
    /** Specifies the name of this asset module. */
    protected final @Nonnull String module;

    /** Specifies the source file paths associated with this asset module. */
    protected final @Nonnull SortedSet<String> paths;

    /**
     * Initialize a regular asset module.
     *
     * @param module Module name.
     * @param paths Source file paths.
     */
    BaseAssetModule(@Nonnull String module, @Nonnull Collection<String> paths) {
      if (Objects.requireNonNull(module).isEmpty() || module.length() < 2)
        throw new IllegalArgumentException(format("Invalid asset module name: %s.", module));
      this.module = module;
      this.paths = new TreeSet<>(paths);
    }

    /** @return Module name. */
    public @Nonnull String getModule() {
      return module;
    }

    /** @return Source file paths for this module. */
    public @Nonnull SortedSet<String> getPaths() {
      return paths;
    }
  }

  /** Specifies a script module to include in the manifest. */
  @Immutable
  private static class JsModule extends BaseAssetModule {
    /** Specifies source-map files associated with the JS sources in this module. */
    private final @Nonnull SortedSet<String> sourceMaps;

    /**
     * Initialize a new JS module.
     *
     * @param module Module name.
     * @param paths Paths for sources associated with this module.
     * @param sourceMaps Paths for source maps associated with this module.
     */
    JsModule(@Nonnull String module, @Nonnull Collection<String> paths, @Nonnull Collection<String> sourceMaps) {
      super(module, paths);
      this.sourceMaps = new TreeSet<>(sourceMaps);
    }

    @Override
    public String toString() {
      return "JsModule{" +
        module +
        ", paths=" + paths +
        '}';
    }

    /** @return Sorted set of source map paths. */
    public @Nonnull SortedSet<String> getSourceMaps() {
      return sourceMaps;
    }
  }

  /** Specifies a style module to include in the manifest. */
  @Immutable
  private static class CssModule extends BaseAssetModule {
    /** Specifies rewrite-map files associated with the CSS sources in this module. */
    private final @Nonnull SortedSet<String> rewriteMaps;

    /**
     * Initialize a new CSS module.
     *
     * @param module Module name.
     * @param paths Paths for sources associated with this module.
     * @param rewriteMaps Paths for rewrite maps associated with this module.
     */
    CssModule(@Nonnull String module, @Nonnull Collection<String> paths, @Nonnull Collection<String> rewriteMaps) {
      super(module, paths);
      this.rewriteMaps = new TreeSet<>(rewriteMaps);
    }

    @Override
    public String toString() {
      return "CssModule{" +
        module +
        ", paths=" + paths +
        '}';
    }

    /** @return Sorted set of rewrite map paths. */
    public @Nonnull SortedSet<String> getRewriteMaps() {
      return rewriteMaps;
    }
  }

  /** Holds references to each source file which we must deal with in this bundler call. */
  @Immutable
  @SuppressWarnings("WeakerAccess")
  public static class BundleSources {
    /** Map of module names to their loaded source groups. */
    private final @Nonnull ConcurrentMap<String, BundleSourceGroup> sourceGroupMap;

    /** Specifies a single source entry within the bundle. */
    static class BundleSourceGroup {
      /** Asset module object reference. */
      final @Nonnull BaseAssetModule module;

      /** Files referred to by this source group. */
      final @Nonnull List<File> files;

      /** Create a bundle source entry from scratch. */
      BundleSourceGroup(@Nonnull BaseAssetModule module)
        throws InterruptedException, ExecutionException, TimeoutException {
        this.module = module;
        this.files = Futures.allAsList(module.paths.parallelStream()
          .map(AssetBundler::loadFile)
          .collect(Collectors.toList()))
          .get(30, TimeUnit.SECONDS);
      }

      /** @return Stream of each file constituent to the bundle source group, paired to the group itself. */
      Stream<Pair<BundleSourceGroup, File>> sourcesStream() {
        return files.parallelStream().map((file) -> new Pair<>(this, file));
      }
    }

    /**
     * Construct a new bundle of sources, from scratch, from a map of modules to source groups.
     *
     * @param sourceGroupMap Map of module names to their source groups.
     */
    public BundleSources(@Nonnull ConcurrentMap<String, BundleSourceGroup> sourceGroupMap) {
      this.sourceGroupMap = sourceGroupMap;
    }

    /** @return Map of source groups for this bundle routine, each assigned to their module name. */
    public @Nonnull ConcurrentMap<String, BundleSourceGroup> getSourceGroupMap() {
      return sourceGroupMap;
    }
  }

  /** Holds onto current state, throughout each builder action. */
  @SuppressWarnings("rawtypes")
  @Immutable @ThreadSafe
  static class BundlerState {
    /** Bundle builder. */
    final @Nonnull AssetBundle.Builder bundle;

    /** Manager for bundler sources. */
    final @Nonnull BundleSources sources;

    /** Registered bundle actions. */
    final @Nonnull ArrayList<Function<AssetBundle.Builder, ListenableFuture>> bundleActions;

    /** Registered file actions. */
    final @Nonnull ArrayList<Function<Pair<BaseAssetModule, InputStream>, List<ListenableFuture>>> fileActions;

    /** Constructor from scratch. */
    BundlerState(@Nonnull AssetBundle.Builder bundle, @Nonnull BundleSources sources) {
      this.bundle = bundle;
      this.sources = sources;
      this.bundleActions = new ArrayList<>();
      this.fileActions = new ArrayList<>();
    }

    /** Register an action which operates on the bundle builder. */
    @CanIgnoreReturnValue @Nonnull
    BundlerState registerBundlerAction(@Nonnull Function<AssetBundle.Builder, ListenableFuture> op) {
      this.bundleActions.add(op);
      return this;
    }

    /** Register an action which operates on each source file. */
    @CanIgnoreReturnValue @Nonnull
    BundlerState registerFileAction(@Nonnull Function<Pair<BaseAssetModule, InputStream>, List<ListenableFuture>> op) {
      this.fileActions.add(op);
      return this;
    }
  }

  /**
   * Digest the provided bytes, the provided number of times.
   *
   * @param digest Algorithm to use.
   * @param bytes Bytes to digest.
   * @param rounds Rounds to apply.
   * @return Message digester, after performing the specified number of rounds.
   */
  private static @Nonnull MessageDigest digestBytes(DigestAlgorithm digest, byte[] bytes, int rounds) {
    try {
      // setup a digester for this file
      MessageDigest mainDigester = MessageDigest.getInstance(digest.algorithm());

      int iterations = 0;
      while (iterations < rounds) {
        iterations++;
        if (iterations > 1) {
          // flip the digester to perform another round
          byte[] digestSoFar = mainDigester.digest();
          mainDigester = MessageDigest.getInstance(digest.algorithm());
          mainDigester.update(digestSoFar);
        }
        mainDigester.update(bytes);
      }
      return mainDigester;

    } catch (NoSuchAlgorithmException inner) {
      throw new RuntimeException(inner);
    }
  }

  /**
   * Validate a module argument, and return the module specifier.
   *
   * @return Module specifier.
   * @throws IllegalArgumentException If the specifier cannot be located or is invalid in some way.
   */
  private static @Nonnull String validateArg(@Nonnull String arg) {
    if (!arg.contains(":"))
      throw new IllegalArgumentException("Invalid format for module spec. No `:` found.");
    return arg
      .replace("\"", "")
      .replace("'", "")
      .substring(0, arg.indexOf(':'));
  }

  /**
   * Parse the set of source files mentioned in a module argument.
   *
   * @param arg Argument to parse source file paths from.
   * @return List of paths, one for each source file found, regardless of type.
   */
  private static @Nonnull List<String> parseFilesFromArg(@Nonnull String arg) {
    String noModule = arg.substring(arg.indexOf(':') + 1);
    if (noModule.contains(" ")) {
      return Arrays.asList(noModule.split(" "));
    }
    return Collections.singletonList(noModule);
  }

  /**
   * Inflates JS module declaration arguments into {@link JsModule} spec instances.
   *
   * @param arg Argument specifying a JS module.
   * @return JS module specification instance.
   */
  private static @Nonnull JsModule interpretJsModule(@Nonnull String arg) {
    // format (inner): `--js="`module.name.here:some/file.js some/file.js.map some/file2.js some/file2.js.map`"`
    logger.trace(format("Interpreting JS arg: `%s`", arg));
    String module = validateArg(arg);
    List<String> files = parseFilesFromArg(arg);

    List<String> sources = files
      .parallelStream()
      .filter((path) -> path.endsWith(".js")).collect(Collectors.toList());

    List<String> sourceMaps = files
      .parallelStream()
      .filter((path) -> path.endsWith(".map")).collect(Collectors.toList());
    return new JsModule(module, sources, sourceMaps);
  }

  /**
   * Inflates CSS module declaration arguments into {@link CssModule} spec instances.
   *
   * @param arg Argument specifying a CSS module.
   * @return CSS module specification instance.
   */
  private static @Nonnull CssModule interpretCssModule(@Nonnull String arg) {
    // format (inner): `--css="`module.name.here:some/file.css some/file2.css some/map.css.json`"`
    logger.trace(format("Interpreting CSS arg: `%s`", arg));
    String module = validateArg(arg);
    List<String> files = parseFilesFromArg(arg);

    List<String> sources = files
      .parallelStream()
      .filter((path) -> path.endsWith(".css")).collect(Collectors.toList());

    List<String> rewriteMaps = files
      .parallelStream()
      .filter((path) -> path.endsWith(".json")).collect(Collectors.toList());
    return new CssModule(module, sources, rewriteMaps);
  }

  /**
   * Asynchronously load a file from disk, which is mentioned as a source file in the current bundler run. This does not
   * open and read the file, it merely locates it and verifies our ability to read it when the time comes.
   *
   * @return Future which resolves to the loaded file.
   */
  private static @Nonnull ListenableFuture<File> loadFile(@Nonnull String path) {
    return executorService.submit(() -> {
      logger.trace(format("Checked asset '%s'.", path));
      File file = new File(path);
      if (!file.canRead())
        throw new IllegalStateException(format("Unable to read source file at path %s.", path));
      if (file.isDirectory())
        throw new IllegalArgumentException(format("Cannot specify directory for asset source (got: '%s').", path));
      return file;
    });
  }

  /**
   * Run the tool. Args should be in the form:
   * <pre>-- --js=module:file.js another.js --css=module:file.css another.css --rewrite=map.json</pre>
   *
   * @param args Arguments to run the tool with.
   */
  public static void main(String... args) {
    CommandLine cl = new CommandLine(new AssetBundler());
    cl.registerConverter(JsModule.class, AssetBundler::interpretJsModule);
    cl.registerConverter(CssModule.class, AssetBundler::interpretCssModule);
    System.exit(cl.execute(args));
  }

  private AssetBundler() { /* Disallow empty instantiation, except by DI or static factory. */ }

  /**
   * Create a new {@link AssetBundler} from scratch.
   *
   * @return Asset bundler.
   */
  public static AssetBundler create() {
    return new AssetBundler();
  }

  /** Private constructor (from scratch). Accessed through static factory methods. */
  @SuppressWarnings("unused")
  private AssetBundler(@Nonnull String output,
                       @Nonnull DigestAlgorithm digest,
                       @Nonnull ManifestFormat format,
                       @Nonnull Integer digestLength,
                       @Nonnull Integer digestRounds,
                       @Nonnull Boolean embedAssets,
                       @Nonnull Boolean enablePrecompression,
                       @Nonnull Boolean enableRewriteMaps,
                       @Nonnull Set<VariantCompression> variants,
                       @Nonnull List<JsModule> jsModules,
                       @Nonnull List<CssModule> cssModules,
                       @Nonnull Boolean verbose,
                       @Nonnull Boolean quiet,
                       @Nonnull Boolean debug,
                       @Nonnull Boolean production) {
    this.output = output;
    this.digest = digest;
    this.format = format;
    this.digestLength = digestLength;
    this.digestRounds = digestRounds;
    this.embedAssets = embedAssets;
    this.rewriteMaps = enableRewriteMaps;
    this.enablePrecompression = enablePrecompression;
    this.compressionModes = variants;
    this.verbose = verbose;
    this.quiet = quiet;
    this.debug = debug;
    this.production = production;
    this.cssModules = cssModules;
    this.jsModules = jsModules;
  }

  /**
   * Emit a verbose-level log message.
   *
   * @param message Message to emit.
   * @param context Context items to include for formatting.
   */
  private void verbose(@Nonnull String message, Object... context) {
    if ((this.debug || this.verbose) && logger.isDebugEnabled())
      logger.debug(format(message, context));
  }

  /**
   * Emit an info-level log message.
   *
   * @param message Message to emit.
   * @param context Context items to include for formatting.
   */
  private void info(@Nonnull String message, Object... context) {
    if (!this.quiet && logger.isInfoEnabled())
      logger.info(format(message, context));
  }

  /**
   * Emit a warning-level log message.
   *
   * @param message Message to emit.
   * @param context Context items to include for formatting.
   */
  private void warn(@Nonnull String message, Object... context) {
    logger.warn(format(message, context));
  }

  /**
   * Emit a fatal error-level log message, then exit.
   *
   * @param failure Failure type.
   * @param message Message to emit as the fatal error.
   * @param context Context items to include for formatting.
   */
  private int error(@Nonnull FailureCode failure, @Nullable String message, Object... context) {
    logger.error(failure.getMessage());
    if (message != null)
      logger.error(format(message, context));
    return failure.getCode();
  }

  /** @return Whether we are operating in debug mode. */
  public boolean isDebug() {
    return this.debug && !this.production;
  }

  /** @return Whether we are operating in production mode. */
  public boolean isProduction() {
    return this.production && !this.debug;
  }

  /** @return Active digest mode for chunk data and names. */
  public @Nonnull DigestAlgorithm getDigest() {
    return digest;
  }

  /** @return Selected format to write the manifest in. */
  public @Nonnull ManifestFormat getFormat() {
    return format;
  }

  // -- Internals -- //

  /** @return Output stream that this tool should emit to. */
  private OutputStream resolveOutputTarget() throws IOException {
    Objects.requireNonNull(this.output);

    if ("-".equals(this.output)) {
      // we are outputting to stdout
      this.verbose("Resolved output stream as STDOUT.");
      return System.out;
    } else {
      // we have a file-based output target. validate it.
      this.verbose("Resolved output stream for file at path '%s'.", this.output);
      File target = new File(this.output);
      if (target.createNewFile() && target.canWrite()) {
        return Files.newOutputStream(target.toPath());
      }
    }
    throw new IllegalArgumentException(format("Failed to load output target at path '%s'.", this.output));
  }

  /**
   * Scan the inputs specified for this bundler run. Make sure each and every source file validly exists and is readable
   * by the bundler, then package it all up in a {@link BundleSources} object. If any error occurs, complain loudly.
   *
   * @return Bundle sources to use for this run.
   */
  private @Nonnull BundleSources prepareInputs() {
    this.verbose("Validating and preparing inputs...");
    if (this.jsModules == null) this.jsModules = Collections.emptyList();
    Objects.requireNonNull(this.jsModules, "JS modules cannot be `null`");
    if (this.cssModules == null) this.cssModules = Collections.emptyList();
    Objects.requireNonNull(this.cssModules, "CSS modules cannot be `null`");

    if (this.debug) {
      verbose("Loaded asset modules:");
      this.jsModules.forEach((js) -> verbose("- %s", js));
      this.cssModules.forEach((css) -> verbose("- %s", css));
    }

    // for each asset...
    Stream<Pair<BundleSources.BundleSourceGroup, File>> assetStream = (
      Stream.concat(this.cssModules.parallelStream(), this.jsModules.parallelStream()))

      // map each bundle into a pair of <bundle, source_file>
      .flatMap((module) -> {
        try {
          return new BundleSources.BundleSourceGroup(module).sourcesStream();
        } catch (ExecutionException exec) {
          Throwable cause = exec.getCause() != null ? exec.getCause() : exec;
          throw new RuntimeException(cause);
        } catch (TimeoutException | InterruptedException interrupted) {
          error(FailureCode.TIMEOUT, null);
          throw new RuntimeException(interrupted);
        }
      });

    return new BundleSources(assetStream
      .map(Pair::getFirst)
      .collect(Collectors.toConcurrentMap(
        (bundleGroup) -> bundleGroup.module.module,
        (bundleGroup) -> bundleGroup,
        (l, r) -> {
          throw new IllegalStateException(format("Cannot duplicate module names. Found two of '%s'.", l.module.module));
        },
        ConcurrentSkipListMap::new)));
  }

  /**
   * Process referenced sources. This involves loading each one, calculating digests for the individual file, factoring
   * the file into top-level digests, and calculating compressed variants based on file content.
   *
   * @param state State object, which provides access to the bundle builder.
   * @param sources Sources associated with this bundle routine.
   */
  private void processSources(@Nonnull BundlerState state, @Nonnull BundleSources sources) throws Exception {
    // set up the main digester
    verbose("Loading sources...");
    MessageDigest mainDigester = MessageDigest.getInstance(this.digest.algorithm());

    for (Map.Entry<String, BundleSources.BundleSourceGroup> module : sources.sourceGroupMap.entrySet()) {
      BundleSources.BundleSourceGroup sourceGroup = module.getValue();
      verbose("Processing %s sources for module '%s'", sourceGroup.files.size(), sourceGroup.module.module);
      ArrayList<ListenableFuture<byte[]>> futures = new ArrayList<>();

      for (File sourceFile : sourceGroup.files) {
        futures.add(this.processFile(sourceFile, sourceGroup.module, state));
      }
      Futures.allAsList(futures).get(5, TimeUnit.MINUTES);
    }
  }

  /**
   * Process a single source file, asynchronously, in a background thread.
   *
   * @param file File to process.
   * @param module Module info for this source file.
   * @param state State object, which provides access to the bundle builder.
   * @return Async future wrapping the file-process operation.
   */
  private @Nonnull ListenableFuture<byte[]> processFile(@Nonnull File file,
                                                        @Nonnull BaseAssetModule module,
                                                        @Nonnull BundlerState state) {
    return executorService.submit(() -> {
      AssetBundle.AssetContent.Builder assetContent = AssetBundle.AssetContent.newBuilder();

      try {
        byte[] fileContents = Files.readAllBytes(file.toPath());
        verbose("Loaded %s bytes for source file '%s'", fileContents.length, file.getPath());

        // perform file content digests
        MessageDigest fileDigestMD5 = digestBytes(DigestAlgorithm.MD5, fileContents, 1);
        MessageDigest fileDigest256 = digestBytes(DigestAlgorithm.SHA256, fileContents, 1);
        MessageDigest fileDigest512 = digestBytes(DigestAlgorithm.SHA256, fileContents, 1);
        byte[] fileMD5 = fileDigestMD5.digest();
        byte[] file256 = fileDigest256.digest();
        byte[] file512 = fileDigest512.digest();
        String hexMD5 = bytesToHex(fileMD5, -1);
        verbose("Calculated content digests for file '%s' (MD5: '%s').",
          file.getPath(), hexMD5);

        // perform token digest
        String preimage = Joiner.on(":")
          .join(module.module, file.getPath(), hexMD5);
        if (logger.isTraceEnabled())
          logger.trace(format("Pre-image for file token: `%s`.", preimage));

        MessageDigest tokenDigester = digestBytes(
          this.digest, preimage.getBytes(StandardCharsets.UTF_8), this.digestRounds);

        byte[] tokenDigest = tokenDigester.digest();
        String encodedToken = bytesToHex(tokenDigest, this.digestLength).toLowerCase();

        verbose("Calculated content digests and token ('%s') for file '%s' (MD5: '%s').",
          encodedToken, file.getPath(), hexMD5);

        assetContent
          .setToken(encodedToken)
          .setModule(module.module)
          .setFilename(file.getName());

        if (this.embedAssets) {
          verbose("Embedded assets are ENABLED. Adding un-compressed asset (size: '%s')...",
            fileContents.length);

          ByteString fileData = ByteString.copyFrom(fileContents);

          assetContent
            // Variant 0: `IDENTITY` (i.e. not compressed)
            .addVariant(CompressedData.newBuilder()
              .setSize(fileContents.length)
              .setData(DataContainer.newBuilder()
                .setRaw(fileData))
              .addIntegrity(DataFingerprint.newBuilder()
                .setHash(HashAlgorithm.MD5)
                .setFingerprint(ByteString.copyFrom(fileMD5)))
              .addIntegrity(DataFingerprint.newBuilder()
                .setHash(HashAlgorithm.SHA256)
                .setFingerprint(ByteString.copyFrom(file256)))
              .addIntegrity(DataFingerprint.newBuilder()
                .setHash(HashAlgorithm.SHA512)
                .setFingerprint(ByteString.copyFrom(file512))));

          if (this.enablePrecompression) {
            // for each compression variant, add a compressed data payload
            ArrayList<ListenableFuture<Pair<VariantCompression, ByteString>>> compressionJobs = new ArrayList<>();
            for (VariantCompression compressionMode : this.compressionModes) {
              if (VariantCompression.IDENTITY.equals(compressionMode))
                continue;
              verbose("Kicking off %s compression job for '%s'.", compressionMode.name(), file.getPath());
              ListenableFuture<Pair<VariantCompression, ByteString>> job = this.compress(
                compressionMode,
                fileData,
                file.getPath()
              );
              compressionJobs.add(job);
            }

            ListenableFuture<List<Pair<VariantCompression, ByteString>>> jobs = Futures.allAsList(compressionJobs);
            jobs.get(60, TimeUnit.SECONDS);

            // map each byte string payload to a content variant
            for (Pair<VariantCompression, ByteString> jobSpec : jobs.get()) {
              ByteString jobResultData = jobSpec.getSecond();
              VariantCompression compressionMode = jobSpec.getFirst();
              if (jobResultData == null) {
                // if the job result is null, it's because the compression algorithm in question was not supported or
                // encountered a fatal IOException.
                continue;
              }
              byte[] compressedData = jobResultData.toByteArray();

              if (ELIDE_INEFFICIENT_VARIANTS && compressedData.length > fileContents.length) {
                verbose(
                  "Compressed data for asset (via algorithm '%s') was larger than the original. Skipping.",
                  compressionMode.name());
                continue;
              }

              MessageDigest variantDigestMD5 = digestBytes(DigestAlgorithm.MD5, compressedData, 1);
              MessageDigest variantDigest256 = digestBytes(DigestAlgorithm.SHA256, compressedData, 1);
              MessageDigest variantDigest512 = digestBytes(DigestAlgorithm.SHA256, compressedData, 1);
              byte[] variantMD5 = variantDigestMD5.digest();

              String variantHexMD5 = bytesToHex(fileMD5, -1);
              verbose("Calculated content digests for compressed variant '%s' (MD5: '%s').",
                compressionMode.name(), variantHexMD5);

              assetContent
                .addVariant(CompressedData.newBuilder()
                  .setSize(compressedData.length)
                  .setCompression(compressionMode.toEnum())
                  .setData(DataContainer.newBuilder()
                    .setRaw(jobResultData))
                  .addIntegrity(DataFingerprint.newBuilder()
                    .setHash(HashAlgorithm.MD5)
                    .setFingerprint(ByteString.copyFrom(variantMD5)))
                  .addIntegrity(DataFingerprint.newBuilder()
                    .setHash(HashAlgorithm.SHA256)
                    .setFingerprint(ByteString.copyFrom(variantDigest256.digest())))
                  .addIntegrity(DataFingerprint.newBuilder()
                    .setHash(HashAlgorithm.SHA512)
                    .setFingerprint(ByteString.copyFrom(variantDigest512.digest()))));
            }
          }
        } else {
          verbose("Embedded assets are DISABLED. Affixing metadata only.");
          assetContent
            .addVariant(CompressedData.newBuilder()
              .addIntegrity(DataFingerprint.newBuilder()
                .setHash(HashAlgorithm.MD5)
                .setFingerprint(ByteString.copyFrom(fileMD5)))
              .addIntegrity(DataFingerprint.newBuilder()
                .setHash(HashAlgorithm.SHA256)
                .setFingerprint(ByteString.copyFrom(file256)))
              .addIntegrity(DataFingerprint.newBuilder()
                .setHash(HashAlgorithm.SHA512)
                .setFingerprint(ByteString.copyFrom(file512))));
        }
        state.bundle.addAsset(assetContent);
        this.processAssetInfo(state, module, encodedToken);
        return tokenDigest;

      } catch (Exception inner) {
        throw new RuntimeException(inner);
      }
    });
  }

  /**
   * Compress a blob of file contents, using the specified compression mode.
   *
   * @param compressionMode Compression mode to employ for this operation.
   * @return Async operation that evaluates to the resulting compressed data.
   */
  private @Nonnull ListenableFuture<Pair<VariantCompression, ByteString>> compress(
          @Nonnull VariantCompression compressionMode,
          @Nonnull ByteString fileContents,
          @Nonnull String file) {
    return executorService.submit(() -> {
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        long start = System.currentTimeMillis();
        verbose("Compressing file '%s' with %s...", file, compressionMode.name());
        byte[] rawData = fileContents.toByteArray();
        byte[] compressed;

        switch (compressionMode) {
          case GZIP:
            try (GZIPOutputStream gzStream = new GZIPOutputStream(out)) {
              gzStream.write(rawData);
              gzStream.flush();
              gzStream.finish();
              gzStream.close();
              compressed = out.toByteArray();
            }
            break;

          case BROTLI:
            if (!brotliAvailable) {
              // return early: brotli is not available on this platform.
              info("No Brotli library available. Skipping pre-compression with Brotli.");
              return null;
            }

            // build Brotli parameters. because the asset bundler is designed for build-time use, we can safely choose a
            // high compression quality here.
            try (BrotliOutputStream brStream = new BrotliOutputStream(out, new Encoder.Parameters()
                    .setQuality(10)
                    .setWindow(24))) {
              brStream.write(rawData);
              brStream.flush();
              brStream.close();
              compressed = out.toByteArray();
            } catch (IOException ioe) {
              warn("Failed to compress with Brotli: got IOException. Proceeding anyway.");
              return null;
            }
            break;

          default:
            throw new IllegalStateException(format("Cannot compress when mode is `%s`.", compressionMode.name()));
        }

        verbose("Compressed file '%s' with %s in %sms.",
          file,
          compressionMode.name(),
          System.currentTimeMillis() - start);

        return new Pair<>(compressionMode, ByteString.copyFrom(compressed));
      }
    });
  }

  /**
   * Process structural asset information for the metadata portion of the manifest.
   *
   * @param encodedToken Encoded token for this asset.
   */
  private void processAssetInfo(@Nonnull BundlerState state,
                                @Nonnull BaseAssetModule module,
                                @Nonnull String encodedToken) {
    verbose("Processing asset metadata for module '%s'.", module.module);
    if (module instanceof JsModule) {
      // prep a JS module and attach it
      ScriptBundle.Builder bundle = ScriptBundle.newBuilder().setModule(module.module);

      // build an asset for each script target
      bundle.addAllAsset(module.paths.parallelStream().map((path) -> {
        //noinspection CodeBlock2Expr
        return ScriptBundle.ScriptAsset.newBuilder()
              .setToken(encodedToken)
              .setFilename(new File(path).getName())
              .setScript(JavaScript.newBuilder()
                .setUri(UnsafeSanitizedContentOrdainer.ordainAsSafe(
                  generateDynamicUrlForScript((JsModule)module, encodedToken),
                  SanitizedContent.ContentKind.TRUSTED_RESOURCE_URI).toTrustedResourceUrlProto()))
              .build();

      }).collect(Collectors.toList()));

      state.bundle.putScripts(module.module, bundle.build());

    } else if (module instanceof CssModule) {
      // prep a CSS module and attach it
      StyleBundle.Builder bundle = StyleBundle.newBuilder().setModule(module.module);

      // build an asset for each script target
      bundle.addAllAsset(module.paths.parallelStream().map((path) -> {
        //noinspection CodeBlock2Expr
        return StyleBundle.StyleAsset.newBuilder()
          .setToken(encodedToken)
          .setFilename(new File(path).getName())
          .setStylesheet(Stylesheet.newBuilder()
            .setUri(UnsafeSanitizedContentOrdainer.ordainAsSafe(
              generateDynamicUrlForStyles((CssModule)module, encodedToken),
              SanitizedContent.ContentKind.TRUSTED_RESOURCE_URI).toTrustedResourceUrlProto()))
          .build();

      }).collect(Collectors.toList()));

      state.bundle.putStyles(module.module, bundle.build());

    } else {
      throw new IllegalStateException(format("Unrecognized asset type: %s", module.getClass().getName()));
    }
  }

  /**
   * Generate a dynamic URI at which a given JavaScript bundle may be loaded and served.
   *
   * @param module Module which we are generating a URI for.
   * @param token Token uniquely identifying this file's data and state.
   * @return Generated serving URI for the script asset.
   */
  private @Nonnull String generateDynamicUrlForScript(@Nonnull JsModule module, @Nonnull String token) {
    String uri = format("%s/%s.js", dynamicAssetPrefix, token);
    verbose("Generated serving URI for JS asset (module: %s): '%s'.", module, uri);
    return uri;
  }

  /**
   * Generate a dynamic URI at which a given stylesheet bundle may be loaded and served.
   *
   * @param module Module which we are generating a URI for.
   * @param token Token uniquely identifying this file's data and state.
   * @return Generated serving URI for the style asset.
   */
  private @Nonnull String generateDynamicUrlForStyles(@Nonnull CssModule module, @Nonnull String token) {
    String uri = format("%s/%s.css", dynamicAssetPrefix, token);
    verbose("Generated serving URI for CSS asset (module: %s): '%s'.", module, uri);
    return uri;
  }

  /**
   * Run the asset bundler. By this point, it is expected that all options and sources will have been mounted on the
   * bundler, via the constructor/factory methods or setters.
   *
   * <p>During the bundle routine, the bundler will validate that each source file properly exists, then it will load it
   * to calculate a digest token. It will also associate the raw content in the file with pre-compressed versions of the
   * content, which the server can use later on down the line.</p>
   *
   * <p>With regard to output, either a file will be written according to {@link #output} (which is a path that must be
   * valid and writable), or the special token {@code -} may be passed to output to standard out.</p>
   *
   * @param sources Initialized and pre-validated source file inputs for this run.
   * @return Exit code the tool should finish with. If non-zero, an error occurred.
   * @throws FileNotFoundException If some file, an input or the output target, could not be found.
   * @throws IOException If some IO failure occurs while writing the output, or reading inputs.
   */
  @CanIgnoreReturnValue
  @SuppressWarnings("WeakerAccess")
  public int bundle(@Nonnull BundleSources sources) throws Exception {
    long start = System.currentTimeMillis();
    Objects.requireNonNull(this.jsModules, "JS modules cannot be `null`");
    Objects.requireNonNull(this.cssModules, "CSS modules cannot be `null`");
    Objects.requireNonNull(this.digest, "Cannot run with `null` digest algorithm.");
    Objects.requireNonNull(this.digestLength, "Cannot run with `null` digest length.");

    // resolve our output target.
    try (OutputStream target = resolveOutputTarget()) {
      try (BufferedOutputStream outputBuffer = new BufferedOutputStream(target)) {
        // prep the bundle builder
        final AssetBundle.Builder builder = AssetBundle.newBuilder()
          .setVersion(version)
          .setStyleRewrite(rewriteMaps)
          .setGenerated(Timestamp.newBuilder()
            .setSeconds(System.currentTimeMillis() / 1000))
          .setDigestSettings(DigestSettings.newBuilder()
            .setAlgorithm(this.digest.toEnum())
            .setTail(this.digestLength)
            .setRounds(this.digestRounds));

        final BundlerState state = new BundlerState(builder, sources);

        info("Running asset bundler (%s JS groups, %s CSS groups)...",
          this.jsModules.size(),
          this.cssModules.size());

        // run the tool
        this.processSources(state, sources);
        final AssetBundle bundle = builder.build();

        switch (this.format) {
            case BINARY:
              verbose("Writing BINARY data to manifest target...");
              bundle.writeDelimitedTo(outputBuffer);
                break;
            case TEXT:
              verbose("Writing TEXT data to manifest target...");
              outputBuffer.write(bundle.toString().getBytes(StandardCharsets.UTF_8));
                break;
            case JSON:
              verbose("Writing JSON data to manifest target...");
              outputBuffer.write(JsonFormat.printer()
                  .omittingInsignificantWhitespace()
                  .sortingMapKeys()
                  .print(bundle)
                  .getBytes(StandardCharsets.UTF_8));
              break;
        }
        if (this.debug) {
          logger.trace(format("Final structure of manifest:\n%s\n", bundle));
        }
        info("Asset bundler run completed in %sms.", System.currentTimeMillis() - start);
      }
    }
    return 0;
  }

  /** Run the tool. */
  @Override
  public Integer call() throws Exception {
    // figure out if we are running in `opt` or `dbg`
    if (isDebug())
      this.verbose("Asset bundler starting up (mode: DEBUG, digest: %s, format: %s)...",
        this.digest.name(), this.format.name());
    else if (isProduction())
      this.verbose("Asset bundler starting up (mode: PRODUCTION, digest: %s, format: %s)...",
        this.digest.name(), this.format.name());
    else
      return this.error(FailureCode.GENERIC,
        "Failed to resolve asset manifest mode. Please pass one of `--dbg` or `--opt`.");
    return this.bundle(this.prepareInputs());
  }
}
