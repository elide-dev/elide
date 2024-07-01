package dev.elide.cli.bridge

public object NativeSubstrate {
  /**
   *
   */
  public external fun enabledLibraries(): Array<String>

  /**
   *
   */
  public external fun embeddedLibraryVersion(lib: String): String
}

// dev.elide.cli.bridge.NativeSubstrate.enabledLibraries
