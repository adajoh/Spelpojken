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
	private Apu apu;
	private MBC mbc;
	private short[] fullRomData;
	private byte[] rawRomData;
	private int cyclesTicked = 0;

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
		apu = new Apu(cpu);
		mbc = new MBC(cpu);

		cpu.setMemoryBus(new Cpu.MemoryBus() {
			@Override
			public short read(int address) {
				tick(4);
				return readMem(address);
			}

			@Override
			public void write(int address, short value) {
				tick(4);
				writeMem(address, value);
			}

			@Override
			public void tick(int cycles) {
				GameBoy.this.tick(cycles);
			}
		});
	}

	public void tick(int cycles) {
		timer.update(cycles);
		gpu.exec(cycles);
		apu.exec(cycles);
		cyclesTicked += cycles;
	}

	public void loadRom(byte[] data) {
		try {
			this.rawRomData = data;
			fullRomData = new short[data.length];
			for (int i = 0; i < data.length; i++) {
				fullRomData[i] = (short) Byte.toUnsignedInt(data[i]);
			}
			
			// Initialize MBC with ROM data
			mbc.init(fullRomData);
			
			// Load ROM into memory
			byte[] bootData = null;
			var bootRes = Cpu.class.getResourceAsStream("/DMG_ROM.gb");
			if (bootRes != null) {
				bootData = bootRes.readAllBytes();
			}
			cpu.loadRom(data, bootData);
			
			if (bootData == null) {
				// Set initial state if no boot ROM is used
				cpu.pc = 0x0100;
				cpu.sp = 0xFFFE;
				cpu.a = 0x01;
				cpu.b = 0x00;
				cpu.c = 0x13;
				cpu.d = 0x00;
				cpu.e = 0xD8;
				cpu.h = 0x01;
				cpu.l = 0x4D;
				cpu.toggleFlag(Cpu.FLAG_ZERO, true);
				cpu.toggleFlag(Cpu.FLAG_SUBTRACT, false);
				cpu.toggleFlag(Cpu.FLAG_HALF_CARRY, true);
				cpu.toggleFlag(Cpu.FLAG_CARRY, true);
				
				// Set some default I/O register values
				cpu.setRawMem(0xFF05, (short) 0x00); // TIMA
				cpu.setRawMem(0xFF06, (short) 0x00); // TMA
				cpu.setRawMem(0xFF07, (short) 0x00); // TAC
				cpu.setRawMem(0xFF10, (short) 0x80); // NR10
				cpu.setRawMem(0xFF11, (short) 0xBF); // NR11
				cpu.setRawMem(0xFF12, (short) 0xF3); // NR12
				cpu.setRawMem(0xFF14, (short) 0xBF); // NR14
				cpu.setRawMem(0xFF16, (short) 0x3F); // NR16
				cpu.setRawMem(0xFF17, (short) 0x00); // NR17
				cpu.setRawMem(0xFF19, (short) 0xBF); // NR19
				cpu.setRawMem(0xFF1A, (short) 0x7F); // NR1A
				cpu.setRawMem(0xFF1B, (short) 0xFF); // NR1B
				cpu.setRawMem(0xFF1C, (short) 0x9F); // NR1C
				cpu.setRawMem(0xFF1E, (short) 0xBF); // NR1E
				cpu.setRawMem(0xFF20, (short) 0xFF); // NR20
				cpu.setRawMem(0xFF21, (short) 0x00); // NR21
				cpu.setRawMem(0xFF22, (short) 0x00); // NR22
				cpu.setRawMem(0xFF23, (short) 0xBF); // NR23
				cpu.setRawMem(0xFF24, (short) 0x77); // NR24
				cpu.setRawMem(0xFF25, (short) 0xF3); // NR25
				cpu.setRawMem(0xFF26, (short) 0xF1); // NR26
				cpu.setRawMem(0xFF40, (short) 0x91); // LCDC
				cpu.setRawMem(0xFF42, (short) 0x00); // SCY
				cpu.setRawMem(0xFF43, (short) 0x00); // SCX
				cpu.setRawMem(0xFF45, (short) 0x00); // LYC
				cpu.setRawMem(0xFF47, (short) 0xE4); // BGP
				cpu.setRawMem(0xFF48, (short) 0xFF); // OBP0
				cpu.setRawMem(0xFF49, (short) 0xFF); // OBP1
				cpu.setRawMem(0xFF4A, (short) 0x00); // WY
				cpu.setRawMem(0xFF4B, (short) 0x00); // WX
				cpu.setRawMem(0xFFFF, (short) 0x00); // IE
			}
			
			LOG.info("Loaded ROM data (" + data.length + " bytes)");
		} catch (Exception e) {
			throw new RuntimeException("Failed to load ROM", e);
		}
	}

	public void loadRom(File file) {
		try {
			byte[] data = Files.readAllBytes(file.toPath());
			loadRom(data);
			LOG.info("Loaded ROM: " + file.getName() + " (" + data.length + " bytes)");
		} catch (Exception e) {
			throw new RuntimeException("Failed to load ROM", e);
		}
	}

	public Cpu getCpu() {
		return cpu;
	}

	public int step() {
		// Check if halted
		if (interrupts.isHalted()) {
			// Still update timer and GPU while halted
			tick(4);
			
			// Check for interrupts that can wake from halt
			int cycles = interrupts.handleInterrupts();
			if (cycles > 0) {
				return cycles;
			}
			return 4;
		}

		// Handle pending interrupts
		int interruptCycles = interrupts.handleInterrupts();
		if (interruptCycles > 0) {
			return interruptCycles;
		}

		// Update IME after previous instruction (for delayed EI)
		interrupts.updateIME();

		// Execute next instruction
		cyclesTicked = 0;
		int totalCycles = opcodes.exec();
		
		// Fill remaining cycles if instruction didn't tick enough
		int remaining = totalCycles - cyclesTicked;
		if (remaining > 0) {
			tick(remaining);
		}

		// Notify listeners
		for (InstructionListener listener : listeners) {
			listener.onExecution();
		}

		// Disable boot rom when 0xFF50 is written to
		if (cpu.getMem(0xFF50) == 1) {
			cpu.loadRom(rawRomData, null);
			cpu.setMem(0xFF50, (short) 0);
			LOG.info("Boot rom disabled");
		}
		
		return totalCycles;
	}

	/**
	 * Write to memory with MBC handling.
	 */
	public void writeMem(int address, short value) {
		address &= 0xFFFF;
		
		// DMA transfer
		if (address == 0xFF46) {
			cpu.setRawMem(address, value);
			gpu.doDMATransfer(value);
			return;
		}

		// ROM area - MBC control
		if (address < 0x8000) {
			mbc.writeRom(address, value);
			return;
		}

		// VRAM
		if (address >= 0x8000 && address < 0xA000) {
			cpu.setRawMem(address, value);
			return;
		}
		
		// External RAM
		if (address >= 0xA000 && address < 0xC000) {
			mbc.writeRam(address, value);
			return;
		}
		
		// WRAM
		if (address >= 0xC000 && address < 0xE000) {
			cpu.setRawMem(address, value);
			return;
		}

		// Echo RAM (0xE000-0xFDFF mirrors 0xC000-0xDDFF)
		if (address >= 0xE000 && address < 0xFE00) {
			cpu.setRawMem(address - 0x2000, value);
			return;
		}

		// STAT register
		if (address == 0xFF41) {
			short currentStat = cpu.getRawMem(0xFF41);
			cpu.setRawMem(address, (short) (0x80 | (value & 0x78) | (currentStat & 0x07)));
			return;
		}

		// Joypad register
		if (address == Joypad.JOYPAD_REGISTER) {
			joypad.write(value);
			return;
		}

		// Serial port (Blargg's test output)
		if (address == 0xFF01) {
			cpu.setRawMem(address, value);
			return;
		}
		if (address == 0xFF02 && value == 0x81) {
			System.out.print((char) cpu.getRawMem(0xFF01));
			cpu.setRawMem(0xFF02, (short) 0x01);
			return;
		}
		
		// DIV register - any write resets it
		if (address == Timer.DIV_REGISTER) {
			timer.resetDIV();
			return;
		}
		
		// TAC register - handle potential falling edge
		if (address == Timer.TAC_REGISTER) {
			cpu.setRawMem(address, value);
			timer.onTACWrite();
			return;
		}
		
		// NR10 - NR52 (APU)
		if (address >= 0xFF10 && address <= 0xFF3F) {
			cpu.setRawMem(address, value);
			apu.writeRegister(address, value);
			return;
		}

		// Normal memory write
		cpu.setRawMem(address, value);
	}

	/**
	 * Read from memory with MBC handling.
	 */
	public short readMem(int address) {
		address &= 0xFFFF;

		// External RAM
		if (address >= 0xA000 && address < 0xC000) {
			return mbc.readRam(address);
		}

		// Echo RAM
		if (address >= 0xE000 && address < 0xFE00) {
			return cpu.getRawMem(address - 0x2000);
		}
		
		// Joypad register
		if (address == Joypad.JOYPAD_REGISTER) {
			return joypad.read();
		}
		
		// Normal memory read
		return cpu.getRawMem(address);
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

	public Apu getApu() {
		return apu;
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
			cyclesRun += step();
		}
		
		// Wait for audio device to catch up if it's falling behind
		apu.sync();
	}
}
