package se.djax.spelpojken;

/**
 * Game Boy Timer implementation with falling-edge detection.
 */
public class Timer {

	public static final int DIV_REGISTER = 0xFF04;
	public static final int TIMA_REGISTER = 0xFF05;
	public static final int TMA_REGISTER = 0xFF06;
	public static final int TAC_REGISTER = 0xFF07;

	private final Cpu cpu;
	private final Interrupts interrupts;

	private int internalCounter = 0;
	private boolean previousEdge = false;

	public Timer(Cpu cpu, Interrupts interrupts) {
		this.cpu = cpu;
		this.interrupts = interrupts;
	}

	/**
	 * Update timer with the given number of CPU cycles.
	 */
	public void update(int cycles) {
		for (int i = 0; i < cycles; i++) {
			internalCounter = (internalCounter + 1) & 0xFFFF;
			
			// DIV is top 8 bits of the internal 16-bit counter's lower 14 bits?
			// Actually DIV is just bits 8-15 of the 16-bit counter.
			cpu.setRawMem(DIV_REGISTER, (short) (internalCounter >> 8));
			
			updateFallingEdge();
		}
	}

	private void updateFallingEdge() {
		int tac = cpu.getRawMem(TAC_REGISTER);
		boolean enabled = (tac & 0x04) != 0;
		int mode = tac & 0x03;
		
		int bitMask;
		switch (mode) {
			case 0: bitMask = 1 << 9; break;  // 4096 Hz
			case 1: bitMask = 1 << 3; break;  // 262144 Hz
			case 2: bitMask = 1 << 5; break;  // 65536 Hz
			case 3: bitMask = 1 << 7; break;  // 16384 Hz
			default: bitMask = 1 << 9; break;
		}
		
		boolean currentEdge = enabled && (internalCounter & bitMask) != 0;
		
		if (previousEdge && !currentEdge) {
			// Falling edge detected - increment TIMA
			incrementTIMA();
		}
		previousEdge = currentEdge;
	}

	private void incrementTIMA() {
		int tima = cpu.getRawMem(TIMA_REGISTER);
		tima = (tima + 1) & 0xFF;
		
		if (tima == 0) {
			// Overflow! Load TMA into TIMA and request interrupt
			tima = cpu.getRawMem(TMA_REGISTER);
			interrupts.requestInterrupt(Interrupts.TIMER);
		}
		
		cpu.setRawMem(TIMA_REGISTER, (short) tima);
	}

	/**
	 * Reset DIV register (writing any value resets it to 0).
	 */
	public void resetDIV() {
		internalCounter = 0;
		cpu.setRawMem(DIV_REGISTER, (short) 0);
		updateFallingEdge();
	}

	/**
	 * Called when TAC register is written to.
	 */
	public void onTACWrite() {
		updateFallingEdge();
	}
}
