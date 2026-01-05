package se.djax.spelpojken;

/**
 * Handles Game Boy interrupts.
 * 
 * Interrupt vectors:
 * - VBlank:  0x0040
 * - LCD STAT: 0x0048
 * - Timer:   0x0050
 * - Serial:  0x0058
 * - Joypad:  0x0060
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
		int ifReg = cpu.getMem(IF_REGISTER);
		int ieReg = cpu.getMem(IE_REGISTER);
		
		// Check if any enabled interrupt is pending
		int pending = ifReg & ieReg & 0x1F;
		
		if (pending != 0) {
			// Wake from HALT even if IME is disabled
			halted = false;
			
			if (ime) {
				// Handle interrupts in priority order
				for (int i = 0; i < 5; i++) {
					if ((pending & (1 << i)) != 0) {
						// Disable IME
						ime = false;
						
						// Clear the interrupt flag
						cpu.setMem(IF_REGISTER, (short) (ifReg & ~(1 << i)));
						
						// Push PC onto stack
						cpu.push(cpu.pc);
						
						// Jump to interrupt vector
						cpu.pc = VECTORS[i];
						
						return 20; // Interrupt handling takes 20 cycles
					}
				}
			}
		}
		
		return 0;
	}

	/**
	 * Request an interrupt.
	 */
	public void requestInterrupt(int interrupt) {
		int ifReg = cpu.getMem(IF_REGISTER);
		cpu.setMem(IF_REGISTER, (short) (ifReg | (1 << interrupt)));
	}

	/**
	 * Enable interrupts (EI instruction).
	 * Interrupts are enabled after the next instruction.
	 */
	public void enableInterrupts() {
		imeScheduled = true;
	}

	/**
	 * Disable interrupts (DI instruction).
	 */
	public void disableInterrupts() {
		ime = false;
		imeScheduled = false;
	}

	/**
	 * Enable interrupts immediately (RETI instruction).
	 */
	public void enableInterruptsImmediate() {
		ime = true;
	}

	/**
	 * Called after each instruction to handle delayed EI.
	 */
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

	public boolean isIME() {
		return ime;
	}
}
