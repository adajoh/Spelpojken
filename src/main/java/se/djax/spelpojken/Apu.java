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
    private int nr50 = 0; // Master volume & VIN
    private int nr51 = 0xFF; // Panning
    
    // Channel 1 (Square with Sweep)
    private boolean ch1Enabled = false;
    private int ch1Timer = 0;
    private int ch1Duty = 0;
    private int ch1DutyStep = 0;
    private int ch1Length = 0;
    private boolean ch1LengthEnabled = false;
    private int ch1Volume = 0;
    private int ch1InitialVolume = 0;
    private boolean ch1EnvelopeDir = false; 
    private int ch1EnvelopePeriod = 0;
    private int ch1EnvelopeTimer = 0;
    
    // Channel 2 (Square)
    private boolean ch2Enabled = false;
    private int ch2Timer = 0;
    private int ch2Duty = 0;
    private int ch2DutyStep = 0;
    private int ch2Length = 0;
    private boolean ch2LengthEnabled = false;
    private int ch2Volume = 0;
    private int ch2InitialVolume = 0;
    private boolean ch2EnvelopeDir = false;
    private int ch2EnvelopePeriod = 0;
    private int ch2EnvelopeTimer = 0;

    // Channel 3 (Wave)
    private boolean ch3Enabled = false;
    private boolean ch3DACEnabled = false;
    private int ch3Timer = 0;
    private int ch3Position = 0; // 0-31
    private int ch3Length = 0;
    private boolean ch3LengthEnabled = false;
    private int ch3VolumeShift = 0; // 0=0%, 1=100%, 2=50%, 3=25%
    private final short[] waveRam = new short[16];

    // Channel 4 (Noise)
    private boolean ch4Enabled = false;
    private int ch4Timer = 0;
    private int ch4Length = 0;
    private boolean ch4LengthEnabled = false;
    private int ch4Volume = 0;
    private int ch4InitialVolume = 0;
    private boolean ch4EnvelopeDir = false;
    private int ch4EnvelopePeriod = 0;
    private int ch4EnvelopeTimer = 0;
    private int ch4LSFR = 0x7FFF;
    private boolean ch4LSFR7Bit = false;
    private int ch4ShiftClock = 0;
    private int ch4DivisorCode = 0;
    
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
        }
    }

    public void exec(int cycles) {
        if (!masterEnabled) return;

        updateFrameSequencer(cycles);
        
        // Update Channel 1
        if (ch1Enabled) {
            ch1Timer -= cycles;
            if (ch1Timer <= 0) {
                ch1Timer = (2048 - getCh1Period()) * 4;
                ch1DutyStep = (ch1DutyStep + 1) % 8;
            }
        }

        // Update Channel 2
        if (ch2Enabled) {
            ch2Timer -= cycles;
            if (ch2Timer <= 0) {
                ch2Timer = (2048 - getCh2Period()) * 4;
                ch2DutyStep = (ch2DutyStep + 1) % 8;
            }
        }

        // Update Channel 3
        if (ch3Enabled && ch3DACEnabled) {
            ch3Timer -= cycles;
            if (ch3Timer <= 0) {
                ch3Timer = (2048 - getCh3Period()) * 2;
                ch3Position = (ch3Position + 1) % 32;
            }
        }

        // Update Channel 4
        if (ch4Enabled) {
            ch4Timer -= cycles;
            if (ch4Timer <= 0) {
                ch4Timer = getCh4Frequency();
                
                int result = (ch4LSFR & 0x1) ^ ((ch4LSFR >> 1) & 0x1);
                ch4LSFR >>= 1;
                ch4LSFR |= (result << 14);
                if (ch4LSFR7Bit) {
                    ch4LSFR &= ~0x40;
                    ch4LSFR |= (result << 6);
                }
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
            if (--ch1Length == 0) ch1Enabled = false;
        }
        if (ch2LengthEnabled && ch2Length > 0) {
            if (--ch2Length == 0) ch2Enabled = false;
        }
        if (ch3LengthEnabled && ch3Length > 0) {
            if (--ch3Length == 0) ch3Enabled = false;
        }
        if (ch4LengthEnabled && ch4Length > 0) {
            if (--ch4Length == 0) ch4Enabled = false;
        }
    }

    private void updateEnvelopes() {
        if (ch1EnvelopePeriod > 0) {
            if (--ch1EnvelopeTimer <= 0) {
                ch1EnvelopeTimer = ch1EnvelopePeriod;
                if (ch1EnvelopeDir && ch1Volume < 15) ch1Volume++;
                else if (!ch1EnvelopeDir && ch1Volume > 0) ch1Volume--;
            }
        }
        if (ch2EnvelopePeriod > 0) {
            if (--ch2EnvelopeTimer <= 0) {
                ch2EnvelopeTimer = ch2EnvelopePeriod;
                if (ch2EnvelopeDir && ch2Volume < 15) ch2Volume++;
                else if (!ch2EnvelopeDir && ch2Volume > 0) ch2Volume--;
            }
        }
        if (ch4EnvelopePeriod > 0) {
            if (--ch4EnvelopeTimer <= 0) {
                ch4EnvelopeTimer = ch4EnvelopePeriod;
                if (ch4EnvelopeDir && ch4Volume < 15) ch4Volume++;
                else if (!ch4EnvelopeDir && ch4Volume > 0) ch4Volume--;
            }
        }
    }

    private int sampleClock = 0;
    private void generateSample(int cycles) {
        sampleClock += cycles;
        int cyclesPerSample = GameBoy.CPU_CLOCK_SPEED / SAMPLE_RATE;
        
        while (sampleClock >= cyclesPerSample) {
            sampleClock -= cyclesPerSample;
            
            float s1 = ch1Enabled ? (DUTY_PATTERNS[ch1Duty][ch1DutyStep] * 2 - 1) * (ch1Volume / 15.0f) : 0;
            float s2 = ch2Enabled ? (DUTY_PATTERNS[ch2Duty][ch2DutyStep] * 2 - 1) * (ch2Volume / 15.0f) : 0;
            
            float s3 = 0;
            if (ch3Enabled && ch3DACEnabled && ch3VolumeShift > 0) {
                int waveByte = waveRam[ch3Position / 2];
                int nibble = (ch3Position % 2 == 0) ? (waveByte >> 4) : (waveByte & 0x0F);
                s3 = (nibble >> (ch3VolumeShift - 1)) / 7.5f - 1.0f;
            }

            float s4 = ch4Enabled ? ((ch4LSFR & 0x1) * 2 - 1) * (ch4Volume / 15.0f) : 0;

            float left = 0, right = 0;
            if ((nr51 & 0x10) != 0) left += s1;
            if ((nr51 & 0x01) != 0) right += s1;
            if ((nr51 & 0x20) != 0) left += s2;
            if ((nr51 & 0x02) != 0) right += s2;
            if ((nr51 & 0x40) != 0) left += s3;
            if ((nr51 & 0x04) != 0) right += s3;
            if ((nr51 & 0x80) != 0) left += s4;
            if ((nr51 & 0x08) != 0) right += s4;

            float volL = ((nr50 >> 4) & 0x07) / 7.0f;
            float volR = (nr50 & 0x07) / 7.0f;

            buffer[bufferPtr++] = left * 0.25f * volL;
            buffer[bufferPtr++] = right * 0.25f * volR;
            
			if (bufferPtr >= BUFFER_SIZE) {
                if (audioDevice != null) {
                    audioDevice.writeSamples(buffer, 0, BUFFER_SIZE);
                }
                bufferPtr = 0;
            }
        }
    }

    public void sync() {
        if (audioDevice != null && bufferPtr > 0) {
            // Write remaining samples in buffer
            float[] remaining = new float[bufferPtr];
            System.arraycopy(buffer, 0, remaining, 0, bufferPtr);
            audioDevice.writeSamples(remaining, 0, bufferPtr);
            bufferPtr = 0;
        }
    }

    private int getCh1Period() {
        return (cpu.getRawMem(0xFF14) & 0x07) << 8 | cpu.getRawMem(0xFF13);
    }

    private int getCh2Period() {
        return (cpu.getRawMem(0xFF19) & 0x07) << 8 | cpu.getRawMem(0xFF18);
    }

    private int getCh3Period() {
        return (cpu.getRawMem(0xFF1E) & 0x07) << 8 | cpu.getRawMem(0xFF1D);
    }

    private int getCh4Frequency() {
        int divisor = (ch4DivisorCode == 0) ? 8 : ch4DivisorCode * 16;
        return divisor << ch4ShiftClock;
    }

    public void writeRegister(int address, short value) {
        if (!masterEnabled && address != 0xFF26 && (address < 0xFF30 || address > 0xFF3F)) return;

        if (address >= 0xFF30 && address <= 0xFF3F) {
            waveRam[address - 0xFF30] = value;
            return;
        }

        switch (address) {
            case 0xFF11: ch1Duty = (value >> 6) & 0x03; ch1Length = 64 - (value & 0x3F); break;
            case 0xFF12: ch1InitialVolume = (value >> 4) & 0x0F; ch1EnvelopeDir = (value & 0x08) != 0; ch1EnvelopePeriod = value & 0x07; break;
            case 0xFF14:
                ch1LengthEnabled = (value & 0x40) != 0;
                if ((value & 0x80) != 0) {
                    ch1Enabled = true;
                    if (ch1Length == 0) ch1Length = 64;
                    ch1Volume = ch1InitialVolume;
                    ch1EnvelopeTimer = ch1EnvelopePeriod;
                }
                break;
            case 0xFF16: ch2Duty = (value >> 6) & 0x03; ch2Length = 64 - (value & 0x3F); break;
            case 0xFF17: ch2InitialVolume = (value >> 4) & 0x0F; ch2EnvelopeDir = (value & 0x08) != 0; ch2EnvelopePeriod = value & 0x07; break;
            case 0xFF19:
                ch2LengthEnabled = (value & 0x40) != 0;
                if ((value & 0x80) != 0) {
                    ch2Enabled = true;
                    if (ch2Length == 0) ch2Length = 64;
                    ch2Volume = ch2InitialVolume;
                    ch2EnvelopeTimer = ch2EnvelopePeriod;
                }
                break;
            case 0xFF1A: ch3DACEnabled = (value & 0x80) != 0; if (!ch3DACEnabled) ch3Enabled = false; break;
            case 0xFF1B: ch3Length = 256 - value; break;
            case 0xFF1C: ch3VolumeShift = (value >> 5) & 0x03; break;
            case 0xFF1E:
                ch3LengthEnabled = (value & 0x40) != 0;
                if ((value & 0x80) != 0) {
                    ch3Enabled = true;
                    if (ch3Length == 0) ch3Length = 256;
                    ch3Position = 0;
                    ch3Timer = (2048 - getCh3Period()) * 2;
                }
                break;
            case 0xFF20: ch4Length = 64 - (value & 0x3F); break;
            case 0xFF21: ch4InitialVolume = (value >> 4) & 0x0F; ch4EnvelopeDir = (value & 0x08) != 0; ch4EnvelopePeriod = value & 0x07; break;
            case 0xFF22: ch4ShiftClock = (value >> 4) & 0x0F; ch4LSFR7Bit = (value & 0x08) != 0; ch4DivisorCode = value & 0x07; break;
            case 0xFF23:
                ch4LengthEnabled = (value & 0x40) != 0;
                if ((value & 0x80) != 0) {
                    ch4Enabled = true;
                    if (ch4Length == 0) ch4Length = 64;
                    ch4Volume = ch4InitialVolume;
                    ch4EnvelopeTimer = ch4EnvelopePeriod;
                    ch4LSFR = 0x7FFF;
                }
                break;
            case 0xFF24: nr50 = value; break;
            case 0xFF25: nr51 = value; break;
            case 0xFF26: 
                masterEnabled = (value & 0x80) != 0;
                if (!masterEnabled) {
                    ch1Enabled = ch2Enabled = ch3Enabled = ch4Enabled = false;
                }
                break;
        }
    }
}
