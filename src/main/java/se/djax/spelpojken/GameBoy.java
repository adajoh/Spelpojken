package se.djax.spelpojken;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GameBoy {

	public static final int MEMORY_SIZE = 0xFFFF + 1;
	public static final int CPU_CLOCK_SPEED = 4194304; // Hz

	private static Logger LOG = Logger.getLogger(GameBoy.class.getName());

	public interface InstructionListener {
		public void onExecution();
	}

	private List<InstructionListener> listeners;
	private Cpu cpu;
	private Opcodes opcodes;
	private Gpu gpu;
	private Interrupts interrupts;
	private Timer timer;
	private Joypad joypad;
	private MBC mbc;
	private File romFile;
	private short[] fullRomData;

	public GameBoy() {
		listeners = new ArrayList<>();
		cpu = new Cpu();
		interrupts = new Interrupts(cpu);
		opcodes = new Opcodes(cpu);
		opcodes.setInterrupts(interrupts);
		gpu = new Gpu(cpu);
		gpu.setInterrupts(interrupts);
		timer = new Timer(cpu, interrupts);
		joypad = new Joypad(cpu, interrupts);
		mbc = new MBC(cpu);
	}

	public void loadRom(File file) {
		romFile = file;
		try {
			byte[] data = Files.readAllBytes(file.toPath());
			fullRomData = new short[data.length];
			for (int i = 0; i < data.length; i++) {
				fullRomData[i] = (short) Byte.toUnsignedInt(data[i]);
			}
			
			// Initialize MBC with ROM data
			mbc.init(fullRomData);
			
			// Load ROM into memory
			cpu.loadRom(romFile, true);
			
			LOG.info("Loaded ROM: " + file.getName() + " (" + data.length + " bytes)");
		} catch (Exception e) {
			throw new RuntimeException("Failed to load ROM", e);
		}
	}

	public Cpu getCpu() {
		return cpu;
	}

	public void step() {
		// Check if halted
		if (interrupts.isHalted()) {
			// Still update timer and GPU while halted
			timer.update(4);
			gpu.exec(4);
			
			// Check for interrupts that can wake from halt
			int cycles = interrupts.handleInterrupts();
			if (cycles > 0) {
				// Woken up by interrupt
				return;
			}
			return;
		}

		// Handle pending interrupts
		int interruptCycles = interrupts.handleInterrupts();
		if (interruptCycles > 0) {
			timer.update(interruptCycles);
			gpu.exec(interruptCycles);
			return;
		}

		// Execute next instruction
		int cycles = opcodes.exec();
		
		// Update IME after instruction (for delayed EI)
		interrupts.updateIME();
		
		// Update timer
		timer.update(cycles);
		
		// Update GPU
		gpu.exec(cycles);

		// Handle special memory writes
		handleMemoryIO();

		// Notify listeners
		for (InstructionListener listener : listeners) {
			listener.onExecution();
		}

		// Disable boot rom when 0xFF50 is written to
		if (cpu.getMem(0xFF50) == 1) {
			cpu.loadRom(romFile, false);
			cpu.setMem(0xFF50, (short) 0);
			LOG.info("Boot rom disabled");
		}
	}

	/**
	 * Handle special memory I/O operations.
	 */
	private void handleMemoryIO() {
		// DIV register reset (any write resets it to 0)
		// This is handled in Memory class or can be checked here
		
		// DMA transfer
		int dma = cpu.getMem(0xFF46);
		if (dma > 0) {
			gpu.doDMATransfer(dma);
			cpu.setMem(0xFF46, (short) 0);
		}
	}

	/**
	 * Write to memory with MBC handling.
	 */
	public void writeMem(int address, short value) {
		// ROM area - MBC control
		if (address < 0x8000) {
			mbc.writeRom(address, value);
			return;
		}
		
		// External RAM
		if (address >= 0xA000 && address < 0xC000) {
			mbc.writeRam(address, value);
			return;
		}
		
		// Joypad register
		if (address == Joypad.JOYPAD_REGISTER) {
			joypad.write(value);
			return;
		}
		
		// DIV register - any write resets it
		if (address == Timer.DIV_REGISTER) {
			timer.resetDIV();
			return;
		}
		
		// TAC register - reset timer counter
		if (address == Timer.TAC_REGISTER) {
			cpu.setMem(address, value);
			timer.resetTimerCounter();
			return;
		}
		
		// Normal memory write
		cpu.setMem(address, value);
	}

	/**
	 * Read from memory with MBC handling.
	 */
	public short readMem(int address) {
		// External RAM
		if (address >= 0xA000 && address < 0xC000) {
			return mbc.readRam(address);
		}
		
		// Joypad register
		if (address == Joypad.JOYPAD_REGISTER) {
			return joypad.read();
		}
		
		// Normal memory read
		return cpu.getMem(address);
	}

	public void addListener(InstructionListener listener) {
		listeners.add(listener);
	}

	public Opcodes getOpcodes() {
		return opcodes;
	}

	public Gpu getGpu() {
		return gpu;
	}

	public Interrupts getInterrupts() {
		return interrupts;
	}

	public Timer getTimer() {
		return timer;
	}

	public Joypad getJoypad() {
		return joypad;
	}

	public MBC getMBC() {
		return mbc;
	}

	/**
	 * Press a button.
	 */
	public void pressButton(Joypad.Button button) {
		joypad.pressButton(button);
	}

	/**
	 * Release a button.
	 */
	public void releaseButton(Joypad.Button button) {
		joypad.releaseButton(button);
	}

	/**
	 * Run for approximately one frame (~70224 cycles).
	 */
	public void runFrame() {
		int targetCycles = 70224;
		int cyclesRun = 0;
		
		while (cyclesRun < targetCycles) {
			step();
			cyclesRun += 4; // Approximate
		}
	}
}
