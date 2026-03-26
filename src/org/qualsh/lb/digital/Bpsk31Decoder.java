package org.qualsh.lb.digital;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * BPSK31 (PSK31) decoder operating on raw 48 kHz / 16-bit / mono PCM audio.
 *
 * <h2>Signal flow</h2>
 * <ol>
 *   <li>PCM bytes arrive via {@link #feed(byte[])}.</li>
 *   <li>Each sample is mixed to baseband: I = s·cos(2πf_c·n/F_s), Q = s·sin(2πf_c·n/F_s).</li>
 *   <li>A single-pole IIR low-pass filter (~47 Hz) retains only the baseband signal.</li>
 *   <li>Integrate-and-dump over one symbol period (1536 samples at 48 kHz / 31.25 baud).</li>
 *   <li>Phase of the accumulated complex value is compared to the previous symbol:
 *       |Δφ| &gt; 90° = bit 1 (phase reversal), otherwise = bit 0.</li>
 *   <li>PSK31 Varicode decoding: two consecutive 0 bits mark a character boundary.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>
 *   Bpsk31Decoder dec = new Bpsk31Decoder(text -&gt; System.out.print(text));
 *   dec.setCarrierHz(1000.0);   // tune to the PSK31 carrier in the audio
 *   dec.feed(pcmBytes);         // 48 kHz / 16-bit / mono PCM
 * </pre>
 *
 * <h2>Tuning</h2>
 * <p>Call {@link #setCarrierHz} whenever the user clicks a new frequency on the
 * waterfall display.  The decoder resets its filter state automatically so it
 * re-acquires phase lock within a few symbol periods.
 *
 * <h2>Varicode</h2>
 * <p>The table is the standard G3PLX PSK31 Varicode: all 128 ASCII characters,
 * code words 1–10 bits long, no code word contains "00" or ends with "0".
 */
public class Bpsk31Decoder {

    // ── Audio constants ────────────────────────────────────────────────────────

    private static final int    SAMPLE_RATE        = 48_000;
    private static final double SYMBOL_RATE        = 31.25;

    /**
     * Samples per symbol: 48000 / 31.25 = 1536 (exact integer).
     */
    private static final int SAMPLES_PER_SYMBOL = (int) Math.round(SAMPLE_RATE / SYMBOL_RATE);

    /**
     * IIR single-pole low-pass filter alpha.
     * Cutoff ≈ 1.5 × symbol_rate ≈ 47 Hz.
     * α = 1 − exp(−2π · fc / Fs)
     */
    private static final double LPF_ALPHA =
            1.0 - Math.exp(-2.0 * Math.PI * (SYMBOL_RATE * 1.5) / SAMPLE_RATE);

    // ── Mutable state ─────────────────────────────────────────────────────────

    private double  carrierHz    = 1000.0;
    private long    sampleIndex  = 0;

    // IIR low-pass filter state
    private double lpfI = 0.0;
    private double lpfQ = 0.0;

    // Integrate-and-dump accumulator
    private double accumI     = 0.0;
    private double accumQ     = 0.0;
    private int    accumCount = 0;

    // Differential BPSK phase tracking
    private double  prevPhase   = 0.0;
    private boolean firstSymbol = true;

    // Varicode bit accumulator (pending-zero technique for correct gap detection)
    private int varBits      = 0;  // MSB = oldest received bit
    private int varLen       = 0;  // number of bits accumulated
    private int pendingZeros = 0;  // zeros seen but not yet flushed to varBits

    // ── Output callback ───────────────────────────────────────────────────────

    private final Consumer<String> textCallback;

    // ── Varicode lookup ───────────────────────────────────────────────────────

    /**
     * Maps PSK31 Varicode bit-string (MSB first, as received from the
     * differential decoder) to the corresponding ASCII character.
     *
     * Source: G3PLX PSK31 standard Varicode table (verified from reference
     * implementation at github.com/yhuag/varicode-decoder).
     *
     * Properties:
     * <ul>
     *   <li>All 128 ASCII characters (0–127) are represented.</li>
     *   <li>No code word contains the two-bit separator "00".</li>
     *   <li>All code words end with bit 1 (never bit 0).</li>
     *   <li>Code lengths range from 1 bit (space) to 10 bits.</li>
     * </ul>
     */
    private static final Map<String, Character> VARICODE_MAP = new HashMap<>(256);

    static {
        // Indexed by ASCII code value (0–127).
        // All entries end in '1' and contain no "00".
        final String[] TABLE = {
            /* 000 NUL */ "1010101011",
            /* 001 SOH */ "1011011011",
            /* 002 STX */ "1011101101",
            /* 003 ETX */ "1101110111",
            /* 004 EOT */ "1011101011",
            /* 005 ENQ */ "1101011111",
            /* 006 ACK */ "1011101111",
            /* 007 BEL */ "1011111101",
            /* 008  BS */ "1011111111",
            /* 009  HT */ "11101111",
            /* 010  LF */ "11101",
            /* 011  VT */ "1101101111",
            /* 012  FF */ "1011011101",
            /* 013  CR */ "11111",
            /* 014  SO */ "1101110101",
            /* 015  SI */ "1110101011",
            /* 016 DLE */ "1011110111",
            /* 017 DC1 */ "1011110101",
            /* 018 DC2 */ "1110101101",
            /* 019 DC3 */ "1110101111",
            /* 020 DC4 */ "1101011011",
            /* 021 NAK */ "1101101011",
            /* 022 SYN */ "1101101101",
            /* 023 ETB */ "1101010111",
            /* 024 CAN */ "1101111011",
            /* 025  EM */ "1101111101",
            /* 026 SUB */ "1110110111",
            /* 027 ESC */ "1101010101",
            /* 028  FS */ "1101011101",
            /* 029  GS */ "1110111011",
            /* 030  RS */ "1011111011",
            /* 031  US */ "1101111111",
            /* 032  SP */ "1",
            /* 033   ! */ "111111111",
            /* 034   " */ "101011111",
            /* 035   # */ "111110101",
            /* 036   $ */ "111011011",
            /* 037   % */ "1011010101",
            /* 038   & */ "1010111011",
            /* 039   ' */ "101111111",
            /* 040   ( */ "11111011",
            /* 041   ) */ "11110111",
            /* 042   * */ "101101111",
            /* 043   + */ "111011111",
            /* 044   , */ "1110101",
            /* 045   - */ "110101",
            /* 046   . */ "1010111",
            /* 047   / */ "110101111",
            /* 048   0 */ "10110111",
            /* 049   1 */ "10111101",
            /* 050   2 */ "11101101",
            /* 051   3 */ "11111111",
            /* 052   4 */ "101110111",
            /* 053   5 */ "101011011",
            /* 054   6 */ "101101011",
            /* 055   7 */ "110101101",
            /* 056   8 */ "110101011",
            /* 057   9 */ "110110111",
            /* 058   : */ "11110101",
            /* 059   ; */ "110111101",
            /* 060   < */ "111101101",
            /* 061   = */ "1010101",
            /* 062   > */ "111010111",
            /* 063   ? */ "1010101111",
            /* 064   @ */ "1010111101",
            /* 065   A */ "1111101",
            /* 066   B */ "11101011",
            /* 067   C */ "10101101",
            /* 068   D */ "10110101",
            /* 069   E */ "1110111",
            /* 070   F */ "11011011",
            /* 071   G */ "11111101",
            /* 072   H */ "101010101",
            /* 073   I */ "1111111",
            /* 074   J */ "111111101",
            /* 075   K */ "101111101",
            /* 076   L */ "11010111",
            /* 077   M */ "10111011",
            /* 078   N */ "11011101",
            /* 079   O */ "10101011",
            /* 080   P */ "11010101",
            /* 081   Q */ "111011101",
            /* 082   R */ "10101111",
            /* 083   S */ "1101111",
            /* 084   T */ "1101101",
            /* 085   U */ "101010111",
            /* 086   V */ "110110101",
            /* 087   W */ "101011101",
            /* 088   X */ "101110101",
            /* 089   Y */ "101111011",
            /* 090   Z */ "1010101101",
            /* 091   [ */ "111110111",
            /* 092   \ */ "111101111",
            /* 093   ] */ "111111011",
            /* 094   ^ */ "1010111111",
            /* 095   _ */ "101101101",
            /* 096   ` */ "1011011111",
            /* 097   a */ "1011",
            /* 098   b */ "1011111",
            /* 099   c */ "101111",
            /* 100   d */ "101101",
            /* 101   e */ "11",
            /* 102   f */ "111101",
            /* 103   g */ "1011011",
            /* 104   h */ "101011",
            /* 105   i */ "1101",
            /* 106   j */ "111101011",
            /* 107   k */ "10111111",
            /* 108   l */ "11011",
            /* 109   m */ "111011",
            /* 110   n */ "1111",
            /* 111   o */ "111",
            /* 112   p */ "111111",
            /* 113   q */ "110111111",
            /* 114   r */ "10101",
            /* 115   s */ "10111",
            /* 116   t */ "101",
            /* 117   u */ "110111",
            /* 118   v */ "1111011",
            /* 119   w */ "1101011",
            /* 120   x */ "11011111",
            /* 121   y */ "1011101",
            /* 122   z */ "111010101",
            /* 123   { */ "1010110111",
            /* 124   | */ "110111011",
            /* 125   } */ "1010110101",
            /* 126   ~ */ "1011010111",
            /* 127 DEL */ "1110110101",
        };

        for (int c = 0; c < TABLE.length; c++) {
            String code = TABLE[c];
            if (code == null || code.isEmpty()) continue;
            VARICODE_MAP.put(code, (char) c);
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Create a decoder.
     *
     * @param textCallback receives each decoded character as a one-char String;
     *                     called on the thread that invokes {@link #feed}
     */
    public Bpsk31Decoder(Consumer<String> textCallback) {
        if (textCallback == null) throw new IllegalArgumentException("textCallback must not be null");
        this.textCallback = textCallback;
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    /**
     * Set the BPSK31 carrier frequency in Hz (default 1000 Hz).
     * Resets all filter and decoder state so the decoder re-acquires on the
     * new frequency within a few symbol periods.
     *
     * @param hz carrier frequency (typically 500–2500 Hz)
     */
    public synchronized void setCarrierHz(double hz) {
        this.carrierHz = hz;
        resetState();
    }

    /** @return current carrier frequency in Hz */
    public synchronized double getCarrierHz() {
        return carrierHz;
    }

    /**
     * Reset all DSP and Varicode state without changing the carrier frequency.
     * Call when switching from one audio file to another.
     */
    public synchronized void reset() {
        resetState();
    }

    // ── Audio input ───────────────────────────────────────────────────────────

    /**
     * Feed raw PCM audio for decoding.
     *
     * <p>Format: 16-bit signed little-endian, 48 kHz, mono
     * (matching {@link org.qualsh.lb.digital.AudioRouter#FORMAT}).
     *
     * <p>Safe to call repeatedly; DSP state is preserved across calls.
     * The {@code textCallback} is invoked synchronously whenever a
     * character is decoded.
     *
     * @param pcm raw PCM bytes (must have even length ≥ 2)
     */
    public synchronized void feed(byte[] pcm) {
        if (pcm == null || pcm.length < 2) return;

        final double fc          = carrierHz;
        final int    numSamples  = pcm.length / 2;

        for (int i = 0; i < numSamples; i++) {
            // ── Decode little-endian 16-bit signed sample → float [-1, +1) ───
            int    lo = pcm[i * 2]     & 0xFF;
            int    hi = pcm[i * 2 + 1] & 0xFF;
            double s  = (short) ((hi << 8) | lo) / 32768.0;

            // ── Step 1: Mix to baseband ───────────────────────────────────────
            double ang = 2.0 * Math.PI * fc * sampleIndex / SAMPLE_RATE;
            double mixI = s * Math.cos(ang);
            double mixQ = s * Math.sin(ang);
            sampleIndex++;

            // ── Step 2: IIR single-pole low-pass filter ───────────────────────
            lpfI += LPF_ALPHA * (mixI - lpfI);
            lpfQ += LPF_ALPHA * (mixQ - lpfQ);

            // ── Step 3: Integrate-and-dump over one symbol period ─────────────
            accumI += lpfI;
            accumQ += lpfQ;

            if (++accumCount >= SAMPLES_PER_SYMBOL) {
                processSymbol(accumI, accumQ);
                accumI = accumQ = 0.0;
                accumCount = 0;
            }
        }
    }

    // ── Symbol-level processing ───────────────────────────────────────────────

    private void processSymbol(double I, double Q) {
        double mag = Math.sqrt(I * I + Q * Q);

        // Skip near-silence; also reset phase reference to avoid stale state
        if (mag < 1e-5) {
            firstSymbol = true;
            return;
        }

        double phase = Math.atan2(Q, I);

        if (firstSymbol) {
            prevPhase   = phase;
            firstSymbol = false;
            return;
        }

        // Differential phase decision
        double dPhase = phase - prevPhase;
        prevPhase = phase;

        // Normalise to (−π, π]
        while (dPhase >  Math.PI) dPhase -= 2.0 * Math.PI;
        while (dPhase < -Math.PI) dPhase += 2.0 * Math.PI;

        // |Δφ| > 90° → phase reversal → bit 1 (Varicode '1')
        // |Δφ| ≤ 90° → no change      → bit 0 (Varicode '0')
        int bit = (Math.abs(dPhase) > Math.PI / 2.0) ? 1 : 0;

        decodeBit(bit);
    }

    // ── Varicode bit accumulator ──────────────────────────────────────────────

    /**
     * Accumulates bits using the "pending-zeros" technique.
     *
     * <p>Because all PSK31 Varicode code words end with bit 1, a run of
     * two or more consecutive 0 bits always indicates an inter-character gap
     * and never a trailing sequence within a code word.  The pending-zeros
     * approach defers adding a 0 bit to the accumulator until we see whether
     * it is part of the code word (followed by a 1) or the start of the gap
     * (followed by another 0).
     */
    private void decodeBit(int bit) {
        if (bit == 1) {
            // Flush any zeros that were part of the code word
            for (int z = 0; z < pendingZeros; z++) {
                if (varLen < 20) {
                    varBits = (varBits << 1);  // append 0
                    varLen++;
                }
            }
            pendingZeros = 0;

            // Append the 1
            if (varLen < 20) {
                varBits = (varBits << 1) | 1;
                varLen++;
            }
        } else {
            // Bit 0: defer until we see whether this is part of the gap
            pendingZeros++;

            if (pendingZeros >= 2) {
                // Inter-character gap: decode whatever is in the accumulator
                if (varLen > 0) {
                    emitVaricode(varBits, varLen);
                }
                varBits      = 0;
                varLen       = 0;
                pendingZeros = 0;
            }
        }
    }

    // ── Varicode lookup ───────────────────────────────────────────────────────

    private void emitVaricode(int bits, int len) {
        // Reconstruct the MSB-first bit string for map lookup
        char[] chars = new char[len];
        for (int i = len - 1; i >= 0; i--) {
            chars[i] = (char) ('0' + (bits & 1));
            bits >>= 1;
        }
        Character c = VARICODE_MAP.get(new String(chars));
        if (c != null) {
            textCallback.accept(String.valueOf(c));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void resetState() {
        sampleIndex  = 0;
        lpfI = lpfQ  = 0.0;
        accumI = accumQ = 0.0;
        accumCount   = 0;
        prevPhase    = 0.0;
        firstSymbol  = true;
        varBits      = 0;
        varLen       = 0;
        pendingZeros = 0;
    }
}
