package se.djax.spelpojken;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GameBoy {

	public static final int MEMORY_SIZE = 0xFFFF + 1;

	private static Logger LOG = Logger.getLogger(GameBoy.class.getName());

	public interface InstructionListener {
		public void onExecution();
	}

	private List<InstructionListener> listeners;
	private Cpu cpu;
	private Opcodes opcodes;
	private Gpu gpu;
	/// private Interrupts interrupts;
	private File romFile;

	public GameBoy() {
		listeners = new ArrayList<>();
		cpu = new Cpu();
		opcodes = new Opcodes(cpu);
		gpu = new Gpu(cpu);
		// interrupts = new Interrupts();
	}

	public void loadRom(File file) {
		romFile = file;
		cpu.loadRom(romFile, true);
	}

	public Cpu getCpu() {
		return cpu;
	}

	public void step() {
		int cycles = opcodes.exec();
		gpu.exec(cycles);

		// Notify listeners
		for (InstructionListener listener : listeners) {
			listener.onExecution();
		}

		/*
		 * 
		 * Special stuff
		 * 
		 */

		// Disable boot rom
		if (cpu.getMem(0xFF50) == 1) {
			cpu.loadRom(romFile, false);
			cpu.setMem(0xFF50, (short) 0);
			LOG.info("Boot rom disabled");
		}
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

}
