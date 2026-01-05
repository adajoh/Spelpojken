package se.djax.spelpojken;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;

import org.junit.Assert;

public class Cpu {

	private static Logger LOG = Logger.getLogger(Cpu.class.getName());

	public static final short FLAG_ZERO = 7; // Z
	public static final int FLAG_SUBTRACT = 6; // N
	public static final int FLAG_HALF_CARRY = 5; // H
	public static final int FLAG_CARRY = 4; // C

	public final short[] rom;

	public short a;
	public short b;
	public short c;
	public short d;
	public short e;
	private short f;
	public short h;
	public short l;

	public int pc = 0x0000;
	public int sp = 0xFFFE;

	public Cpu() {
		rom = new short[GameBoy.MEMORY_SIZE];
	}

	public void loadRom(byte[] data, byte[] bootData) {
		try {
			// init
			for (int i = 0; i < rom.length; i++) {
				// rom[i] = -1;
			}

			// load rom
			for (int i = 0; i < data.length && i < rom.length; i++) {
				rom[i] = (short) Byte.toUnsignedInt(data[i]);
			}

			if (bootData != null) {
				for (int i = 0; i < 256 && i < bootData.length; i++) {
					rom[i] = (short) Byte.toUnsignedInt(bootData[i]);
				}
				LOG.info("Loaded boot rom");
			}

			LOG.info("Loaded rom size:" + data.length + " bytes");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void loadRom(File file, boolean withBootRom) {
		try {
			Assert.assertTrue(file.exists());
			byte[] data = Files.readAllBytes(file.toPath());
			byte[] bootData = null;

			if (withBootRom) {
				var bootRes = Cpu.class.getResourceAsStream("/DMG_ROM.gb");
				if (bootRes != null) {
					bootData = bootRes.readAllBytes();
				}
			}
			loadRom(data, bootData);
			LOG.info("Loaded rom:" + file.getAbsolutePath() + " size:" + data.length + " bytes");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void init() {

	}

	public int get16bitValue(int startAddress) {
		return get16bitValue(getMem(startAddress + 1), getMem(startAddress));
	}

	public int get16bitValue(short high, short low) {
		return high << 8 & 0xFF00 | low & 0xFF;
	}

	public void inc(String register) {
		short i = getRegister(register);

		toogleFlag(FLAG_HALF_CARRY, (i & 0x0F) == 0x0F);

		i = (short) ((i + 1) & 0xFF);

		toogleFlag(FLAG_ZERO, i == 0);
		toogleFlag(FLAG_SUBTRACT, false);

		setRegister(register, i);
	}

	public void dec(String register) {
		short i = getRegister(register);

		toogleFlag(FLAG_HALF_CARRY, (i & 0x0F) == 0);

		i = (short) ((i - 1) & 0xFF);

		toogleFlag(FLAG_ZERO, i == 0);
		toogleFlag(FLAG_SUBTRACT, true);

		setRegister(register, i);
	}

	public void subN(short val) {
		short a = getRegister("A");
		int result = a - val;
		
		toogleFlag(FLAG_HALF_CARRY, (a & 0x0F) < (val & 0x0F));
		toogleFlag(FLAG_CARRY, result < 0);
		
		result &= 0xFF;

		toogleFlag(FLAG_ZERO, result == 0);
		toogleFlag(FLAG_SUBTRACT, true);

		setRegister("A", (short) result);
	}

	/**
	 * Add with carry: A = A + n + carry
	 */
	public void adcN(short val) {
		short a = getRegister("A");
		int carry = getFlag(FLAG_CARRY) ? 1 : 0;
		int result = a + val + carry;
		
		toogleFlag(FLAG_HALF_CARRY, ((a & 0x0F) + (val & 0x0F) + carry) > 0x0F);
		toogleFlag(FLAG_CARRY, result > 0xFF);
		
		result &= 0xFF;
		
		toogleFlag(FLAG_ZERO, result == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		
		setRegister("A", (short) result);
	}

	/**
	 * Subtract with carry: A = A - n - carry
	 */
	public void sbcN(short val) {
		short a = getRegister("A");
		int carry = getFlag(FLAG_CARRY) ? 1 : 0;
		int result = a - val - carry;
		
		toogleFlag(FLAG_HALF_CARRY, ((a & 0x0F) - (val & 0x0F) - carry) < 0);
		toogleFlag(FLAG_CARRY, result < 0);
		
		result &= 0xFF;
		
		toogleFlag(FLAG_ZERO, result == 0);
		toogleFlag(FLAG_SUBTRACT, true);
		
		setRegister("A", (short) result);
	}

	/**
	 * Decimal Adjust Accumulator (DAA)
	 * Adjusts the result of a binary addition/subtraction to BCD
	 */
	public void daa() {
		int a = getRegister("A");
		
		if (!getFlag(FLAG_SUBTRACT)) {
			// After addition
			if (getFlag(FLAG_CARRY) || a > 0x99) {
				a += 0x60;
				toogleFlag(FLAG_CARRY, true);
			}
			if (getFlag(FLAG_HALF_CARRY) || (a & 0x0F) > 0x09) {
				a += 0x06;
			}
		} else {
			// After subtraction
			if (getFlag(FLAG_CARRY)) {
				a -= 0x60;
			}
			if (getFlag(FLAG_HALF_CARRY)) {
				a -= 0x06;
			}
		}
		
		a &= 0xFF;
		toogleFlag(FLAG_ZERO, a == 0);
		toogleFlag(FLAG_HALF_CARRY, false);
		
		setRegister("A", (short) a);
	}

	/**
	 * Set Carry Flag
	 */
	public void scf() {
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, false);
		toogleFlag(FLAG_CARRY, true);
	}

	/**
	 * Complement Carry Flag
	 */
	public void ccf() {
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, false);
		toogleFlag(FLAG_CARRY, !getFlag(FLAG_CARRY));
	}

	/**
	 * Rotate Left Circular (RLC): bit 7 -> carry and bit 0
	 */
	public void rlc(String register) {
		short val = getRegister(register);
		boolean bit7 = getBit(val, 7);
		
		val = (short) ((val << 1) & 0xFF);
		if (bit7) {
			val = (short) setBit(val, 0);
		}
		
		toogleFlag(FLAG_CARRY, bit7);
		toogleFlag(FLAG_ZERO, val == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, false);
		
		setRegister(register, val);
	}

	/**
	 * Rotate Right Circular (RRC): bit 0 -> carry and bit 7
	 */
	public void rrc(String register) {
		short val = getRegister(register);
		boolean bit0 = getBit(val, 0);
		
		val = (short) ((val >> 1) & 0x7F);
		if (bit0) {
			val = (short) setBit(val, 7);
		}
		
		toogleFlag(FLAG_CARRY, bit0);
		toogleFlag(FLAG_ZERO, val == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, false);
		
		setRegister(register, val);
	}

	/**
	 * Shift Left Arithmetic (SLA): bit 7 -> carry, bit 0 = 0
	 */
	public void sla(String register) {
		short val = getRegister(register);
		toogleFlag(FLAG_CARRY, getBit(val, 7));
		
		val = (short) ((val << 1) & 0xFF);
		
		toogleFlag(FLAG_ZERO, val == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, false);
		
		setRegister(register, val);
	}

	/**
	 * Shift Right Arithmetic (SRA): bit 0 -> carry, bit 7 unchanged
	 */
	public void sra(String register) {
		short val = getRegister(register);
		boolean bit7 = getBit(val, 7);
		toogleFlag(FLAG_CARRY, getBit(val, 0));
		
		val = (short) ((val >> 1) & 0x7F);
		if (bit7) {
			val = (short) setBit(val, 7);
		}
		
		toogleFlag(FLAG_ZERO, val == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, false);
		
		setRegister(register, val);
	}

	/**
	 * Shift Right Logical (SRL wrapper for register)
	 */
	public void srlReg(String register) {
		setRegister(register, srl(getRegister(register)));
	}

	public void andN(short val) {
		val = (short) (val & getRegister("A"));

		toogleFlag(FLAG_ZERO, val == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, true);
		toogleFlag(FLAG_CARRY, false);

		setRegister("A", val);
	}

	public void xorN(short val) {
		val = (short) (val ^ getRegister("A"));

		toogleFlag(FLAG_ZERO, val == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, false); // XOR always clears H flag
		toogleFlag(FLAG_CARRY, false);

		setRegister("A", val);
	}

	public short swap(short i) {
		i = (short) (((i & 0x0F) << 4) | ((i & 0xF0) >> 4));
		toogleFlag(FLAG_ZERO, i == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, false); // SWAP clears all flags except Z
		toogleFlag(FLAG_CARRY, false);

		return i;
	}

	public void orN(String register) {
		short i = getRegister(register);

		i = (short) (i | getRegister("A"));

		toogleFlag(FLAG_ZERO, i == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_CARRY, false);
		toogleFlag(FLAG_HALF_CARRY, false);

		setRegister("A", i);
	}

	public void addN(short val) {
		short a = getRegister("A");
		int result = a + val;
		
		toogleFlag(FLAG_HALF_CARRY, ((a & 0x0F) + (val & 0x0F)) > 0x0F);
		toogleFlag(FLAG_CARRY, result > 0xFF);

		result &= 0xFF;

		toogleFlag(FLAG_ZERO, result == 0);
		toogleFlag(FLAG_SUBTRACT, false);

		setRegister("A", (short) result);
	}

	public short add8Bit(short i, short j) {
		int x = i + j;

		// Handle overflow
		if (x > 0xFF) {
			x = x - 0x100;
		}
		return (short) x;
	}

	public int add16Bit(int i, int j, boolean updateFlags) {
		int result = i + j;

		if (updateFlags) {
			toogleFlag(Cpu.FLAG_SUBTRACT, false);
			toogleFlag(Cpu.FLAG_HALF_CARRY, ((i & 0x0FFF) + (j & 0x0FFF)) > 0x0FFF);
			toogleFlag(Cpu.FLAG_CARRY, result > 0xFFFF);
		}

		return result & 0xFFFF;
	}

	public int sub16Bit(int i, int j) {
		int result = i - j;
		return result & 0xFFFF;
	}

	public void cp(short i, short j) {
		int result = i - j;

		toogleFlag(Cpu.FLAG_ZERO, result == 0);
		toogleFlag(Cpu.FLAG_SUBTRACT, true);
		toogleFlag(Cpu.FLAG_HALF_CARRY, (i & 0x0F) < (j & 0x0F));
		toogleFlag(Cpu.FLAG_CARRY, i < j);
	}

	public short getRegister(String reg) {
		switch (reg) {
		case "A":
			return a;
		case "B":
			return b;
		case "C":
			return c;
		case "D":
			return d;
		case "E":
			return e;
		case "F":
			return f;
		case "H":
			return h;
		case "L":
			return l;
		case "(HL)":
			return getMem(get16BitRegister("HL"));
		case "(DE)":
			return getMem(get16BitRegister("DE"));
		default:
			throw new RuntimeException("Weird register:" + reg);
		}
	}

	public short srl(short val) {
		toogleFlag(FLAG_CARRY, getBit(val, 0));
		val = (short) ((val >> 1) & 0x7F); // Shift right, MSB becomes 0

		toogleFlag(Cpu.FLAG_ZERO, val == 0);
		toogleFlag(Cpu.FLAG_SUBTRACT, false);
		toogleFlag(Cpu.FLAG_HALF_CARRY, false);

		return val;
	}

	public void toogleFlag(int flag, boolean val) {
		if (val) {
			f = (short) setBit(f, flag);
		} else {
			f = (short) resetBit(f, flag);
		}
	}

	public int resetBit(int value, int bit) {
		value &= ~(1 << bit);
		return value;
	}

	public void setRegister(String reg, short val) {
		switch (reg) {
		case "A":
			a = val;
			break;
		case "B":
			b = val;
			break;
		case "C":
			c = val;
			break;
		case "D":
			d = val;
			break;
		case "E":
			e = val;
			break;
		case "F":
			f = val;
			break;
		case "H":
			h = val;
			break;
		case "L":
			l = val;
			break;
		case "(HL)":
			setMem(get16BitRegister("HL"), val);
			break;
		case "(DE)":
			setMem(get16BitRegister("DE"), val);
			break;
		default:
			throw new RuntimeException("Weird register:" + reg);
		}
	}

	// Flip all bits
	public short cpl(short val) {
		return (short) (~val & 0xFF);
	}

	// First bit on 0 !
	public int setBit(int value, int bit) {
		return value | (1 << bit);
	}

	public boolean getFlag(int flag) {
		return getBit(f, flag);
	}

	// Used for CB RL n (like CB 17)
	public void rl(String register) {
		short i = getRegister(register);

		boolean carry = getFlag(Cpu.FLAG_CARRY);
		toogleFlag(Cpu.FLAG_CARRY, getBit(i, 7));

		i = (short) (((i & 0xff) << 1) | ((i & 0xff) >>> (7)));
		i = (short) (i & 0xFF);

		if (carry) {
			i = (short) setBit(i, 0);
		} else {
			i = (short) resetBit(i, 0);
		}

		toogleFlag(Cpu.FLAG_ZERO, i == 0);
		toogleFlag(Cpu.FLAG_SUBTRACT, false);
		toogleFlag(Cpu.FLAG_HALF_CARRY, false);

		setRegister(register, i);
	}

	public void rr(String register) {
		short i = getRegister(register);

		boolean carry = getFlag(Cpu.FLAG_CARRY);
		toogleFlag(Cpu.FLAG_CARRY, getBit(i, 0)); // Check bit 0 for carry

		i = (short) ((i >> 1) & 0x7F); // Shift right

		if (carry) {
			i = (short) setBit(i, 7); // Old carry goes to bit 7
		}

		toogleFlag(Cpu.FLAG_ZERO, i == 0);
		toogleFlag(Cpu.FLAG_SUBTRACT, false);
		toogleFlag(Cpu.FLAG_HALF_CARRY, false);

		setRegister(register, i);
	}

	public void pop(String targetRegister) {
		int i = pop();
		set16BitRegister(targetRegister.charAt(0) + "", targetRegister.charAt(1) + "", i);
	}

	public int pop() {
		int i = get16bitValue(sp);

		setMem(sp, (short) 0);
		setMem(sp + 1, (short) 0);

		sp += 2;

		return i;
	}

	public void push(int val) { // 16bit always?
		sp -= 2;
		setMem(sp, (short) getLowByte(val));
		setMem(sp + 1, (short) getHighByte(val));
	}

	public int getLowByte(int val) {
		return (val & 0xFF);
	}

	public int getHighByte(int val) {
		return ((val >> 8) & 0xFF);
	}

	public int get16BitRegister(String reg) {
		switch (reg) {
		case "AF":
			return get16bitValue(getRegister("A"), getRegister("F"));
		case "BC":
			return get16bitValue(getRegister("B"), getRegister("C"));
		case "HL":
			return get16bitValue(getRegister("H"), getRegister("L"));
		case "DE":
			return get16bitValue(getRegister("D"), getRegister("E"));
		default:
			throw new RuntimeException("Unknown register pair:" + reg);
		}
	}

	public void set16BitRegister(String highReg, String lowReg, int val) {
		setRegister(highReg, (short) getHighByte(val));
		setRegister(lowReg, (short) getLowByte(val));
	}

	public boolean getBit(int value, int bit) {
		return ((value >> bit) & 1) == 1;
	}

	public short getMem(int i) {
		return rom[i & 0xFFFF];
	}

	public void setMem(int i, short value) {
		rom[i & 0xFFFF] = value;
	}

}
