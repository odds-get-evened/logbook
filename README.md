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

---

## Digital Modes

### Overview

The Digital Modes window is a built-in signal decoder and encoder designed for amateur radio operators who work with computer-assisted modes. It gives you a live spectrum display, a scrolling waterfall, and a decoding engine — all in one panel — so you can listen to, analyze, and decode digital signals without leaving the logbook. The window supports seven popular modes and works equally well with live audio streamed from your radio, a pre-recorded WAV file you upload, or a fresh recording you capture on the spot. Whether you are chasing DX contacts on FT8 or monitoring a WSPR propagation beacon, the Digital Modes window handles both the receiving and the transmitting side.

---

### Opening the Digital Modes Window

You can open the Digital Modes window in any of three ways:

- From the menu bar, choose **Tools → Digital Modes**.
- Click the **Digital Modes** button on the main toolbar.
- Press **Ctrl+D** on your keyboard.

The window opens as a separate panel alongside the main logbook. You do not need to close it to log a contact — both windows work at the same time, so you can decode a signal and record the QSO without switching back and forth.

---

### Supported Modes

The Digital Modes window supports the following seven modes. Select the mode you want from the drop-down list at the top of the window before you begin decoding or encoding.

**FT8** — A weak-signal mode developed for amateur HF and VHF contacts. FT8 transmissions last exactly 15 seconds and carry a short exchange of callsigns, signal reports, and grid squares. It is one of the most popular digital modes in amateur radio today because it can complete contacts under band conditions that would defeat voice or Morse code. At least 15 seconds of continuous audio must be loaded before the decoder can produce results.

**FT4** — A faster variant of FT8 designed for contest operating. Each transmission lasts about 7.5 seconds, making QSO rates roughly twice as fast as FT8. Like FT8, it uses tightly timed slots, so your system clock must be accurate. It is well suited for digital contest events.

**WSPR** — Weak Signal Propagation Reporter. WSPR transmissions carry only a callsign, a Maidenhead grid square, and a transmit power level, and are used to map propagation paths around the world rather than to hold conversations. A full WSPR cycle takes 2 minutes, so at least 2 minutes of audio must be loaded for decoding to work. Encoding a WSPR message also requires a Maidenhead grid square to be set in Preferences, in addition to your callsign.

**JS8** — A conversational mode based on the same underlying technology as FT8 but designed for free-form keyboard-to-keyboard chat. JS8 is popular for nets, message passing, and emergency communications because it does not limit you to a fixed exchange format.

**RTTY** — Radioteletype, one of the oldest digital modes still in active use. RTTY sends text character by character using two audio tones and is commonly heard in HF contests and on traditional digital sub-bands. No special timing requirements apply — RTTY decodes continuously as long as audio is present.

**PSK31** — A narrow-bandwidth mode designed for real-time keyboard chat on HF. PSK31 fits in less than 100 Hz of spectrum, making it extremely efficient on crowded bands. Like RTTY, it decodes continuously and has no timing restrictions.

**Olivia** — A robust multi-tone mode designed to get through when conditions are poor. Olivia uses a wide bandwidth and a slow character rate to achieve reliable copy even on very weak or fading signals. It is often used on 40 and 80 meters for casual contacts and emergency traffic.

---

### Getting Audio Into the Application

#### Connecting a Radio Rig

If you have configured a USB serial connection in Preferences, the application can stream audio directly from your radio. Open **Preferences** from the main menu, navigate to the **Rig** tab, and enter the serial port name (for example, `/dev/ttyUSB0` on Linux or `COM3` on Windows) along with the correct baud rate for your radio. Once the connection is established, audio from your radio appears in the spectrum display automatically — no further action is needed.

#### Uploading a WAV File

Click the **Upload WAV** button to load a recording from your computer. A standard file-chooser dialog will open; navigate to your WAV file and click Open. The file loads immediately and the spectrum display updates to show its contents. Uploading a new file always replaces the previous one, so save any recording you want to keep before uploading another. The application accepts WAV files in mono or stereo at any common sample rate.

#### Recording From Your Radio

Click the **Record** button to capture live audio from your radio. A save dialog will appear so you can choose where the file will be stored and what to name it. Recording begins as soon as you confirm the location. When you are finished capturing, press **Stop Recording** — the file is saved automatically and loaded into the application, ready for decoding. This is a convenient way to preserve a signal for detailed analysis later, or to build a library of recordings for a particular band or mode.

---

### The Spectrum Display

#### The Frequency Graph

The top portion of the spectrum display shows signal strength across a range of frequencies in real time. Each vertical bar represents the energy present at a particular frequency — taller bars indicate a stronger signal at that frequency. The green trace running across the display is a smoothed average that helps you see the overall shape of the spectrum and spot signals that rise above the noise floor.

#### The Waterfall Display

Below the frequency graph is the waterfall — a scrolling color display where time moves downward. Each new slice of spectrum is added at the top and previous slices scroll down, giving you a history of what has been received. Brighter, warmer colors represent stronger signals, while dark colors represent silence or low-level noise. Signals appear as vertical lines (steady carriers), diagonal streaks (signals drifting in frequency), or short bursts (timed transmissions like FT8). The color scale runs from black for no signal, through blue and green for weak signals, up through yellow for moderate signals, and white for a very strong signal.

#### Setting the Center Frequency and Bandwidth

A yellow vertical line on the frequency graph marks the current center frequency — the frequency the decoder is focused on. A pair of orange vertical bars on either side of the yellow line shows the current decoding bandwidth.

To move the center frequency, click anywhere on the frequency graph. The yellow line jumps to that position and the decoder re-centers on the new frequency.

To adjust the bandwidth, click and drag the left or right orange bar. Dragging outward widens the bandwidth to capture more of the spectrum; dragging inward narrows it to focus on a single signal and filter out adjacent interference.

When you switch modes, the application automatically adjusts the bandwidth to the recommended setting for that mode, so you generally do not need to set it manually unless you want to fine-tune it.

---

### Decoding Signals

Decoding happens automatically while audio is loaded — you do not need to press a decode button. As signals are detected, results appear in the green terminal-style text area near the bottom of the window. Each line shows the timestamp of the detection, the mode name, the center frequency in hertz, and the decoded text content of the transmission.

Below the text area is the decode log table. Every signal the decoder detects is recorded as a row in this table, so you have a running history of everything heard during the session. Click any row in the table to see the full decoded text for that signal in the text area above. The table can be scrolled and reviewed at any time without affecting ongoing decoding.

---

### Playback Controls

The playback controls let you listen to a loaded WAV file through your computer's speakers or headphones.

- **Play** — Starts playback from the current position. You will hear the audio exactly as it was recorded.
- **Pause** — Holds playback at the current position. Press Play again to resume from the same spot.
- **Stop** — Stops playback and returns to the beginning of the file.
- **Loop** — Repeats the audio continuously from start to finish, then starts over. This is especially useful when you are studying a repeated signal or waiting for a pattern to appear. Toggle it off to stop looping.

Loading a new WAV file or starting a new recording automatically stops any playback that is currently in progress.

---

### Encoding and Transmitting

The **Encode** tab lets you compose a digital message and transmit it through your connected radio.

1. Select the mode you want to use from the mode drop-down at the top of the window.
2. Click the **Encode** tab.
3. Type your message in the message field. Most modes will automatically include your callsign in every transmission — make sure your callsign is set in Preferences before you begin.
4. Press **Encode** to generate the audio signal for your message.
5. Optionally, press **Preview Audio** to hear what the encoded signal will sound like before transmitting. This lets you verify the message sounds correct.
6. When you are ready, press **Transmit** to send the signal through your radio.

**Transmit** requires a radio rig to be connected and configured in Preferences. If no rig is connected, the button will have no effect.

**Note for WSPR users:** In addition to your callsign, WSPR requires a Maidenhead grid square. Set both in Preferences before encoding a WSPR message.

**Note for FT8 and WSPR users:** Messages in these two modes are limited to 13 characters.

---

### Tips for Best Results

- Switch to the correct mode before decoding. The decoder only looks for the signal pattern of the selected mode, so selecting FT8 while a PSK31 signal is playing will produce no results.
- For FT8, make sure at least 15 seconds of audio are loaded before decoding — shorter clips will not produce results.
- For WSPR, at least 2 minutes of continuous audio are needed for the decoder to complete a full decoding cycle.
- Use the bandwidth bars to zoom in on a specific signal and filter out adjacent interference. Narrowing the bandwidth around the signal you want can significantly improve decode rates on a busy band.
- The waterfall is a great way to spot signals visually before you try to decode them. Look for vertical lines (steady carriers), diagonal streaks (signals drifting in frequency), or short evenly spaced bursts (timed modes like FT8).
- If you upload a WAV file recorded from a different radio, make sure the original recording was made in USB (Upper Sideband) mode. Most digital HF modes are designed for USB, and recordings made in LSB or FM will not decode correctly.
- Set your callsign in Preferences before using the Encode tab. Most modes embed your callsign directly in every transmission, and the Encode button will remain unavailable until a callsign is configured.

---

### Preferences Integration

The **Preferences** dialog — accessible from the main menu — is where you connect the Digital Modes window to your station setup. Use it to configure the following:

- **Serial port and baud rate** — Required if you want to stream audio directly from your radio or use the Transmit function. Enter the port name and baud rate that match your radio's interface.
- **Operator callsign** — Required for encoding any digital mode. Your callsign is included automatically in outgoing transmissions for most modes.
- **Maidenhead grid square** — Required specifically for WSPR. Enter the four- or six-character grid square for your location.

All Preferences settings are saved automatically and remembered the next time you open the application.

---

### Troubleshooting

| Issue | Likely Cause | What To Try |
|---|---|---|
| No signal appears in the spectrum display | No audio is loaded or the rig is not connected | Upload a WAV file, or check the serial port setting in Preferences |
| Decoder produces no results | Audio clip is too short for the selected mode | Load a longer recording — FT8 needs at least 15 seconds, WSPR needs at least 2 minutes |
| Waterfall is all black | Audio level is very low | Check radio volume or the audio level in your WAV file |
| Encode button is grayed out | Callsign has not been set | Open Preferences and enter your amateur radio callsign |
| Transmit button does nothing | No rig is connected or configured | Connect your radio via USB and configure the serial port in Preferences |
| Uploading a file replaces my previous audio | This is expected behavior | Save any recording you want to keep before uploading a new file |
