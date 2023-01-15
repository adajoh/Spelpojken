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

	public void loadRom(File file, boolean withBootRom) {
		try {
			Assert.assertTrue(file.exists());
			byte[] data = Files.readAllBytes(file.toPath());

			// init
			for (int i = 0; i < rom.length; i++) {
				// rom[i] = -1;
			}

			// load rom
			for (int i = 0; i < data.length; i++) {
				rom[i] = (short) Byte.toUnsignedInt(data[i]);
			}

			if (withBootRom) {
				File boot = new File(Cpu.class.getResource("/DMG_ROM.gb").toURI());
				Assert.assertTrue(boot.exists());
				byte[] bootData = Files.readAllBytes(boot.toPath());
				for (int i = 0; i < 256; i++) {
					rom[i] = (short) Byte.toUnsignedInt(bootData[i]);
				}
				LOG.info("Loaded boot rom");
			}

			LOG.info("Loaded rom:" + file.getAbsolutePath() + " size:" + data.length + " bytes");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void init() {

	}

	public int get16bitValue(int startAddress) {
		return get16bitValue(rom[startAddress + 1], rom[startAddress]);
	}

	public int get16bitValue(short high, short low) {
		return high << 8 & 0xFF00 | low & 0xFF;
	}

	public void inc(String register) {
		short i = getRegister(register);

		if (i == 0xFF) {
			i = 0;
		} else {
			i++;
		}

		toogleFlag(FLAG_ZERO, i == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, (i & 0x0F) + 1 > 0x0F); // TODO not tested

		setRegister(register, i);
	}

	public void dec(String register) {
		short i = getRegister(register);

		toogleFlag(FLAG_HALF_CARRY, (i & 0x0F) == 0); // TODO not tested

		if (i == 0) {
			i = 0xFF;
		} else {
			i--;
		}

		toogleFlag(FLAG_ZERO, i == 0);
		toogleFlag(FLAG_SUBTRACT, true);

		setRegister(register, i);
	}

	public void subN(short val) {
		toogleFlag(FLAG_HALF_CARRY, (val & 0x0F) == 0); // TODO not tested

		val = (short) (getRegister("A") - val);

		if (val < 0) {
			val = 0xFF;
		}

		toogleFlag(FLAG_ZERO, val == 0);
		toogleFlag(FLAG_SUBTRACT, true);

		setRegister("A", val);
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
		toogleFlag(FLAG_HALF_CARRY, true);
		toogleFlag(FLAG_CARRY, false);

		setRegister("A", val);
	}

	public short swap(short i) {
		i = (short) (i >> 4 | i << 4 & 0xFF);
		toogleFlag(FLAG_ZERO, i == 0);
		toogleFlag(FLAG_SUBTRACT, false);
		toogleFlag(FLAG_HALF_CARRY, true);
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
		toogleFlag(FLAG_HALF_CARRY, (val & 0x0F) == 0); // TODO not tested
		// TODO fix carry

		val = add8Bit(getRegister("A"), val);

		toogleFlag(FLAG_ZERO, val == 0);
		toogleFlag(FLAG_SUBTRACT, false);

		setRegister("A", val);
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
		int x = i + j;

		// Handle overflow
		if (x > 0xFFFF) { // TODO not tested and I do not know if this is right, used for HL++
			x = x - 0x10000;
		}

		if (updateFlags) {
			toogleFlag(Cpu.FLAG_SUBTRACT, false);
			toogleFlag(Cpu.FLAG_HALF_CARRY, false); // TODO fix flags for 0x19
			toogleFlag(Cpu.FLAG_CARRY, false);
		}

		return x;
	}

	public int sub16Bit(int i, int j) {
		int x = i - j;

		// Handle underflow
		if (x < 0) { // TODO not tested and I do not know if this is right
			x = 0xFFFF;
		}
		return x;
	}

	@SuppressWarnings("unused")
	public void cp(short i, short j) {
		int val = i - j;

		toogleFlag(Cpu.FLAG_ZERO, val == 0);
		toogleFlag(Cpu.FLAG_SUBTRACT, true);

		// TODO figure this out
		if (false) {
			// cpu.f = (short) cpu.setBit(cpu.f, Cpu.FLAG_HALF_CARRY);
		}

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
		val = (short) (a >> 1 & 0xFF);

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
		toogleFlag(Cpu.FLAG_CARRY, getBit(i, 7));

		i = (short) (((i & 0xff) >>> 7) | ((i & 0xff) << (1)));
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

	public void pop(String targetRegister) {
		int i = pop();
		set16BitRegister(targetRegister.charAt(0) + "", targetRegister.charAt(1) + "", i);
	}

	public int pop() {
		int i = get16bitValue(sp);

		rom[sp] = 0;
		rom[sp + 1] = 0;

		sp += 2;

		return i;
	}

	public void push(int val) { // 16bit always?
		sp -= 2;
		rom[sp] = (short) getLowByte(val);
		rom[sp + 1] = (short) getHighByte(val);
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
		return rom[i];
	}

	public void setMem(int i, short value) {
		rom[i] = value;
	}

}
