package se.djax.spelpojken;

/**
 * Handles Game Boy interrupts.
 */
public class Interrupts {

	// Interrupt flags (bits in IF and IE registers)
	public static final int VBLANK = 0;
	public static final int LCD_STAT = 1;
	public static final int TIMER = 2;
	public static final int SERIAL = 3;
	public static final int JOYPAD = 4;

	// Interrupt vectors
	private static final int[] VECTORS = { 0x0040, 0x0048, 0x0050, 0x0058, 0x0060 };

	// Memory addresses
	public static final int IF_REGISTER = 0xFF0F; // Interrupt Flag
	public static final int IE_REGISTER = 0xFFFF; // Interrupt Enable

	private final Cpu cpu;
	
	// Interrupt Master Enable
	private boolean ime = false;
	
	// EI enables interrupts after one instruction delay
	private boolean imeScheduled = false;
	
	// HALT state
	private boolean halted = false;

	public Interrupts(Cpu cpu) {
		this.cpu = cpu;
	}

	/**
	 * Check and handle pending interrupts.
	 * Returns the number of cycles used (0 if no interrupt, 20 if interrupt handled).
	 */
	public int handleInterrupts() {
		// Use getRawMem to check for interrupts without consuming cycles
		int ifReg = cpu.getRawMem(IF_REGISTER);
		int ieReg = cpu.getRawMem(IE_REGISTER);
		
		int pending = ifReg & ieReg & 0x1F;
		
		if (pending != 0) {
			if (halted) {
				halted = false;
			}
		}

		if (ime && pending != 0) {
			for (int i = 0; i < 5; i++) {
				if ((pending & (1 << i)) != 0) {
					ime = false;
					imeScheduled = false;
					
					// Interrupt handling takes 20 cycles
					// We'll perform them in steps
					cpu.tick(8); // Internal preparation
					
					// Clear flag
					ifReg = cpu.getRawMem(IF_REGISTER);
					cpu.setRawMem(IF_REGISTER, (short) (ifReg & ~(1 << i)));
					
					// Push PC (this will tick 8 more cycles via memory bus)
					cpu.push(cpu.pc);
					
					// Jump to vector
					cpu.pc = VECTORS[i];
					
					cpu.tick(4); // Final jump cycle
					
					return 20;
				}
			}
		}
		
		return 0;
	}

	public void requestInterrupt(int interrupt) {
		int ifReg = cpu.getRawMem(IF_REGISTER);
		cpu.setRawMem(IF_REGISTER, (short) (ifReg | (1 << interrupt)));
	}

	public void enableInterrupts() {
		imeScheduled = true;
	}

	public void disableInterrupts() {
		ime = false;
		imeScheduled = false;
	}

	public void enableInterruptsImmediate() {
		ime = true;
		imeScheduled = false;
	}

	public void updateIME() {
		if (imeScheduled) {
			ime = true;
			imeScheduled = false;
		}
	}

	public boolean isHalted() {
		return halted;
	}

	public void setHalted(boolean halted) {
		this.halted = halted;
	}

	public boolean isIme() {
		return ime;
	}
}
