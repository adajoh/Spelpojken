package se.djax.spelpojken;

/**
 * Memory Bank Controller implementation.
 * Handles ROM/RAM banking for cartridges larger than 32KB.
 */
public class MBC {

	public enum MBCType {
		NONE,       // No MBC (32KB ROM only)
		MBC1,       // Up to 2MB ROM, 32KB RAM
		MBC2,       // Up to 256KB ROM, 512x4 bits RAM
		MBC3,       // Up to 2MB ROM, 32KB RAM, RTC
		MBC5        // Up to 8MB ROM, 128KB RAM
	}

	private final Cpu cpu;
	private MBCType type = MBCType.NONE;
	
	// Full ROM data (can be larger than 32KB)
	private short[] fullRom;
	
	// External RAM (up to 128KB)
	private short[] externalRam;
	
	// MBC registers
	private boolean ramEnabled = false;
	private int romBank = 1;
	private int ramBank = 0;
	private boolean romRamMode = false; // false = ROM mode, true = RAM mode (MBC1)
	
	// ROM/RAM sizes
	private int romSize;
	private int ramSize;

	public MBC(Cpu cpu) {
		this.cpu = cpu;
		externalRam = new short[0x20000]; // 128KB max
	}

	/**
	 * Initialize MBC based on cartridge header.
	 */
	public void init(short[] romData) {
		this.fullRom = romData;
		
		// Read cartridge type from header (0x0147)
		int cartridgeType = romData[0x0147];
		
		switch (cartridgeType) {
			case 0x00:
				type = MBCType.NONE;
				break;
			case 0x01:
			case 0x02:
			case 0x03:
				type = MBCType.MBC1;
				break;
			case 0x05:
			case 0x06:
				type = MBCType.MBC2;
				break;
			case 0x0F:
			case 0x10:
			case 0x11:
			case 0x12:
			case 0x13:
				type = MBCType.MBC3;
				break;
			case 0x19:
			case 0x1A:
			case 0x1B:
			case 0x1C:
			case 0x1D:
			case 0x1E:
				type = MBCType.MBC5;
				break;
			default:
				type = MBCType.NONE;
		}
		
		// Read ROM size (0x0148)
		int romSizeCode = romData[0x0148];
		romSize = 32768 << romSizeCode; // 32KB << code
		
		// Read RAM size (0x0149)
		int ramSizeCode = romData[0x0149];
		switch (ramSizeCode) {
			case 0x00: ramSize = 0; break;
			case 0x01: ramSize = 2048; break;
			case 0x02: ramSize = 8192; break;
			case 0x03: ramSize = 32768; break;
			case 0x04: ramSize = 131072; break;
			case 0x05: ramSize = 65536; break;
			default: ramSize = 0;
		}
		
		System.out.println("MBC Type: " + type + ", ROM: " + romSize + " bytes, RAM: " + ramSize + " bytes");
	}

	/**
	 * Handle writes to ROM area (0x0000-0x7FFF) for MBC control.
	 */
	public void writeRom(int address, short value) {
		if (type == MBCType.NONE) {
			return; // No MBC, ignore writes
		}
		
		switch (type) {
			case MBC1:
				handleMBC1Write(address, value);
				break;
			case MBC3:
				handleMBC3Write(address, value);
				break;
			case MBC5:
				handleMBC5Write(address, value);
				break;
			default:
				break;
		}
	}

	private void handleMBC1Write(int address, short value) {
		if (address < 0x2000) {
			// RAM Enable
			ramEnabled = (value & 0x0F) == 0x0A;
		} else if (address < 0x4000) {
			// ROM Bank Number (lower 5 bits)
			int bank = value & 0x1F;
			if (bank == 0) bank = 1;
			romBank = (romBank & 0x60) | bank;
			updateRomBank();
		} else if (address < 0x6000) {
			// RAM Bank Number / Upper ROM Bank bits
			if (romRamMode) {
				ramBank = value & 0x03;
			} else {
				romBank = (romBank & 0x1F) | ((value & 0x03) << 5);
				updateRomBank();
			}
		} else if (address < 0x8000) {
			// ROM/RAM Mode Select
			romRamMode = (value & 0x01) != 0;
		}
	}

	private void handleMBC3Write(int address, short value) {
		if (address < 0x2000) {
			// RAM/RTC Enable
			ramEnabled = (value & 0x0F) == 0x0A;
		} else if (address < 0x4000) {
			// ROM Bank Number
			int bank = value & 0x7F;
			if (bank == 0) bank = 1;
			romBank = bank;
			updateRomBank();
		} else if (address < 0x6000) {
			// RAM Bank Number / RTC Register Select
			ramBank = value & 0x0F;
		}
	}

	private void handleMBC5Write(int address, short value) {
		if (address < 0x2000) {
			// RAM Enable
			ramEnabled = (value & 0x0F) == 0x0A;
		} else if (address < 0x3000) {
			// ROM Bank Number (lower 8 bits)
			romBank = (romBank & 0x100) | (value & 0xFF);
			updateRomBank();
		} else if (address < 0x4000) {
			// ROM Bank Number (bit 8)
			romBank = (romBank & 0xFF) | ((value & 0x01) << 8);
			updateRomBank();
		} else if (address < 0x6000) {
			// RAM Bank Number
			ramBank = value & 0x0F;
		}
	}

	/**
	 * Update the ROM bank mapped at 0x4000-0x7FFF.
	 */
	private void updateRomBank() {
		int bankOffset = romBank * 0x4000;
		
		// Make sure we don't read beyond the ROM
		if (bankOffset + 0x4000 > fullRom.length) {
			bankOffset = bankOffset % fullRom.length;
		}
		
		// Copy ROM bank to 0x4000-0x7FFF
		for (int i = 0; i < 0x4000; i++) {
			if (bankOffset + i < fullRom.length) {
				cpu.setMem(0x4000 + i, fullRom[bankOffset + i]);
			}
		}
	}

	/**
	 * Read from external RAM (0xA000-0xBFFF).
	 */
	public short readRam(int address) {
		if (!ramEnabled || ramSize == 0) {
			return 0xFF;
		}
		
		int offset = (ramBank * 0x2000) + (address - 0xA000);
		if (offset < ramSize) {
			return externalRam[offset];
		}
		return 0xFF;
	}

	/**
	 * Write to external RAM (0xA000-0xBFFF).
	 */
	public void writeRam(int address, short value) {
		if (!ramEnabled || ramSize == 0) {
			return;
		}
		
		int offset = (ramBank * 0x2000) + (address - 0xA000);
		if (offset < ramSize) {
			externalRam[offset] = value;
		}
	}

	public MBCType getType() {
		return type;
	}

	public boolean hasRAM() {
		return ramSize > 0;
	}
}
