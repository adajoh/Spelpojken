package se.djax.spelpojken;

/**
 * Joypad input handling.
 * 
 * Register 0xFF00 (P1/JOYP):
 * Bit 5 - Select button keys (0=Select)
 * Bit 4 - Select direction keys (0=Select)
 * Bit 3 - Down or Start (0=Pressed)
 * Bit 2 - Up or Select (0=Pressed)
 * Bit 1 - Left or B (0=Pressed)
 * Bit 0 - Right or A (0=Pressed)
 */
public class Joypad {

	public static final int JOYPAD_REGISTER = 0xFF00;

	// Button states (true = pressed)
	private boolean buttonA = false;
	private boolean buttonB = false;
	private boolean buttonSelect = false;
	private boolean buttonStart = false;
	private boolean dpadUp = false;
	private boolean dpadDown = false;
	private boolean dpadLeft = false;
	private boolean dpadRight = false;

	private final Cpu cpu;
	private final Interrupts interrupts;

	public Joypad(Cpu cpu, Interrupts interrupts) {
		this.cpu = cpu;
		this.interrupts = interrupts;
	}

	/**
	 * Read joypad register.
	 */
	public short read() {
		int joyp = cpu.getRawMem(JOYPAD_REGISTER);
		int value = 0xCF | (joyp & 0x30); // Bits 6-7 always 1, bits 4-5 from register, bits 0-3 default 1
		
		// Check which buttons are selected (0 = selected)
		boolean selectButtons = (joyp & 0x20) == 0;
		boolean selectDpad = (joyp & 0x10) == 0;
		
		if (selectButtons) {
			if (buttonA) value &= ~0x01;
			if (buttonB) value &= ~0x02;
			if (buttonSelect) value &= ~0x04;
			if (buttonStart) value &= ~0x08;
		}
		
		if (selectDpad) {
			if (dpadRight) value &= ~0x01;
			if (dpadLeft) value &= ~0x02;
			if (dpadUp) value &= ~0x04;
			if (dpadDown) value &= ~0x08;
		}
		
		return (short) value;
	}

	/**
	 * Write to joypad register (only bits 4-5 are writable).
	 */
	public void write(short value) {
		cpu.setRawMem(JOYPAD_REGISTER, (short) (value & 0x30));
	}

	/**
	 * Press a button.
	 */
	public void pressButton(Button button) {
		setButton(button, true);
		interrupts.requestInterrupt(Interrupts.JOYPAD);
	}

	/**
	 * Release a button.
	 */
	public void releaseButton(Button button) {
		setButton(button, false);
	}

	private void setButton(Button button, boolean pressed) {
		switch (button) {
			case A: buttonA = pressed; break;
			case B: buttonB = pressed; break;
			case SELECT: buttonSelect = pressed; break;
			case START: buttonStart = pressed; break;
			case UP: dpadUp = pressed; break;
			case DOWN: dpadDown = pressed; break;
			case LEFT: dpadLeft = pressed; break;
			case RIGHT: dpadRight = pressed; break;
		}
	}

	public enum Button {
		A, B, SELECT, START, UP, DOWN, LEFT, RIGHT
	}
}
