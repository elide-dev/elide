#!/bin/bash

# shellcheck disable=SC2034
# shellcheck disable=SC2129

# Elide Installer
# ---------------
# Version: 0.10
# Author: Sam Gammon
#
# This script can be used as a one-liner to install the Elide command-line interface. Various arguments can be passed to
# the script to help with debugging or to customize the installation process.
#
# An exhaustive list of script arguments are listed below. Please make sure to amend the changelog with any changes made
# to this script.
#
# Options:
#   --install-dir=<path>         Install to a custom directory
#   --install-rev=<version>      Install a specific version of Elide
#   --arch=<arch>                Install for a specific architecture (optional, overrides detection)
#   --os=<os>                    Install for a specific operating system (optional, overrides detection)
#   --[no]-path                  Whether to add the install directory to the PATH
#   --no-banner                  Opt out of the announcement banner after install
#   --no-color                   Disable color output
#   --debug                      Enable debug output
#   --trace                      Enable bash tracing
#   --version                    Show version information
#   --help                       Show the installer tool's help message
#
# Changelog:
#   0.10 2023-08-01  Sam Gammon  Version bump for launch
#   0.9  2023-08-01  Sam Gammon  Version bump for release
#   0.8  2022-12-28  Sam Gammon  Add --no-banner flag to skip banner
#   0.7  2022-12-28  Sam Gammon  Fix tool revision message with custom version
#   0.6  2022-12-28  Sam Gammon  Add latest version message to install script
#   0.5  2022-12-28  Sam Gammon  Updated to new default version format
#   0.4  2022-12-25  Sam Gammon  Swapped brotli for zstd.
#   0.3  2022-12-22  Sam Gammon  Fixes for pipe/buffer issues with gzip decompression, add bzip2 as archive option.
#   0.2  2022-12-22  Sam Gammon  Added support for gzip and brotli archive download when XZ is not available.
#   0.1  2022-12-21  Sam Gammon  Initial release.

set -e;
set +x;

TOOL_REVISION="1.0.0-alpha7";
INSTALLER_VERSION="v0.10";

TOOL="cli";
VERSION="v1";
RELEASE="snapshot";
COMPRESSION="gz";
BINARY="elide";
DOWNLOAD_BASE="https://dl.elide.dev";
DEFAULT_INSTALL_DIR="$HOME/bin";

ENABLE_DEBUG="false";
ENABLE_COLOR="true";
INSTALL_INTO_PATH="true";

if [[ "$@" == *"no-color"* ]]; then
    MAGENTA="";
    CYAN="";
    BLUE="";
    RED="";
    YELLOW="";
    GRAY="";
    BOLD="";
    NC="";
else
    MAGENTA="\033[0;35m";
    CYAN="\033[0;36m";
    BLUE="\033[0;34m";
    RED="\033[0;31m";
    YELLOW="\033[0;33m";
    GRAY="\033[0;37m";
    BOLD="\033[1m";
    NC="\033[0m";
fi

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

## Bash function that only prints if debugging is enabled.
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
    echo -e "  curl https://dl.elide.dev/cli/v1/snapshot/install.sh | bash";
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
    set -x;
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

# default to downloading gzip variant, but consume XZ or Brotli variant if the tools are locally available to decompress
# the resulting archive. XZ and Brotli do not ship with mac, but developers often have these tools.
COMPRESSION_TOOL="gzip";
if [ -x "$(command -v bzip2)" ]; then
    debug "Found compression: bzip2"
    COMPRESSION_TOOL="bzip2";
    COMPRESSION="bz2";
fi
if [ -x "$(command -v zstd)" ]; then
    debug "Found compression: zstd"
    COMPRESSION_TOOL="zstd";
    COMPRESSION="zst";
fi
if [ -x "$(command -v xz)" ]; then
    debug "Found compression: xz"
    COMPRESSION_TOOL="xz";
    COMPRESSION="xz";
fi
debug "Using compression tool: $COMPRESSION_TOOL (extension $COMPRESSION)";

## resolve install directory
PARAM_INSTALL_DIR=$(echo "$@" | grep -o -E "install-dir=? ?([^ ]+)" | cut -d' ' -f2 | cut -d'=' -f2 || $INSTALL_DIR);
INSTALL_DIR=${PARAM_INSTALL_DIR:-$DEFAULT_INSTALL_DIR};
debug "Resolved install dir: $INSTALL_DIR";

## resolve architecture
ARCH=$(uname -m);
debug "Resolved architecture: $ARCH";

if [ "$ARCH" = "x86_64" ]; then
    ARCH="amd64"
fi
if [ "$ARCH" = "arm64" ]; then
    ARCH="aarch64"
fi

## resolve platform
OS=$(uname -s);
debug "Resolved OS: $ARCH";

if [ "$OS" = "Linux" ]; then
    OS="linux"
fi
if [ "$OS" = "Darwin" ]; then
    OS="darwin"
fi

## issue fingerprint

## if host ID exists, use it
if [ -f ~/.elide/.host_id ]; then
    HOST_ID=$(cat ~/.elide/.host_id);
    debug "Host fingerprint loaded: $HOST_ID";
else
    ## otherwise, generate it
    HOST_ID=$(uuidgen);
    debug "Issued host fingerprint: $HOST_ID";
    mkdir -p ~/.elide;
    echo "$HOST_ID" >> ~/.elide/.host_id;
fi

VARIANT="$OS-$ARCH";
DOWNLOAD_ENDPOINT="$DOWNLOAD_BASE/$TOOL/$VERSION/$RELEASE/$VARIANT/$TOOL_REVISION/$BINARY.$COMPRESSION";
debug "Download endpoint: $DOWNLOAD_ENDPOINT";

say "Installing Elide (variant: $VARIANT, version: $TOOL_REVISION)...";

DEBUG_FLAGS="-vv";
CURL_ARGS="--no-buffer --progress-bar --location --fail --tlsv1.2 --retry 3 --retry-delay 2 --http2";

debug "Downloading binary with command: curl $CURL_ARGS";

DECOMPRESS_ARGS="-d";
if [ "$ENABLE_DEBUG" = true ]; then
    CURL_ARGS="$CURL_ARGS $DEBUG_FLAGS";
    DECOMPRESS_ARGS="-d -v";
    set -x;
fi

debug "Decompressing with command: $COMPRESSION_TOOL $DECOMPRESS_ARGS";

## okay, it's time to download the binary and decompress it as we go.

# shellcheck disable=SC2086
mkdir -p "$INSTALL_DIR" && curl $CURL_ARGS -H "User-Agent: elide-installer/$INSTALLER_VERSION" -H "Elide-Host-ID: $HOST_ID" $DOWNLOAD_ENDPOINT | $COMPRESSION_TOOL $DECOMPRESS_ARGS > "$INSTALL_DIR/$BINARY" && chmod +x "$INSTALL_DIR/$BINARY";
set +x;

## test the binary
if [ -x "$INSTALL_DIR/$BINARY" ]; then
    debug "Binary installed successfully.";

    # unless the user has opted out, run command help
    if [ "$SHOW_BANNER" = true ]; then
        "$INSTALL_DIR/$BINARY" --help;
        echo "";
    fi
else
    debug "Binary failed to install Path \"$INSTALL_DIR/$BINARY\" does not exist or is not executable.";
    exit 1;
fi

## if we're here, we're done!
echo -e "Elide installed successfully! 🎉";

## check to see if it's on the PATH.
IS_ON_PATH="false";
if [ -x "$(command -v $BINARY)" ]; then
    IS_ON_PATH="true";
else
    if [ "$INSTALL_INTO_PATH" == true ]; then
        DID_INSTALL="false";

        ## look for a regular `.profile` file
        if [ -f ~/.profile ]; then
            DID_INSTALL="true";
            IS_ON_PATH="true";
            echo "" >> ~/.profile;
            echo "# Elide PATH export" >> ~/.profile;
            echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >> ~/.profile;
        fi

        ## look for a zsh profile, install the install dir onto the path there if found
        if [ "$DID_INSTALL" != true ]; then
            if [ -f ~/.zshrc ]; then
                DID_INSTALL="true";
                IS_ON_PATH="true";
                echo "" >> ~/.zshrc;
                echo "# Elide PATH export" >> ~/.zshrc;
                echo "export PATH=\"\$PATH:$INSTALL_DIR\"" >> ~/.zshrc;
            fi
        fi

        ## look for a bash profile, install the install dir onto the path there if found
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
