# Logbook — Shortwave Listening Log

Logbook is a desktop app for shortwave radio listeners to keep track of every broadcast they hear. Record what you heard, when, and where the signal came from — all displayed on an interactive world map.

---

## Getting Started

### First Launch

Before logging anything, set up your receiving location:

1. Go to **Edit → Preferences**
2. Click the **Location** tab
3. Type your city or location name in the **Find Location** box and press **Find** — or click **Pick from Map** to click your spot directly on the map
4. Enter a name for your location (e.g. "Home" or "Back Porch")
5. Click **Save**

Your receiving location will be filled in automatically on every log entry from then on.

---

## The Main Window

The window is divided into three main areas:

```
┌────────────────────────────┬────────────────────┐
│  Log Table (top-left)      │                    │
│  Search • Filter • Results │   Log Entry Form   │
├────────────────────────────│   (right panel)    │
│  World Map (bottom-left)   │                    │
└────────────────────────────┴────────────────────┘
```

### Log Table

The log table lists every reception you've recorded. Each row shows:

| Column | What it means |
|--------|--------------|
| **Date** | Date of the reception |
| **Time (UTC)** | Time you heard the broadcast, in UTC |
| **Frequency** | Frequency in kHz |
| **Mode** | Broadcast mode (AM, SSB, CW, etc.) |
| **RX Location** | Where you were listening from |
| **Description** | Your notes about the broadcast |
| **TX Location** | Where the broadcast originated |

**Sorting:** Click any column header to sort by that column.

**Right-click a row** for a menu with options to Edit, zoom the map to that broadcast's signal path, or Delete the entry.

**Double-click a row** to zoom the map so both the transmitter and your receiver are visible at the same time.

### World Map

The map shows your logs as markers, with lines connecting transmitter and receiver locations so you can see signal paths at a glance.

- **Pan:** Click and drag to move around the map
- **Zoom:** Use the mouse wheel or the zoom buttons

When you select a log entry in the table, the map highlights the transmit and receive locations for that entry.

### Right Panel — Info & Entry Tabs

The right panel has two tabs:

- **Info** — shows details of whichever log entry is selected (read-only)
- **Entry** — the form for recording a new reception or editing an existing one

---

## Recording a Reception

Click the **Entry** tab on the right side to open the log entry form.

### Form Fields

| Field | What to enter |
|-------|--------------|
| **Frequency (kHz)** | The frequency you were tuned to, e.g. `9650` |
| **Mode** | The broadcast mode — select from the dropdown (AM is most common for shortwave) |
| **Date** | The date of the reception — click the calendar icon to pick from a calendar |
| **Time (UTC)** | The time in UTC when you heard the broadcast |
| **Description** | Your notes: signal quality, program content, anything you want to remember |
| **TX Location** | Click **Location…** to pick the transmitting station's location from the list |
| **My Location** | Filled in automatically from your preferences |

### Getting the Frequency from Your Radio

If your radio is connected via CAT control, click **From Radio** to read the current frequency directly — no typing needed. The small status dot next to the button shows grey (not connected) or green (connected and ready).

### Saving

Click **Save** to record the entry. Click **Cancel** to discard it and return to the Info tab.

---

## Searching and Filtering Your Log

Above the log table you'll find search and filter controls:

- **Search box** — type any text to instantly filter entries by frequency or description
- **This Hour** button — shows only logs from the current hour, handy during an active session
- **Reset** button — clears the search and shows all entries

You can also filter by listening location by selecting one in the **Locations** tab (see below).

---

## Managing Your Listening Locations

The **Locations** tab sits below the log table on the left. It lists all the places you've saved as your own listening spots.

- **Click a location** to filter the log table to entries from that spot, and to pan the map to it
- **Click on empty space** in the list to deselect and show all logs again

### Adding a New Listening Location

1. Click **Add** in the Locations tab
2. Enter a name for the location
3. Use **Find Location** to search by place name, or **Pick from Map** to click the spot on the map
4. Click **OK**

### Editing or Deleting a Location

Select a location in the list, then click **Edit** or **Delete**. You'll be asked to confirm before anything is deleted.

---

## Managing Stations and Transmitter Locations

Stations are the broadcasting organizations (e.g. BBC World Service, Voice of America). TX Locations are the specific transmitter sites those stations use.

### Adding a New Station

Go to **Tools → Station → New…** (or press `Ctrl+S`) and enter the station name.

### Adding a Transmitter Location

Go to **Tools → Location → New…** (or press `Ctrl+L`). Fill in:

| Field | What to enter |
|-------|--------------|
| **Station** | Which broadcaster operates this transmitter |
| **Name** | A label for this transmitter site |
| **Frequency (kHz)** | The frequency this transmitter broadcasts on |
| **Language** | The broadcast language |
| **Start / Stop Time** | Scheduled broadcast times |
| **Latitude / Longitude** | The transmitter's geographic coordinates |

---

## DX Cluster — Live Spots from Other Listeners

The DX Cluster panel shows real-time spots reported by other listeners around the world. It's a great way to find out what's on the air right now without having to scan yourself.

### Connecting

1. Go to **Tools → DX Cluster Settings…**
2. Pick a server from the **Known Servers** list (host and port fill in automatically), or enter your own
3. Enter your **callsign**
4. Click **Save**
5. Back in the main window, click **Connect** in the DX Cluster panel

The status dot shows grey (disconnected) or green (connected). A status bar shows how long you've been connected and how many spots have arrived.

### Using Spots

The spots table shows:

| Column | Meaning |
|--------|---------|
| **Time** | When the spot was received |
| **Frequency** | The spotted frequency in kHz |
| **Callsign** | The station being reported |
| **Spotter** | Who sent in the report |
| **Comment** | Any notes about the signal |

**Double-click a spot** to automatically tune your connected radio to that frequency and pre-fill the log entry form — ready to save with one more click.

---

## CAT Radio Control (Connecting Your Radio)

CAT control lets Logbook talk to your radio and read its current frequency automatically.

Go to **Tools → CAT Settings…** to set up your connection.

### Option 1 — rigctld (Recommended)

If you're using Hamlib's `rigctld` daemon:

- **Host** — usually `localhost`
- **Port** — usually `4532`

### Option 2 — Direct Serial / USB Cable

For a direct cable connection:

- **Port** — select your serial port from the dropdown
- **Baud Rate** — must match your radio's baud rate setting
- **Protocol** — choose YAESU, KENWOOD, or ICOM
- **CI-V Address** — for Icom radios, enter the hex CI-V address (usually `A4`)

Click **Test Connection** to confirm everything is working before you save.

---

## Exporting Your Log

Export your data from **File → Export…**:

| Format | Good for |
|--------|---------|
| **CSV** | Opening in a spreadsheet app (Excel, LibreOffice Calc, etc.) |
| **JSON** | Backing up your data or sharing it with other apps |
| **ADIF** | Importing into other ham radio logging programs |

A file dialog will ask you where to save the exported file.

---

## Preferences

Open preferences via **Edit → Preferences** (or `Ctrl+P`).

### Location Tab

Set or update your default receiving location:

- **Find Location** — search by place name to fill in coordinates automatically
- **Pick from Map** — click anywhere on the map to set your location
- **Reset** — clears the saved location

### Appearance Tab

Choose your preferred look:

| Theme | Style |
|-------|-------|
| **System Default** | Matches your operating system |
| **Flat Light** | Clean, bright theme |
| **Flat Dark** | Dark background, easier on the eyes at night |
| **Flat IntelliJ** | Slightly different light style |
| **Flat Darcula** | Dark with muted colors |

Theme changes take effect immediately — no restart needed.

### General Tab

Shows where your log database file is stored on your computer. Use **Browse…** to move it to a different folder, then click **Save** — your data is copied to the new location automatically.

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Q` or `Ctrl+X` | Quit Logbook |
| `Ctrl+P` | Open Preferences |
| `Ctrl+S` | Add a new station |
| `Ctrl+L` | Add a new TX location |

---

## Installation

### Quick Install (Linux / macOS)

```bash
git clone <repo-url>
cd logbook
./install.sh
```

After installation, launch from the terminal:

```bash
logbook
```

On Linux, Logbook also appears in your application menu under **HamRadio** or **Utility**.

### Uninstall

```bash
./install.sh --uninstall
```

---

## License

See repository for license details.
