package se.djax.spelpojken;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

/**
 * Game Boy APU (Audio Processing Unit) implementation.
 * 
 * Channels:
 * 1: Square 1 with Sweep
 * 2: Square 2
 * 3: Wave (custom samples)
 * 4: Noise
 */
public class Apu {
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 4096;
    
    private final Cpu cpu;
    private AudioDevice audioDevice;
    private float[] buffer = new float[BUFFER_SIZE];
    private int bufferPtr = 0;
    
    // Frame Sequencer
    private int frameSequencerCycles = 0;
    private int frameSequencerStep = 0;
    
    // Global Control
    private boolean masterEnabled = true;
    
    // Channel 1 (Square with Sweep)
    private boolean ch1Enabled = false;
    private int ch1Timer = 0;
    private int ch1Period = 0;
    private int ch1Duty = 0;
    private int ch1DutyStep = 0;
    private int ch1Length = 0;
    private boolean ch1LengthEnabled = false;
    private int ch1Volume = 0;
    private int ch1InitialVolume = 0;
    private boolean ch1EnvelopeDir = false; // true = up, false = down
    private int ch1EnvelopePeriod = 0;
    private int ch1EnvelopeTimer = 0;
    
    // Channel 2 (Square)
    private boolean ch2Enabled = false;
    private int ch2Timer = 0;
    private int ch2Period = 0;
    private int ch2Duty = 0;
    private int ch2DutyStep = 0;
    private int ch2Length = 0;
    private boolean ch2LengthEnabled = false;
    private int ch2Volume = 0;
    private int ch2InitialVolume = 0;
    private boolean ch2EnvelopeDir = false;
    private int ch2EnvelopePeriod = 0;
    private int ch2EnvelopeTimer = 0;
    
    // Duty cycle patterns
    private static final int[][] DUTY_PATTERNS = {
        {0,0,0,0,0,0,0,1}, // 12.5%
        {1,0,0,0,0,0,0,1}, // 25%
        {1,0,0,0,0,1,1,1}, // 50%
        {0,1,1,1,1,1,1,0}  // 75%
    };

    public Apu(Cpu cpu) {
        this.cpu = cpu;
    }

    public void initAudio() {
        if (Gdx.audio != null && this.audioDevice == null) {
            this.audioDevice = Gdx.audio.newAudioDevice(SAMPLE_RATE, true);
            System.out.println("Audio device initialized successfully.");
        }
    }

    public void exec(int cycles) {
        if (!masterEnabled) return;

        // Simple ticking of components
        updateFrameSequencer(cycles);
        
        // Update Channel 1
        if (ch1Enabled) {
            ch1Timer -= cycles;
            if (ch1Timer <= 0) {
                ch1Period = getCh1Period();
                ch1Timer = (2048 - ch1Period) * 4;
                ch1DutyStep = (ch1DutyStep + 1) % 8;
            }
        }

        // Update Channel 2
        if (ch2Enabled) {
            ch2Timer -= cycles;
            if (ch2Timer <= 0) {
                ch2Period = getCh2Period();
                ch2Timer = (2048 - ch2Period) * 4;
                ch2DutyStep = (ch2DutyStep + 1) % 8;
            }
        }

        generateSample(cycles);
    }

    private void updateFrameSequencer(int cycles) {
        frameSequencerCycles += cycles;
        if (frameSequencerCycles >= 8192) { // 512 Hz
            frameSequencerCycles -= 8192;
            
            // Length counters (256 Hz)
            if (frameSequencerStep % 2 == 0) {
                updateLength();
            }
            
            // Envelopes (64 Hz)
            if (frameSequencerStep == 7) {
                updateEnvelopes();
            }
            
            frameSequencerStep = (frameSequencerStep + 1) % 8;
        }
    }

    private void updateLength() {
        if (ch1LengthEnabled && ch1Length > 0) {
            ch1Length--;
            if (ch1Length == 0) ch1Enabled = false;
        }
        if (ch2LengthEnabled && ch2Length > 0) {
            ch2Length--;
            if (ch2Length == 0) ch2Enabled = false;
        }
    }

    private void updateEnvelopes() {
        // Channel 1
        if (ch1EnvelopePeriod > 0) {
            ch1EnvelopeTimer--;
            if (ch1EnvelopeTimer <= 0) {
                ch1EnvelopeTimer = ch1EnvelopePeriod;
                if (ch1EnvelopeDir && ch1Volume < 15) ch1Volume++;
                else if (!ch1EnvelopeDir && ch1Volume > 0) ch1Volume--;
            }
        }
        // Channel 2
        if (ch2EnvelopePeriod > 0) {
            ch2EnvelopeTimer--;
            if (ch2EnvelopeTimer <= 0) {
                ch2EnvelopeTimer = ch2EnvelopePeriod;
                if (ch2EnvelopeDir && ch2Volume < 15) ch2Volume++;
                else if (!ch2EnvelopeDir && ch2Volume > 0) ch2Volume--;
            }
        }
    }

    private int sampleClock = 0;
    private void generateSample(int cycles) {
        sampleClock += cycles;
        int cyclesPerSample = GameBoy.CPU_CLOCK_SPEED / SAMPLE_RATE;
        
        while (sampleClock >= cyclesPerSample) {
            sampleClock -= cyclesPerSample;
            
            float sample1 = 0;
            if (ch1Enabled) {
                sample1 = (DUTY_PATTERNS[ch1Duty][ch1DutyStep] * 2 - 1) * (ch1Volume / 15.0f);
            }

            float sample2 = 0;
            if (ch2Enabled) {
                sample2 = (DUTY_PATTERNS[ch2Duty][ch2DutyStep] * 2 - 1) * (ch2Volume / 15.0f);
            }
            
            float mixed = (sample1 + sample2) * 0.5f;
            
            // Mono to Stereo
            buffer[bufferPtr++] = mixed * 0.4f; // Left
            buffer[bufferPtr++] = mixed * 0.4f; // Right
            
            if (bufferPtr >= BUFFER_SIZE) {
                if (audioDevice != null) {
                    audioDevice.writeSamples(buffer, 0, BUFFER_SIZE);
                }
                bufferPtr = 0;
            }
        }
    }

    private int getCh1Period() {
        int low = cpu.getRawMem(0xFF13);
        int high = cpu.getRawMem(0xFF14) & 0x07;
        return (high << 8) | low;
    }

    private int getCh2Period() {
        int low = cpu.getRawMem(0xFF18);
        int high = cpu.getRawMem(0xFF19) & 0x07;
        return (high << 8) | low;
    }

    public void writeRegister(int address, short value) {
        if (!masterEnabled && address != 0xFF26) return;

        switch (address) {
            // Channel 1
            case 0xFF10: // NR10 Sweep
                break;
            case 0xFF11: // NR11 Length/Duty
                ch1Duty = (value >> 6) & 0x03;
                ch1Length = 64 - (value & 0x3F);
                break;
            case 0xFF12: // NR12 Envelope
                ch1InitialVolume = (value >> 4) & 0x0F;
                ch1EnvelopeDir = (value & 0x08) != 0;
                ch1EnvelopePeriod = value & 0x07;
                break;
            case 0xFF13: // NR13 Period Low
                break;
            case 0xFF14: // NR14 Control/Period High
                ch1LengthEnabled = (value & 0x40) != 0;
                if ((value & 0x80) != 0) { // Trigger
                    ch1Enabled = true;
                    if (ch1Length == 0) ch1Length = 64;
                    ch1Volume = ch1InitialVolume;
                    ch1EnvelopeTimer = ch1EnvelopePeriod;
                    ch1Timer = (2048 - getCh1Period()) * 4;
                }
                break;

            // Channel 2
            case 0xFF16: // NR21 Length/Duty
                ch2Duty = (value >> 6) & 0x03;
                ch2Length = 64 - (value & 0x3F);
                break;
            case 0xFF17: // NR22 Envelope
                ch2InitialVolume = (value >> 4) & 0x0F;
                ch2EnvelopeDir = (value & 0x08) != 0;
                ch2EnvelopePeriod = value & 0x07;
                break;
            case 0xFF18: // NR23 Period Low
                break;
            case 0xFF19: // NR24 Control/Period High
                ch2LengthEnabled = (value & 0x40) != 0;
                if ((value & 0x80) != 0) { // Trigger
                    ch2Enabled = true;
                    if (ch2Length == 0) ch2Length = 64;
                    ch2Volume = ch2InitialVolume;
                    ch2EnvelopeTimer = ch2EnvelopePeriod;
                    ch2Timer = (2048 - getCh2Period()) * 4;
                }
                break;
            
            case 0xFF26: // NR52 Sound Control
                masterEnabled = (value & 0x80) != 0;
                if (!masterEnabled) {
                    // Reset all channels when sound is disabled
                    ch1Enabled = false;
                    ch2Enabled = false;
                }
                break;
        }
    }
}
