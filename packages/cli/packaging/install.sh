#!/bin/bash
# shellcheck disable=SC2034
# shellcheck disable=SC2129
# shellcheck disable=SC2199
set -e
set +x
TOOL_REVISION="1.0.0-beta9"
INSTALLER_VERSION="v0.19"
TOOL="cli"
VERSION="v1"
RELEASE="snapshot"
COMPRESSION="tgz"
BINARY="elide"
DOWNLOAD_BASE="https://elide.zip"
DEFAULT_INSTALL_DIR="$HOME/elide"
ENABLE_DEBUG="false"
ENABLE_COLOR="true"
INSTALL_INTO_PATH="true"
if [[ "$@" == *"no-color"* ]]; then
  MAGENTA=""
  CYAN=""
  RED=""
  YELLOW=""
  GRAY=""
  BOLD=""
  NC=""
else
  MAGENTA="\033[0;35m"
  CYAN="\033[0;36m"
  RED="\033[0;31m"
  YELLOW="\033[0;33m"
  GRAY="\033[0;37m"
  BOLD="\033[1m"
  NC="\033[0m"
fi
function say {
  echo -e "[${MAGENTA}elide${NC}]  $1"
}
function warn {
  echo -e "[${YELLOW}elide${NC}]  $1"
}
function error {
  echo -e "[${RED}elide${NC}]  Error: $2"
  exit "$1"
}
function debug {
  if [ "$ENABLE_DEBUG" = true ]; then
    DATESTRING=$(date +"%Y-%m-%d %H:%M:%S")
    if [ "$ENABLE_COLOR" = true ]; then
      echo -e "${MAGENTA}[elide]${NC}  ${GRAY}$DATESTRING${NC}  $1"
    else
      echo -e "[elide]  $DATESTRING  $1"
    fi
  fi
}
if [[ "$@" == *"help"* ]]; then
  echo -e ""
  echo -e "${BOLD}${MAGENTA}Elide: ${CYAN}CLI installer${NC} $INSTALLER_VERSION"
  echo -e "https://github.com/elide-dev"
  echo -e ""
  echo -e "Usage:"
  echo -e "  ./install.sh"
  echo -e "  ./install.sh | bash [options]"
  echo -e "  curl https://elide.sh | bash"
  echo -e "  curl https://dl.elide.dev/cli/v1/snapshot/install.sh | bash [options]"
  echo -e ""
  echo -e "Options:"
  echo -e "  ${YELLOW}--installer-format${NC}=<format>  The archive format to download (default: auto, options: tgz, txz, zip)"
  echo -e "  ${YELLOW}--preserve-installer${NC}         Don't delete the installer archive after installation."
  echo -e "  ${YELLOW}--install-digest${NC}=<digest>    The expected digest of the installer archive"
  echo -e "  ${YELLOW}--install-dir${NC}=<path>     Install to a custom directory"
  echo -e "  ${YELLOW}--install-rev${NC}=<version>  Install a specific version of Elide"
  echo -e "  ${YELLOW}--${NC}[${YELLOW}no${NC}]${YELLOW}-path${NC}              Whether to add the install directory to the PATH"
  echo -e "  ${YELLOW}--no-banner${NC}              Opt out of the announcement banner after install"
  echo -e "  ${YELLOW}--gha${NC}                    Prefer to serve the download directly from GitHub"
  echo -e "  ${YELLOW}--no-color${NC}               Disable color output"
  echo -e "  ${YELLOW}--debug${NC}                  Enable debug output"
  echo -e "  ${YELLOW}--trace${NC}                  Enable bash tracing"
  echo -e "  ${YELLOW}--version${NC}                Show version information"
  echo -e "  ${YELLOW}--help${NC}                   Show this help message"
  echo -e ""
  echo -e "Copyright 2025, Elide Technologies, Inc."
  exit 0
fi
if [[ "$@" == *"version"* ]]; then
  echo "Elide Installer:"
  echo "$VERSION"
fi
if [[ "$@" == *"debug"* ]]; then
  debug "Elide installer: Debugging is enabled."
  ENABLE_DEBUG=true
fi
if [[ "$@" == *"no-path"* ]]; then
  debug "User disabled path installation."
  INSTALL_INTO_PATH="false"
fi
SHOW_BANNER="true"
if [[ "$@" == *"no-banner"* ]]; then
  debug "Opting out of install banner."
  SHOW_BANNER="false"
fi
if [[ "$@" == *"gha"* ]]; then
  debug "Preferring GHA."
  DOWNLOAD_BASE="https://gha.elide.zip"
fi
if [[ "$@" == *"trace"* ]]; then
  debug "Trace mode is active."
  set -x
fi
if [[ "$@" == *"arch"* ]]; then
  ARCH=$(echo "$@" | grep -o -E "arch=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2)
  debug "User specified a specific arch: $ARCH"
fi
if [[ "$@" == *"rev"* ]]; then
  TOOL_REVISION=$(echo "$@" | grep -o -E "rev=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2)
  debug "User specified a specific revision: $TOOL_REVISION"
fi
if [[ "$@" == *"os"* ]]; then
  OS=$(echo "$@" | grep -o -E "os=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2)
  debug "User specified a specific OS: $OS"
fi
COMPRESSION_TOOL="gzip"
COMPRESSION="tgz"
if [[ "$@" == *"installer-format"* ]]; then
  COMPRESSION=$(echo "$@" | grep -o -E "installer-format=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2)
  debug "User specified a specific installer format: $COMPRESSION"
  if [ "$COMPRESSION" = "tgz" ]; then
    COMPRESSION_TOOL="gzip"
  elif [ "$COMPRESSION" = "zip" ]; then
    COMPRESSION_TOOL="unzip"
    COMPRESSION="zip"
  elif [ "$COMPRESSION" = "txz" ]; then
    COMPRESSION_TOOL="xz"
    COMPRESSION="txz"
  else
    error 1 "Unsupported installer format: $COMPRESSION. Supported formats are: tgz, txz, zip."
  fi
else
  debug "No installer format specified, using default with detection: $COMPRESSION_TOOL"
  if [ -x "$(command -v xz)" ]; then
    debug "Found compression: xz"
    COMPRESSION_TOOL="xz"
    COMPRESSION="txz"
  fi
fi
debug "Using compression tool: $COMPRESSION_TOOL (extension $COMPRESSION)"
INSTALLER_DIGEST=""
if [[ "$@" == *"install-digest"* ]]; then
  INSTALLER_DIGEST=$(echo "$@" | grep -o -E "install-digest=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2)
  debug "User specified an installer digest: $INSTALLER_DIGEST"
fi
PRESERVE_INSTALLER=false
if [[ "$@" == *"preserve-installer"* ]]; then
  debug "User requested to preserve the installer archive."
  PRESERVE_INSTALLER=true
fi
PARAM_INSTALL_DIR=$(echo "$@" | grep -o -E "install-dir=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2 || $INSTALL_DIR)
INSTALL_DIR=${PARAM_INSTALL_DIR:-$DEFAULT_INSTALL_DIR}
debug "Resolved install dir: $INSTALL_DIR"
if [ -d "$HOME/bin" ]; then
  debug "Found $HOME/bin, will symlink elide into it."
  INSTALL_SYMLINK_DIR="$HOME/bin"
else
  INSTALL_SYMLINK_DIR=""
fi
ARCH=$(uname -m)
debug "Resolved architecture: $ARCH"
if [ "$ARCH" = "x86_64" ]; then
  ARCH="amd64"
fi
if [ "$ARCH" = "arm64" ]; then
  ARCH="aarch64"
fi
OS=$(uname -s)
debug "Resolved OS: $ARCH"
if [ "$OS" = "Linux" ]; then
  OS="linux"
fi
if [ "$OS" = "Darwin" ]; then
  OS="darwin"
fi
if [ -f ~/.elide/.host_id ]; then
  HOST_ID=$(cat ~/.elide/.host_id)
  debug "Host fingerprint loaded: $HOST_ID"
else
  mkdir -p ~/.elide
  if [ -x "$(command -v uuidgen)" ]; then
    HOST_ID=$(uuidgen)
    debug "Issued host fingerprint: $HOST_ID"
    echo "$HOST_ID" >> ~/.elide/.host_id
  fi
fi
VARIANT="$OS-$ARCH"
DOWNLOAD_ENDPOINT="$DOWNLOAD_BASE/$TOOL/$VERSION/$RELEASE/$VARIANT/$TOOL_REVISION/$BINARY.$COMPRESSION"
debug "Download endpoint: $DOWNLOAD_ENDPOINT"
say "Installing Elide (variant: $VARIANT, version: $TOOL_REVISION)..."
DEBUG_FLAGS="-vv"
CURL_ARGS="--no-buffer --progress-bar --location --fail --tlsv1.2 --retry 3 --retry-delay 2 --http2"
debug "Downloading binary with command: curl $CURL_ARGS"
DECOMPRESS_ARGS="-d"
UNTAR_ARGS="-x"
if [ "$ENABLE_DEBUG" = true ]; then
  CURL_ARGS="$CURL_ARGS $DEBUG_FLAGS"
  UNTAR_ARGS="-xv"
  DECOMPRESS_ARGS="-d -v"
  set -x
fi
debug "Decompressing with command: $COMPRESSION_TOOL $DECOMPRESS_ARGS"
mkdir -p "$INSTALL_DIR" && curl $CURL_ARGS -H "User-Agent: elide-installer/$INSTALLER_VERSION" -H "Elide-Host-ID: $HOST_ID" $DOWNLOAD_ENDPOINT | tee elide.$COMPRESSION | $COMPRESSION_TOOL $DECOMPRESS_ARGS | tar $UNTAR_ARGS -C "$INSTALL_DIR" --strip-components=1 -f - && chmod +x "$INSTALL_DIR/$BINARY"
if [ "$INSTALLER_DIGEST" != "" ]; then
  debug "Verifying installer digest: $INSTALLER_DIGEST"
  ACTUAL_DIGEST=$(sha256sum elide.$COMPRESSION | cut -d' ' -f1)
  if [ "$ACTUAL_DIGEST" != "$INSTALLER_DIGEST" ]; then
    error 1 "Installer digest mismatch! Expected: $INSTALLER_DIGEST, got: $ACTUAL_DIGEST"
  else
    debug "Installer digest verified successfully."
    say "Digest OK: $ACTUAL_DIGEST"
    echo "$INSTALLER_DIGEST elide.$COMPRESSION" > elide.$COMPRESSION.sha256
  fi
else
  debug "No installer digest provided, skipping verification."
fi
if [ "$PRESERVE_INSTALLER" = false ]; then
  debug "Deleting installer archive."
  rm -f elide.$COMPRESSION elide.$COMPRESSION.sha256
else
  debug "Preserving installer archive at elide.$COMPRESSION"
fi
set +x
if [ "$INSTALL_SYMLINK_DIR" != "" ]; then
  debug "Symlinking elide into $INSTALL_SYMLINK_DIR"
  if [ -f "$INSTALL_SYMLINK_DIR/$BINARY" ]; then
    warn "Found existing $INSTALL_SYMLINK_DIR/$BINARY, renaming to $INSTALL_SYMLINK_DIR/$BINARY.old"
    if [ -f "$INSTALL_SYMLINK_DIR/$BINARY.old" ]; then
      rm -f "$INSTALL_SYMLINK_DIR/$BINARY.old"
    fi
    mv "$INSTALL_SYMLINK_DIR/$BINARY" "$INSTALL_SYMLINK_DIR/$BINARY.old"
  fi
  ln -s "$INSTALL_DIR/$BINARY" "$INSTALL_SYMLINK_DIR/$BINARY"
fi
set +x
if [ -x "$INSTALL_DIR/$BINARY" ]; then
  debug "Binary installed successfully."
  if [ "$SHOW_BANNER" = true ]; then
    "$INSTALL_DIR/$BINARY" --help
    echo ""
  fi
  debug "Finished running Elide help."
else
  debug "Binary failed to install Path \"$INSTALL_DIR/$BINARY\" does not exist or is not executable."
  exit 1
fi
echo ""
echo -e "Elide installed successfully! 🎉"
echo ""
echo "Installation location: $INSTALL_DIR/elide"

IS_ON_PATH="false"
INSTALLED_INTO=""
debug "Starting path logic."
if [ -x "$(command -v $BINARY)" ]; then
  echo ""
  echo "Elide is already present on your PATH, so no further action is necessary."
  IS_ON_PATH="true"
else
  if [ "$INSTALL_INTO_PATH" == true ]; then
    DID_INSTALL="false"
    if [ -f ~/.zshrc ]; then
      DID_INSTALL="true"
      IS_ON_PATH="true"
      # shellcheck disable=SC2088
      INSTALLED_INTO="~/.zshrc"
      debug "Found .zshrc; adding PATH installation for Elide."
      echo "" >> ~/.zshrc
      echo "# Elide PATH export" >> ~/.zshrc
      echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >> ~/.zshrc
    else
      debug "No .zshrc found."
    fi
    if [ "$DID_INSTALL" != true ]; then
      if [ -f ~/.bashrc ]; then
        DID_INSTALL="true"
        IS_ON_PATH="true"
        # shellcheck disable=SC2088
        INSTALLED_INTO="~/.bashrc"
        debug "Found .bashrc; adding PATH installation for Elide."
        echo "" >> ~/.bashrc
        echo "# Elide PATH export" >> ~/.bashrc
        echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >> ~/.bashrc
      else
        debug "No .bashrc found."
      fi
    fi
    if [ "$DID_INSTALL" != true ]; then
      if [ -f ~/.profile ]; then
        DID_INSTALL="true"
        IS_ON_PATH="true"
        # shellcheck disable=SC2088
        INSTALLED_INTO="~/.profile"
        debug "Found .profile; adding PATH installation for Elide."
        echo "" >> ~/.profile
        echo "# Elide PATH export" >> ~/.profile
        echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >> ~/.profile
      else
        debug "No .profile found."
      fi
    fi
  else
    debug "Skipping path installation."
  fi
  if [ "$DID_INSTALL" == true ]; then
    echo -e ""
    echo -e "Elide has been added to your PATH."
    echo -e "Run the following to update your current shell:"
    echo -e ""
    echo -e "  source $INSTALLED_INTO"
  else
    if [ "$IS_ON_PATH" != true ]; then
      echo -e ""
      echo -e "Note: Elide is not available on your PATH."
      echo -e "Add the following to your shell profile to add it to your PATH:"
      echo -e ""
      echo -e "  export PATH=\"\$PATH:$INSTALL_DIR\""
    fi
  fi
fi
echo -e ""
echo -e " Get started with:"
echo -e "  $ $BINARY"
echo -e ""
