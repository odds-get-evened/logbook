# logbook

**A desktop shortwave listening log app**

Track your shortwave radio receptions with a simple, searchable log. Logbook
stores your listening history in a local SQLite database and integrates with
the [hflog](https://github.com/white5moke/hflog) API for data acquisition and
storage.

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java        | 22 or later |
| Apache Maven | 3.9 or later *(build only)* |

---

## Installation

### Quick install (Linux / macOS)

Clone the repository and run the installer:

```bash
git clone <repo-url>
cd logbook
./install.sh
```

The installer will:

1. Verify your Java and Maven versions.
2. Build a self-contained fat JAR (`target/logbook.jar`).
3. Copy the JAR to `~/.local/share/logbook/`.
4. Write a launcher script to `~/.local/bin/logbook`.
5. Install application icons and a `.desktop` entry (Linux).

> **PATH note** — if `~/.local/bin` is not in your `PATH`, add this line to
> your shell config (`~/.bashrc`, `~/.zshrc`, etc.) and restart your terminal:
> ```bash
> export PATH="${HOME}/.local/bin:${PATH}"
> ```

### Uninstall

```bash
./install.sh --uninstall
```

---

## Building manually

If you prefer to build without installing:

```bash
mvn clean package -DskipTests
```

The fat JAR (all dependencies included) is written to `target/logbook.jar`.

Run it directly:

```bash
java -jar target/logbook.jar
```

---

## Running

After installation:

```bash
logbook
```

Or run the JAR directly from anywhere:

```bash
java -jar ~/.local/share/logbook/logbook.jar
```

On Linux you can also launch Logbook from your application menu under the
**HamRadio** or **Utility** category.

---

## Using the app

### Layout

The main window is divided into two panels:

| Panel | Purpose |
|-------|---------|
| **Left — Log table** | Scrollable list of all reception log entries |
| **Right — Log form** | Enter or edit a reception |

A debug console window also opens alongside the main window (development
builds).

### Logging a reception

Fill in the fields on the right-hand panel and submit:

- **Frequency** — in kHz (e.g. `9650`)
- **Station** — choose from the built-in list or type to search; pre-loaded
  with 23 shortwave broadcasters (All India Radio, Radio Free Asia, VOA, etc.)
- **Language** — 14 languages supported (Arabic, Chinese, English, French,
  German, Japanese, Spanish, and more)
- **Mode** — AM, SSB, CW, etc.
- **Time on / off** — UTC reception times
- **Location** — your receiver/listening location (set up first under
  **Tools → Location → New…**)
- **Notes** — free-text notes about the reception

### Menu reference

| Menu | Item | Shortcut | Action |
|------|------|----------|--------|
| File | Exit | `Ctrl+X` | Quit the application |
| Edit | Preferences | — | Open the preferences dialog |
| Tools → Station | New… | `Ctrl+S` | Add a new shortwave station |
| Tools → Location | New… | `Ctrl+L` | Add a new listening location |
| Help | About | — | Version and credits |

### Keyboard shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Q` | Quit |
| `Ctrl+S` | New station dialog |
| `Ctrl+L` | New location dialog |
| `Ctrl+X` | Exit (via menu) |

### Data storage

All data is stored in a local SQLite database:

```
~/<username>/LB/lb.db
```

The database is created automatically on first launch. No network connection is
required to use the core logging features.

---

## Project structure

```
logbook/
├── pom.xml              Maven build configuration
├── install.sh           Installer script (Linux / macOS)
├── README.md
└── src/
    ├── org/qualsh/lb/   Application source (Java 22)
    │   ├── App.java     Entry point
    │   ├── MainWin.java Main Swing window
    │   ├── data/        Database layer (SQLite)
    │   ├── log/         Log entry model
    │   ├── station/     Station model
    │   ├── location/    Receiver location model
    │   ├── language/    Language model
    │   ├── util/        Utilities (preferences, debug console, …)
    │   └── view/        Swing GUI components and dialogs
    └── imgs/            Application icons (16 / 32 / 48 / 128 px)
```

---

## License

See repository for license details.
