#!/usr/bin/env bash
# Logbook Installer
# Builds and installs the Logbook shortwave listening log app

set -e

APP_NAME="logbook"
APP_VERSION="0.0.7"
INSTALL_DIR="${HOME}/.local/share/${APP_NAME}"
BIN_DIR="${HOME}/.local/bin"
DESKTOP_DIR="${HOME}/.local/share/applications"
ICONS_DIR="${HOME}/.local/share/icons/hicolor"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }
die()     { error "$*"; exit 1; }

# ── Prerequisite checks ────────────────────────────────────────────────────────

check_java() {
    if ! command -v java &>/dev/null; then
        die "Java is not installed. Please install Java 22 or later and try again.
  Debian/Ubuntu:  sudo apt install openjdk-22-jdk
  Fedora/RHEL:    sudo dnf install java-22-openjdk
  Arch Linux:     sudo pacman -S jdk22-openjdk
  macOS (brew):   brew install openjdk@22"
    fi

    local java_version
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [[ "${java_version}" -lt 22 ]]; then
        die "Java 22 or later is required (found Java ${java_version}).
Please upgrade your Java installation."
    fi
    info "Java ${java_version} found."
}

check_maven() {
    if ! command -v mvn &>/dev/null; then
        die "Apache Maven is not installed. Please install Maven 3.9+ and try again.
  Debian/Ubuntu:  sudo apt install maven
  Fedora/RHEL:    sudo dnf install maven
  Arch Linux:     sudo pacman -S maven
  macOS (brew):   brew install maven"
    fi
    info "Maven found: $(mvn --version | head -1)."
}

# ── Build ──────────────────────────────────────────────────────────────────────

build_jar() {
    info "Building ${APP_NAME} v${APP_VERSION}..."
    mvn --batch-mode clean package -DskipTests -q
    if [[ ! -f "target/logbook.jar" ]]; then
        die "Build failed: target/logbook.jar not found."
    fi
    info "Build successful."
}

# ── Install ────────────────────────────────────────────────────────────────────

install_files() {
    info "Installing to ${INSTALL_DIR}..."
    mkdir -p "${INSTALL_DIR}" "${BIN_DIR}"

    cp target/logbook.jar "${INSTALL_DIR}/logbook.jar"

    # Launcher script
    cat > "${BIN_DIR}/${APP_NAME}" <<LAUNCHER
#!/usr/bin/env bash
exec java -jar "${INSTALL_DIR}/logbook.jar" "\$@"
LAUNCHER
    chmod +x "${BIN_DIR}/${APP_NAME}"
    info "Launcher written to ${BIN_DIR}/${APP_NAME}."
}

install_icons() {
    local icon_src="src/imgs"
    if [[ ! -d "${icon_src}" ]]; then
        warn "Icon directory not found, skipping icon installation."
        return
    fi

    for size in 16 32 48 128; do
        local icon_file="${icon_src}/lb_${size}x${size}.png"
        if [[ -f "${icon_file}" ]]; then
            local dest="${ICONS_DIR}/${size}x${size}/apps"
            mkdir -p "${dest}"
            cp "${icon_file}" "${dest}/${APP_NAME}.png"
        fi
    done

    command -v gtk-update-icon-cache &>/dev/null && \
        gtk-update-icon-cache -f -t "${ICONS_DIR}" 2>/dev/null || true
    info "Icons installed."
}

install_desktop_entry() {
    mkdir -p "${DESKTOP_DIR}"
    cat > "${DESKTOP_DIR}/${APP_NAME}.desktop" <<DESKTOP
[Desktop Entry]
Version=1.0
Type=Application
Name=Logbook
GenericName=Shortwave Listening Log
Comment=A desktop shortwave listening log app
Exec=${BIN_DIR}/${APP_NAME}
Icon=${APP_NAME}
Categories=HamRadio;Utility;
Keywords=shortwave;radio;ham;listening;log;
StartupNotify=true
DESKTOP
    command -v update-desktop-database &>/dev/null && \
        update-desktop-database "${DESKTOP_DIR}" 2>/dev/null || true
    info "Desktop entry installed."
}

# ── PATH hint ──────────────────────────────────────────────────────────────────

check_path() {
    if [[ ":${PATH}:" != *":${BIN_DIR}:"* ]]; then
        warn "${BIN_DIR} is not in your PATH."
        echo
        echo "  Add it by appending one of the following to your shell config"
        echo "  (~/.bashrc, ~/.zshrc, etc.) and restarting your terminal:"
        echo
        echo '    export PATH="${HOME}/.local/bin:${PATH}"'
        echo
    fi
}

# ── Uninstall ──────────────────────────────────────────────────────────────────

uninstall() {
    info "Uninstalling ${APP_NAME}..."
    rm -f  "${BIN_DIR}/${APP_NAME}"
    rm -rf "${INSTALL_DIR}"
    rm -f  "${DESKTOP_DIR}/${APP_NAME}.desktop"
    for size in 16 32 48 128; do
        rm -f "${ICONS_DIR}/${size}x${size}/apps/${APP_NAME}.png"
    done
    info "Uninstall complete."
    exit 0
}

# ── Entry point ────────────────────────────────────────────────────────────────

usage() {
    echo "Usage: $0 [--uninstall]"
    echo
    echo "  (no args)    Build and install Logbook"
    echo "  --uninstall  Remove a previously installed Logbook"
    exit 0
}

main() {
    case "${1:-}" in
        --uninstall|-u) uninstall ;;
        --help|-h)      usage ;;
        "")             ;;
        *) die "Unknown option: $1. Run '$0 --help' for usage." ;;
    esac

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo " Logbook v${APP_VERSION} Installer"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    check_java
    check_maven
    build_jar
    install_files
    install_icons
    install_desktop_entry
    check_path

    echo
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    info "Installation complete!"
    echo
    echo "  Launch from terminal:   logbook"
    echo "  Or find it in your application menu under 'HamRadio / Utility'."
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

main "$@"
