#!/bin/bash
set -e;set +x;TOOL_REVISION="1.0.0-alpha7";INSTALLER_VERSION="v0.11";TOOL="cli";VERSION="v1";RELEASE="snapshot";
COMPRESSION="tgz";BINARY="elide";DOWNLOAD_BASE="https://elide.zip";DEFAULT_INSTALL_DIR="$HOME/elide";
ENABLE_DEBUG="false";ENABLE_COLOR="true";INSTALL_INTO_PATH="true";
if [[ "$@" == *"no-color"* ]]; then MAGENTA="";CYAN="";RED="";YELLOW="";GRAY="";BOLD="";NC="";else MAGENTA="\033[0;35m";
CYAN="\033[0;36m";RED="\033[0;31m";YELLOW="\033[0;33m";GRAY="\033[0;37m";BOLD="\033[1m";NC="\033[0m"; fi
function say {
echo -e "[${MAGENTA}elide${NC}]  $1";
}
function warn {
echo -e "[${YELLOW}elide${NC}]  $1";
}
function error {
echo -e "[${RED}elide${NC}]  Error: $2";
exit "$1";
}
function debug {
if [ "$ENABLE_DEBUG" = true ]; then
    DATESTRING=$(date +"%Y-%m-%d %H:%M:%S");
    if [ "$ENABLE_COLOR" = true ]; then
        echo -e "${MAGENTA}[elide]${NC}  ${GRAY}$DATESTRING${NC}  $1";
    else
        echo -e "[elide]  $DATESTRING  $1";
    fi
fi
}
if [[ "$@" == *"help"* ]]; then
echo -e "";
echo -e "${BOLD}${MAGENTA}Elide: ${CYAN}CLI installer${NC} $INSTALLER_VERSION";
echo -e "https://github.com/elide-dev";
echo -e "";
echo -e "Usage:";
echo -e "  ./install.sh";
echo -e "  ./install.sh | bash [options]";
echo -e "  curl https://elide.sh | bash";
echo -e "  curl https://dl.elide.dev/cli/v1/snapshot/install.sh | bash [options]";
echo -e "";
echo -e "Options:";
echo -e "  ${YELLOW}--install-dir${NC}=<path>     Install to a custom directory";
echo -e "  ${YELLOW}--install-rev${NC}=<version>  Install a specific version of Elide";
echo -e "  ${YELLOW}--${NC}[${YELLOW}no${NC}]${YELLOW}-path${NC}              Whether to add the install directory to the PATH";
echo -e "  ${YELLOW}--no-banner${NC}              Opt out of the announcement banner after install";
echo -e "  ${YELLOW}--no-color${NC}               Disable color output";
echo -e "  ${YELLOW}--debug${NC}                  Enable debug output";
echo -e "  ${YELLOW}--trace${NC}                  Enable bash tracing";
echo -e "  ${YELLOW}--version${NC}                Show version information";
echo -e "  ${YELLOW}--help${NC}                   Show this help message";
echo -e "";
echo -e "Copyright 2023, Sam Gammon and the Elide Project Authors.";
exit 0;
fi
if [[ "$@" == *"version"* ]]; then
echo "Elide Installer:";
echo "$VERSION";
fi
if [[ "$@" == *"debug"* ]]; then
debug "Elide installer: Debugging is enabled.";
ENABLE_DEBUG=true;
fi
if [[ "$@" == *"no-path"* ]]; then
debug "User disabled path installation.";
INSTALL_INTO_PATH="false";
fi
SHOW_BANNER="true";
if [[ "$@" == *"no-banner"* ]]; then
debug "Opting out of install banner.";
SHOW_BANNER="false";
fi
if [[ "$@" == *"trace"* ]]; then
debug "Trace mode is active.";
set -x;
fi
if [[ "$@" == *"arch"* ]]; then
ARCH=$(echo "$@" | grep -o -E "arch=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2);
debug "User specified a specific arch: $ARCH";
fi
if [[ "$@" == *"rev"* ]]; then
TOOL_REVISION=$(echo "$@" | grep -o -E "rev=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2);
debug "User specified a specific revision: $TOOL_REVISION";
fi
if [[ "$@" == *"os"* ]]; then
OS=$(echo "$@" | grep -o -E "os=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2);
debug "User specified a specific OS: $OS";
fi
COMPRESSION_TOOL="gzip";
if [ -x "$(command -v bzip2)" ]; then
debug "Found compression: bzip2";
COMPRESSION_TOOL="bzip2";
COMPRESSION="bz2";
fi
if [ -x "$(command -v zstd)" ]; then
debug "Found compression: zstd";
COMPRESSION_TOOL="zstd";
COMPRESSION="zst";
fi
if [ -x "$(command -v xz)" ]; then
debug "Found compression: xz";
COMPRESSION_TOOL="xz";
COMPRESSION="txz";
fi
debug "Using compression tool: $COMPRESSION_TOOL (extension $COMPRESSION)";
PARAM_INSTALL_DIR=$(echo "$@" | grep -o -E "install-dir=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2 || $INSTALL_DIR);
INSTALL_DIR=${PARAM_INSTALL_DIR:-$DEFAULT_INSTALL_DIR};
debug "Resolved install dir: $INSTALL_DIR";
if [ -d "$HOME/bin" ]; then
debug "Found $HOME/bin, will symlink elide into it.";
INSTALL_SYMLINK_DIR="$HOME/bin";
else
INSTALL_SYMLINK_DIR="";
fi
ARCH=$(uname -m);
debug "Resolved architecture: $ARCH";
if [ "$ARCH" = "x86_64" ]; then
ARCH="amd64"
fi
if [ "$ARCH" = "arm64" ]; then
ARCH="aarch64"
fi
OS=$(uname -s);
debug "Resolved OS: $ARCH";
if [ "$OS" = "Linux" ]; then
OS="linux"
fi
if [ "$OS" = "Darwin" ]; then
OS="darwin"
fi
if [ -f ~/.elide/.host_id ]; then
HOST_ID=$(cat ~/.elide/.host_id);
debug "Host fingerprint loaded: $HOST_ID";
else
mkdir -p ~/.elide;
if [ -x "$(command -v uuidgen)" ]; then
HOST_ID=$(uuidgen);
debug "Issued host fingerprint: $HOST_ID";
echo "$HOST_ID" >> ~/.elide/.host_id;
fi
fi
VARIANT="$OS-$ARCH";
DOWNLOAD_ENDPOINT="$DOWNLOAD_BASE/$TOOL/$VERSION/$RELEASE/$VARIANT/$TOOL_REVISION/$BINARY.$COMPRESSION";
debug "Download endpoint: $DOWNLOAD_ENDPOINT";
say "Installing Elide (variant: $VARIANT, version: $TOOL_REVISION)...";
DEBUG_FLAGS="-vv";
CURL_ARGS="--no-buffer --progress-bar --location --fail --tlsv1.2 --retry 3 --retry-delay 2 --http2";
debug "Downloading binary with command: curl $CURL_ARGS";
DECOMPRESS_ARGS="-xz";
if [ "$ENABLE_DEBUG" = true ]; then
CURL_ARGS="$CURL_ARGS $DEBUG_FLAGS";
DECOMPRESS_ARGS="-xzv";
set -x;
fi
debug "Decompressing with command: $COMPRESSION_TOOL $DECOMPRESS_ARGS";
mkdir -p "$INSTALL_DIR" && curl $CURL_ARGS -H "User-Agent: elide-installer/$INSTALLER_VERSION" -H "Elide-Host-ID: $HOST_ID" $DOWNLOAD_ENDPOINT | tar $DECOMPRESS_ARGS -C "$INSTALL_DIR" -f - && chmod +x "$INSTALL_DIR/$BINARY";
set +x;
if [ "$INSTALL_SYMLINK_DIR" != "" ]; then
debug "Symlinking elide into $INSTALL_SYMLINK_DIR";
if [ -f "$INSTALL_SYMLINK_DIR/$BINARY" ]; then
warn "Found existing $INSTALL_SYMLINK_DIR/$BINARY, renaming to $INSTALL_SYMLINK_DIR/$BINARY.old";
if [ -f "$INSTALL_SYMLINK_DIR/$BINARY.old" ]; then
rm -f "$INSTALL_SYMLINK_DIR/$BINARY.old";
fi
mv "$INSTALL_SYMLINK_DIR/$BINARY" "$INSTALL_SYMLINK_DIR/$BINARY.old";
fi
ln -s "$INSTALL_DIR/$BINARY" "$INSTALL_SYMLINK_DIR/$BINARY";
fi
set +x;
if [ -x "$INSTALL_DIR/$BINARY" ]; then
debug "Binary installed successfully.";
if [ "$SHOW_BANNER" = true ]; then
"$INSTALL_DIR/$BINARY" --help;
echo "";
fi
else
debug "Binary failed to install Path \"$INSTALL_DIR/$BINARY\" does not exist or is not executable.";
exit 1;
fi
echo "";
echo -e "Elide installed successfully! 🎉";
IS_ON_PATH="false";
if [ -x "$(command -v $BINARY)" ]; then
IS_ON_PATH="true";
else
if [ "$INSTALL_INTO_PATH" == true ]; then
DID_INSTALL="false";
if [ -f ~/.profile ]; then
DID_INSTALL="true";
IS_ON_PATH="true";
echo "" >> ~/.profile;
echo "# Elide PATH export" >> ~/.profile;
echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >> ~/.profile;
fi
if [ "$DID_INSTALL" != true ]; then
if [ -f ~/.zshrc ]; then
DID_INSTALL="true";
IS_ON_PATH="true";
echo "" >> ~/.zshrc;
echo "# Elide PATH export" >> ~/.zshrc;
echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >> ~/.zshrc;
fi
fi
if [ "$DID_INSTALL" != true ]; then
if [ -f ~/.bashrc ]; then
DID_INSTALL="true";
IS_ON_PATH="true";
echo "" >> ~/.bashrc;
echo "# Elide PATH export" >> ~/.bashrc;
echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >> ~/.bashrc;
fi
fi
fi
if [ "$IS_ON_PATH" != true ]; then
echo -e "";
echo -e "Note: Elide is not available on your PATH.";
echo -e "Add the following to your shell profile to add it to your PATH:";
echo -e "";
echo -e "  export PATH=\"\$PATH:$INSTALL_DIR\"";
echo -e "";
fi
fi
echo -e "";
echo -e " Get started with:";
echo -e "  $ $BINARY shell";
echo -e "";
