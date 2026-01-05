package se.djax.spelpojken;

/**
 * Game Boy Timer implementation.
 * 
 * Registers:
 * - DIV  (0xFF04): Divider register, incremented at 16384 Hz
 * - TIMA (0xFF05): Timer counter
 * - TMA  (0xFF06): Timer modulo (value loaded when TIMA overflows)
 * - TAC  (0xFF07): Timer control
 */
public class Timer {

	public static final int DIV_REGISTER = 0xFF04;
	public static final int TIMA_REGISTER = 0xFF05;
	public static final int TMA_REGISTER = 0xFF06;
	public static final int TAC_REGISTER = 0xFF07;

	// CPU runs at 4194304 Hz
	// DIV increments at 16384 Hz = every 256 cycles
	private static final int DIV_CYCLES = 256;

	// Timer frequencies based on TAC bits 0-1
	// 00: 4096 Hz   = every 1024 cycles
	// 01: 262144 Hz = every 16 cycles
	// 10: 65536 Hz  = every 64 cycles
	// 11: 16384 Hz  = every 256 cycles
	private static final int[] TIMER_CYCLES = { 1024, 16, 64, 256 };

	private final Cpu cpu;
	private final Interrupts interrupts;

	private int divCounter = 0;
	private int timerCounter = 0;

	public Timer(Cpu cpu, Interrupts interrupts) {
		this.cpu = cpu;
		this.interrupts = interrupts;
	}

	/**
	 * Update timer with the given number of CPU cycles.
	 */
	public void update(int cycles) {
		updateDIV(cycles);
		updateTIMA(cycles);
	}

	private void updateDIV(int cycles) {
		divCounter += cycles;
		
		while (divCounter >= DIV_CYCLES) {
			divCounter -= DIV_CYCLES;
			
			// Increment DIV, wrapping at 0xFF
			int div = cpu.getMem(DIV_REGISTER);
			div = (div + 1) & 0xFF;
			
			// Write directly to avoid reset behavior
			cpu.rom[DIV_REGISTER] = (short) div;
		}
	}

	private void updateTIMA(int cycles) {
		int tac = cpu.getMem(TAC_REGISTER);
		
		// Check if timer is enabled (bit 2)
		if ((tac & 0x04) == 0) {
			return;
		}

		timerCounter += cycles;
		
		int frequency = TIMER_CYCLES[tac & 0x03];
		
		while (timerCounter >= frequency) {
			timerCounter -= frequency;
			
			int tima = cpu.getMem(TIMA_REGISTER);
			tima++;
			
			// Check for overflow
			if (tima > 0xFF) {
				// Load TMA into TIMA
				tima = cpu.getMem(TMA_REGISTER);
				
				// Request timer interrupt
				interrupts.requestInterrupt(Interrupts.TIMER);
			}
			
			cpu.setMem(TIMA_REGISTER, (short) (tima & 0xFF));
		}
	}

	/**
	 * Reset DIV register (writing any value resets it to 0).
	 */
	public void resetDIV() {
		cpu.rom[DIV_REGISTER] = 0;
		divCounter = 0;
	}

	/**
	 * Reset timer counter (called when TAC is written).
	 */
	public void resetTimerCounter() {
		timerCounter = 0;
	}
}
