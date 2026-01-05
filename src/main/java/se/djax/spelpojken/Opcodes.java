package se.djax.spelpojken;

public class Opcodes {

	private interface Code {
		public void exec();
	}

	public class Opcode {

		public final String name;
		public final int cycles;
		public final Code code;
		public final int lenght;

		public Opcode(String name, int cycles, int lenght, Code code) {
			this.name = name;
			this.cycles = cycles;
			this.code = code;
			this.lenght = lenght;
		}
	}

	private final Cpu cpu;
	private Opcode[] opcodes;
	private Opcode[] opcodesCB;
	private Interrupts interrupts;

	public Opcodes(Cpu cpu) {
		this.cpu = cpu;
		opcodes = new Opcode[0x100];
		opcodesCB = new Opcode[0x100];
		initOpcodes();
		initCBOpcodes();
	}

	public void setInterrupts(Interrupts interrupts) {
		this.interrupts = interrupts;
	}

	private void initOpcodes() {
		// 0x00 - 0x0F
		opcodes[0x00] = create("NOP", 4, 1, () -> {});
		opcodes[0x01] = create("LD BC,d16", 12, 3, () -> {
			cpu.set16BitRegister("B", "C", cpu.get16bitValue(cpu.pc + 1));
		});
		opcodes[0x02] = create("LD (BC),A", 8, 1, () -> {
			cpu.setMem(cpu.get16BitRegister("BC"), cpu.a);
		});
		opcodes[0x03] = create("INC BC", 8, 1, () -> {
			cpu.set16BitRegister("B", "C", cpu.add16Bit(cpu.get16BitRegister("BC"), 1, false));
		});
		opcodes[0x04] = create("INC B", 4, 1, () -> cpu.inc("B"));
		opcodes[0x05] = create("DEC B", 4, 1, () -> cpu.dec("B"));
		opcodes[0x06] = create("LD B,d8", 8, 2, () -> cpu.b = cpu.getMem(cpu.pc + 1));
		opcodes[0x07] = create("RLCA", 4, 1, () -> {
			boolean bit7 = cpu.getBit(cpu.a, 7);
			cpu.a = (short) ((cpu.a << 1) & 0xFF);
			if (bit7) cpu.a = (short) cpu.setBit(cpu.a, 0);
			cpu.toogleFlag(Cpu.FLAG_CARRY, bit7);
			cpu.toogleFlag(Cpu.FLAG_ZERO, false);
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, false);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, false);
		});
		opcodes[0x08] = create("LD (a16),SP", 20, 3, () -> {
			int addr = cpu.get16bitValue(cpu.pc + 1);
			cpu.setMem(addr, (short) cpu.getLowByte(cpu.sp));
			cpu.setMem(addr + 1, (short) cpu.getHighByte(cpu.sp));
		});
		opcodes[0x09] = create("ADD HL,BC", 8, 1, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), cpu.get16BitRegister("BC"), true));
		});
		opcodes[0x0A] = create("LD A,(BC)", 8, 1, () -> {
			cpu.a = cpu.getMem(cpu.get16BitRegister("BC"));
		});
		opcodes[0x0B] = create("DEC BC", 8, 1, () -> {
			cpu.set16BitRegister("B", "C", cpu.sub16Bit(cpu.get16BitRegister("BC"), 1));
		});
		opcodes[0x0C] = create("INC C", 4, 1, () -> cpu.inc("C"));
		opcodes[0x0D] = create("DEC C", 4, 1, () -> cpu.dec("C"));
		opcodes[0x0E] = create("LD C,d8", 8, 2, () -> cpu.c = cpu.getMem(cpu.pc + 1));
		opcodes[0x0F] = create("RRCA", 4, 1, () -> {
			boolean bit0 = cpu.getBit(cpu.a, 0);
			cpu.a = (short) ((cpu.a >> 1) & 0x7F);
			if (bit0) cpu.a = (short) cpu.setBit(cpu.a, 7);
			cpu.toogleFlag(Cpu.FLAG_CARRY, bit0);
			cpu.toogleFlag(Cpu.FLAG_ZERO, false);
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, false);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, false);
		});

		// 0x10 - 0x1F
		opcodes[0x10] = create("STOP", 4, 2, () -> {
			// STOP instruction - typically used for speed switching in GBC
		});
		opcodes[0x11] = create("LD DE,d16", 12, 3, () -> {
			cpu.set16BitRegister("D", "E", cpu.get16bitValue(cpu.pc + 1));
		});
		opcodes[0x12] = create("LD (DE),A", 8, 1, () -> {
			cpu.setRegister("(DE)", cpu.getRegister("A"));
		});
		opcodes[0x13] = create("INC DE", 8, 1, () -> {
			cpu.set16BitRegister("D", "E", cpu.add16Bit(cpu.get16BitRegister("DE"), 1, false));
		});
		opcodes[0x14] = create("INC D", 4, 1, () -> cpu.inc("D"));
		opcodes[0x15] = create("DEC D", 4, 1, () -> cpu.dec("D"));
		opcodes[0x16] = create("LD D,d8", 8, 2, () -> cpu.d = cpu.getMem(cpu.pc + 1));
		opcodes[0x17] = create("RLA", 4, 1, () -> {
			boolean carry = cpu.getFlag(Cpu.FLAG_CARRY);
			boolean bit7 = cpu.getBit(cpu.a, 7);
			cpu.a = (short) ((cpu.a << 1) & 0xFF);
			if (carry) cpu.a = (short) cpu.setBit(cpu.a, 0);
			cpu.toogleFlag(Cpu.FLAG_CARRY, bit7);
			cpu.toogleFlag(Cpu.FLAG_ZERO, false);
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, false);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, false);
		});
		opcodes[0x18] = create("JR r8", 12, 2, () -> {
			byte i = (byte) cpu.getMem(cpu.pc + 1);
			cpu.pc += i;
		});
		opcodes[0x19] = create("ADD HL,DE", 8, 1, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), cpu.get16BitRegister("DE"), true));
		});
		opcodes[0x1A] = create("LD A,(DE)", 8, 1, () -> {
			cpu.setRegister("A", cpu.getMem(cpu.get16BitRegister("DE")));
		});
		opcodes[0x1B] = create("DEC DE", 8, 1, () -> {
			cpu.set16BitRegister("D", "E", cpu.sub16Bit(cpu.get16BitRegister("DE"), 1));
		});
		opcodes[0x1C] = create("INC E", 4, 1, () -> cpu.inc("E"));
		opcodes[0x1D] = create("DEC E", 4, 1, () -> cpu.dec("E"));
		opcodes[0x1E] = create("LD E,d8", 8, 2, () -> cpu.e = cpu.getMem(cpu.pc + 1));
		opcodes[0x1F] = create("RRA", 4, 1, () -> {
			boolean carry = cpu.getFlag(Cpu.FLAG_CARRY);
			boolean bit0 = cpu.getBit(cpu.a, 0);
			cpu.a = (short) ((cpu.a >> 1) & 0x7F);
			if (carry) cpu.a = (short) cpu.setBit(cpu.a, 7);
			cpu.toogleFlag(Cpu.FLAG_CARRY, bit0);
			cpu.toogleFlag(Cpu.FLAG_ZERO, false);
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, false);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, false);
		});

		// 0x20 - 0x2F
		opcodes[0x20] = create("JR NZ,r8", 12, 2, () -> {
			if (!cpu.getFlag(Cpu.FLAG_ZERO)) {
				byte jp = (byte) cpu.getMem(cpu.pc + 1);
				cpu.pc += jp;
			}
		});
		opcodes[0x21] = create("LD HL,d16", 12, 3, () -> {
			cpu.set16BitRegister("H", "L", cpu.get16bitValue(cpu.pc + 1));
		});
		opcodes[0x22] = create("LD (HL+),A", 8, 1, () -> {
			int hl = cpu.get16BitRegister("HL");
			cpu.setMem(hl, cpu.getRegister("A"));
			cpu.set16BitRegister("H", "L", hl + 1);
		});
		opcodes[0x23] = create("INC HL", 8, 1, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), 1, false));
		});
		opcodes[0x24] = create("INC H", 4, 1, () -> cpu.inc("H"));
		opcodes[0x25] = create("DEC H", 4, 1, () -> cpu.dec("H"));
		opcodes[0x26] = create("LD H,d8", 8, 2, () -> cpu.h = cpu.getMem(cpu.pc + 1));
		opcodes[0x27] = create("DAA", 4, 1, () -> cpu.daa());
		opcodes[0x28] = create("JR Z,r8", 12, 2, () -> {
			if (cpu.getFlag(Cpu.FLAG_ZERO)) {
				byte jp = (byte) cpu.getMem(cpu.pc + 1);
				cpu.pc += jp;
			}
		});
		opcodes[0x29] = create("ADD HL,HL", 8, 1, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), cpu.get16BitRegister("HL"), true));
		});
		opcodes[0x2A] = create("LD A,(HL+)", 8, 1, () -> {
			int hl = cpu.get16BitRegister("HL");
			cpu.setRegister("A", cpu.getMem(hl));
			cpu.set16BitRegister("H", "L", cpu.add16Bit(hl, 1, false));
		});
		opcodes[0x2B] = create("DEC HL", 8, 1, () -> {
			cpu.set16BitRegister("H", "L", cpu.sub16Bit(cpu.get16BitRegister("HL"), 1));
		});
		opcodes[0x2C] = create("INC L", 4, 1, () -> cpu.inc("L"));
		opcodes[0x2D] = create("DEC L", 4, 1, () -> cpu.dec("L"));
		opcodes[0x2E] = create("LD L,d8", 8, 2, () -> cpu.setRegister("L", cpu.getMem(cpu.pc + 1)));
		opcodes[0x2F] = create("CPL", 4, 1, () -> {
			cpu.setRegister("A", cpu.cpl(cpu.getRegister("A")));
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, true);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, true);
		});

		// 0x30 - 0x3F
		opcodes[0x30] = create("JR NC,r8", 12, 2, () -> {
			if (!cpu.getFlag(Cpu.FLAG_CARRY)) {
				byte jp = (byte) cpu.getMem(cpu.pc + 1);
				cpu.pc += jp;
			}
		});
		opcodes[0x31] = create("LD SP,d16", 12, 3, () -> {
			cpu.sp = cpu.get16bitValue(cpu.pc + 1);
		});
		opcodes[0x32] = create("LD (HL-),A", 8, 1, () -> {
			int hl = cpu.get16BitRegister("HL");
			cpu.setMem(hl, cpu.a);
			cpu.set16BitRegister("H", "L", hl - 1);
		});
		opcodes[0x33] = create("INC SP", 8, 1, () -> {
			cpu.sp = (cpu.sp + 1) & 0xFFFF;
		});
		opcodes[0x34] = create("INC (HL)", 12, 1, () -> cpu.inc("(HL)"));
		opcodes[0x35] = create("DEC (HL)", 12, 1, () -> cpu.dec("(HL)"));
		opcodes[0x36] = create("LD (HL),d8", 12, 2, () -> {
			cpu.setMem(cpu.get16BitRegister("HL"), cpu.getMem(cpu.pc + 1));
		});
		opcodes[0x37] = create("SCF", 4, 1, () -> cpu.scf());
		opcodes[0x38] = create("JR C,r8", 12, 2, () -> {
			if (cpu.getFlag(Cpu.FLAG_CARRY)) {
				byte jp = (byte) cpu.getMem(cpu.pc + 1);
				cpu.pc += jp;
			}
		});
		opcodes[0x39] = create("ADD HL,SP", 8, 1, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), cpu.sp, true));
		});
		opcodes[0x3A] = create("LD A,(HL-)", 8, 1, () -> {
			int hl = cpu.get16BitRegister("HL");
			cpu.a = cpu.getMem(hl);
			cpu.set16BitRegister("H", "L", hl - 1);
		});
		opcodes[0x3B] = create("DEC SP", 8, 1, () -> {
			cpu.sp = (cpu.sp - 1) & 0xFFFF;
		});
		opcodes[0x3C] = create("INC A", 4, 1, () -> cpu.inc("A"));
		opcodes[0x3D] = create("DEC A", 4, 1, () -> cpu.dec("A"));
		opcodes[0x3E] = create("LD A,d8", 8, 2, () -> cpu.a = cpu.getMem(cpu.pc + 1));
		opcodes[0x3F] = create("CCF", 4, 1, () -> cpu.ccf());

		// 0x40 - 0x4F: LD B/C,r
		opcodes[0x40] = create("LD B,B", 4, 1, () -> cpu.b = cpu.b);
		opcodes[0x41] = create("LD B,C", 4, 1, () -> cpu.b = cpu.c);
		opcodes[0x42] = create("LD B,D", 4, 1, () -> cpu.b = cpu.d);
		opcodes[0x43] = create("LD B,E", 4, 1, () -> cpu.b = cpu.e);
		opcodes[0x44] = create("LD B,H", 4, 1, () -> cpu.b = cpu.h);
		opcodes[0x45] = create("LD B,L", 4, 1, () -> cpu.b = cpu.l);
		opcodes[0x46] = create("LD B,(HL)", 8, 1, () -> cpu.setRegister("B", cpu.getMem(cpu.get16BitRegister("HL"))));
		opcodes[0x47] = create("LD B,A", 4, 1, () -> cpu.b = cpu.a);
		opcodes[0x48] = create("LD C,B", 4, 1, () -> cpu.c = cpu.b);
		opcodes[0x49] = create("LD C,C", 4, 1, () -> cpu.c = cpu.c);
		opcodes[0x4A] = create("LD C,D", 4, 1, () -> cpu.c = cpu.d);
		opcodes[0x4B] = create("LD C,E", 4, 1, () -> cpu.c = cpu.e);
		opcodes[0x4C] = create("LD C,H", 4, 1, () -> cpu.c = cpu.h);
		opcodes[0x4D] = create("LD C,L", 4, 1, () -> cpu.c = cpu.l);
		opcodes[0x4E] = create("LD C,(HL)", 8, 1, () -> cpu.setRegister("C", cpu.getMem(cpu.get16BitRegister("HL"))));
		opcodes[0x4F] = create("LD C,A", 4, 1, () -> cpu.c = cpu.a);

		// 0x50 - 0x5F: LD D/E,r
		opcodes[0x50] = create("LD D,B", 4, 1, () -> cpu.d = cpu.b);
		opcodes[0x51] = create("LD D,C", 4, 1, () -> cpu.d = cpu.c);
		opcodes[0x52] = create("LD D,D", 4, 1, () -> cpu.d = cpu.d);
		opcodes[0x53] = create("LD D,E", 4, 1, () -> cpu.d = cpu.e);
		opcodes[0x54] = create("LD D,H", 4, 1, () -> cpu.d = cpu.h);
		opcodes[0x55] = create("LD D,L", 4, 1, () -> cpu.d = cpu.l);
		opcodes[0x56] = create("LD D,(HL)", 8, 1, () -> cpu.setRegister("D", cpu.getMem(cpu.get16BitRegister("HL"))));
		opcodes[0x57] = create("LD D,A", 4, 1, () -> cpu.d = cpu.a);
		opcodes[0x58] = create("LD E,B", 4, 1, () -> cpu.e = cpu.b);
		opcodes[0x59] = create("LD E,C", 4, 1, () -> cpu.e = cpu.c);
		opcodes[0x5A] = create("LD E,D", 4, 1, () -> cpu.e = cpu.d);
		opcodes[0x5B] = create("LD E,E", 4, 1, () -> cpu.e = cpu.e);
		opcodes[0x5C] = create("LD E,H", 4, 1, () -> cpu.e = cpu.h);
		opcodes[0x5D] = create("LD E,L", 4, 1, () -> cpu.e = cpu.l);
		opcodes[0x5E] = create("LD E,(HL)", 8, 1, () -> cpu.setRegister("E", cpu.getMem(cpu.get16BitRegister("HL"))));
		opcodes[0x5F] = create("LD E,A", 4, 1, () -> cpu.e = cpu.a);

		// 0x60 - 0x6F: LD H/L,r
		opcodes[0x60] = create("LD H,B", 4, 1, () -> cpu.h = cpu.b);
		opcodes[0x61] = create("LD H,C", 4, 1, () -> cpu.h = cpu.c);
		opcodes[0x62] = create("LD H,D", 4, 1, () -> cpu.h = cpu.d);
		opcodes[0x63] = create("LD H,E", 4, 1, () -> cpu.h = cpu.e);
		opcodes[0x64] = create("LD H,H", 4, 1, () -> cpu.h = cpu.h);
		opcodes[0x65] = create("LD H,L", 4, 1, () -> cpu.h = cpu.l);
		opcodes[0x66] = create("LD H,(HL)", 8, 1, () -> cpu.setRegister("H", cpu.getMem(cpu.get16BitRegister("HL"))));
		opcodes[0x67] = create("LD H,A", 4, 1, () -> cpu.h = cpu.a);
		opcodes[0x68] = create("LD L,B", 4, 1, () -> cpu.l = cpu.b);
		opcodes[0x69] = create("LD L,C", 4, 1, () -> cpu.l = cpu.c);
		opcodes[0x6A] = create("LD L,D", 4, 1, () -> cpu.l = cpu.d);
		opcodes[0x6B] = create("LD L,E", 4, 1, () -> cpu.l = cpu.e);
		opcodes[0x6C] = create("LD L,H", 4, 1, () -> cpu.l = cpu.h);
		opcodes[0x6D] = create("LD L,L", 4, 1, () -> cpu.l = cpu.l);
		opcodes[0x6E] = create("LD L,(HL)", 8, 1, () -> cpu.setRegister("L", cpu.getRegister("(HL)")));
		opcodes[0x6F] = create("LD L,A", 4, 1, () -> cpu.l = cpu.a);

		// 0x70 - 0x7F: LD (HL)/A,r
		opcodes[0x70] = create("LD (HL),B", 8, 1, () -> cpu.setRegister("(HL)", cpu.getRegister("B")));
		opcodes[0x71] = create("LD (HL),C", 8, 1, () -> cpu.setRegister("(HL)", cpu.getRegister("C")));
		opcodes[0x72] = create("LD (HL),D", 8, 1, () -> cpu.setRegister("(HL)", cpu.getRegister("D")));
		opcodes[0x73] = create("LD (HL),E", 8, 1, () -> cpu.setRegister("(HL)", cpu.getRegister("E")));
		opcodes[0x74] = create("LD (HL),H", 8, 1, () -> cpu.setRegister("(HL)", cpu.getRegister("H")));
		opcodes[0x75] = create("LD (HL),L", 8, 1, () -> cpu.setRegister("(HL)", cpu.getRegister("L")));
		opcodes[0x76] = create("HALT", 4, 1, () -> {
			if (interrupts != null) {
				interrupts.setHalted(true);
			}
		});
		opcodes[0x77] = create("LD (HL),A", 8, 1, () -> cpu.setMem(cpu.get16BitRegister("HL"), cpu.a));
		opcodes[0x78] = create("LD A,B", 4, 1, () -> cpu.a = cpu.b);
		opcodes[0x79] = create("LD A,C", 4, 1, () -> cpu.a = cpu.c);
		opcodes[0x7A] = create("LD A,D", 4, 1, () -> cpu.a = cpu.d);
		opcodes[0x7B] = create("LD A,E", 4, 1, () -> cpu.a = cpu.e);
		opcodes[0x7C] = create("LD A,H", 4, 1, () -> cpu.a = cpu.h);
		opcodes[0x7D] = create("LD A,L", 4, 1, () -> cpu.a = cpu.l);
		opcodes[0x7E] = create("LD A,(HL)", 8, 1, () -> cpu.setRegister("A", cpu.getRegister("(HL)")));
		opcodes[0x7F] = create("LD A,A", 4, 1, () -> cpu.a = cpu.a);

		// 0x80 - 0x8F: ADD/ADC A,r
		opcodes[0x80] = create("ADD A,B", 4, 1, () -> cpu.addN(cpu.getRegister("B")));
		opcodes[0x81] = create("ADD A,C", 4, 1, () -> cpu.addN(cpu.getRegister("C")));
		opcodes[0x82] = create("ADD A,D", 4, 1, () -> cpu.addN(cpu.getRegister("D")));
		opcodes[0x83] = create("ADD A,E", 4, 1, () -> cpu.addN(cpu.getRegister("E")));
		opcodes[0x84] = create("ADD A,H", 4, 1, () -> cpu.addN(cpu.getRegister("H")));
		opcodes[0x85] = create("ADD A,L", 4, 1, () -> cpu.addN(cpu.getRegister("L")));
		opcodes[0x86] = create("ADD A,(HL)", 8, 1, () -> cpu.addN(cpu.getRegister("(HL)")));
		opcodes[0x87] = create("ADD A,A", 4, 1, () -> cpu.addN(cpu.getRegister("A")));
		opcodes[0x88] = create("ADC A,B", 4, 1, () -> cpu.adcN(cpu.getRegister("B")));
		opcodes[0x89] = create("ADC A,C", 4, 1, () -> cpu.adcN(cpu.getRegister("C")));
		opcodes[0x8A] = create("ADC A,D", 4, 1, () -> cpu.adcN(cpu.getRegister("D")));
		opcodes[0x8B] = create("ADC A,E", 4, 1, () -> cpu.adcN(cpu.getRegister("E")));
		opcodes[0x8C] = create("ADC A,H", 4, 1, () -> cpu.adcN(cpu.getRegister("H")));
		opcodes[0x8D] = create("ADC A,L", 4, 1, () -> cpu.adcN(cpu.getRegister("L")));
		opcodes[0x8E] = create("ADC A,(HL)", 8, 1, () -> cpu.adcN(cpu.getRegister("(HL)")));
		opcodes[0x8F] = create("ADC A,A", 4, 1, () -> cpu.adcN(cpu.getRegister("A")));

		// 0x90 - 0x9F: SUB/SBC A,r
		opcodes[0x90] = create("SUB B", 4, 1, () -> cpu.subN(cpu.getRegister("B")));
		opcodes[0x91] = create("SUB C", 4, 1, () -> cpu.subN(cpu.getRegister("C")));
		opcodes[0x92] = create("SUB D", 4, 1, () -> cpu.subN(cpu.getRegister("D")));
		opcodes[0x93] = create("SUB E", 4, 1, () -> cpu.subN(cpu.getRegister("E")));
		opcodes[0x94] = create("SUB H", 4, 1, () -> cpu.subN(cpu.getRegister("H")));
		opcodes[0x95] = create("SUB L", 4, 1, () -> cpu.subN(cpu.getRegister("L")));
		opcodes[0x96] = create("SUB (HL)", 8, 1, () -> cpu.subN(cpu.getRegister("(HL)")));
		opcodes[0x97] = create("SUB A", 4, 1, () -> cpu.subN(cpu.getRegister("A")));
		opcodes[0x98] = create("SBC A,B", 4, 1, () -> cpu.sbcN(cpu.getRegister("B")));
		opcodes[0x99] = create("SBC A,C", 4, 1, () -> cpu.sbcN(cpu.getRegister("C")));
		opcodes[0x9A] = create("SBC A,D", 4, 1, () -> cpu.sbcN(cpu.getRegister("D")));
		opcodes[0x9B] = create("SBC A,E", 4, 1, () -> cpu.sbcN(cpu.getRegister("E")));
		opcodes[0x9C] = create("SBC A,H", 4, 1, () -> cpu.sbcN(cpu.getRegister("H")));
		opcodes[0x9D] = create("SBC A,L", 4, 1, () -> cpu.sbcN(cpu.getRegister("L")));
		opcodes[0x9E] = create("SBC A,(HL)", 8, 1, () -> cpu.sbcN(cpu.getRegister("(HL)")));
		opcodes[0x9F] = create("SBC A,A", 4, 1, () -> cpu.sbcN(cpu.getRegister("A")));

		// 0xA0 - 0xAF: AND/XOR A,r
		opcodes[0xA0] = create("AND B", 4, 1, () -> cpu.andN(cpu.getRegister("B")));
		opcodes[0xA1] = create("AND C", 4, 1, () -> cpu.andN(cpu.getRegister("C")));
		opcodes[0xA2] = create("AND D", 4, 1, () -> cpu.andN(cpu.getRegister("D")));
		opcodes[0xA3] = create("AND E", 4, 1, () -> cpu.andN(cpu.getRegister("E")));
		opcodes[0xA4] = create("AND H", 4, 1, () -> cpu.andN(cpu.getRegister("H")));
		opcodes[0xA5] = create("AND L", 4, 1, () -> cpu.andN(cpu.getRegister("L")));
		opcodes[0xA6] = create("AND (HL)", 8, 1, () -> cpu.andN(cpu.getRegister("(HL)")));
		opcodes[0xA7] = create("AND A", 4, 1, () -> cpu.andN(cpu.getRegister("A")));
		opcodes[0xA8] = create("XOR B", 4, 1, () -> cpu.xorN(cpu.getRegister("B")));
		opcodes[0xA9] = create("XOR C", 4, 1, () -> cpu.xorN(cpu.getRegister("C")));
		opcodes[0xAA] = create("XOR D", 4, 1, () -> cpu.xorN(cpu.getRegister("D")));
		opcodes[0xAB] = create("XOR E", 4, 1, () -> cpu.xorN(cpu.getRegister("E")));
		opcodes[0xAC] = create("XOR H", 4, 1, () -> cpu.xorN(cpu.getRegister("H")));
		opcodes[0xAD] = create("XOR L", 4, 1, () -> cpu.xorN(cpu.getRegister("L")));
		opcodes[0xAE] = create("XOR (HL)", 8, 1, () -> cpu.xorN(cpu.getRegister("(HL)")));
		opcodes[0xAF] = create("XOR A", 4, 1, () -> cpu.xorN(cpu.getRegister("A")));

		// 0xB0 - 0xBF: OR/CP A,r
		opcodes[0xB0] = create("OR B", 4, 1, () -> cpu.orN("B"));
		opcodes[0xB1] = create("OR C", 4, 1, () -> cpu.orN("C"));
		opcodes[0xB2] = create("OR D", 4, 1, () -> cpu.orN("D"));
		opcodes[0xB3] = create("OR E", 4, 1, () -> cpu.orN("E"));
		opcodes[0xB4] = create("OR H", 4, 1, () -> cpu.orN("H"));
		opcodes[0xB5] = create("OR L", 4, 1, () -> cpu.orN("L"));
		opcodes[0xB6] = create("OR (HL)", 8, 1, () -> cpu.orN("(HL)"));
		opcodes[0xB7] = create("OR A", 4, 1, () -> cpu.orN("A"));
		opcodes[0xB8] = create("CP B", 4, 1, () -> cpu.cp(cpu.a, cpu.b));
		opcodes[0xB9] = create("CP C", 4, 1, () -> cpu.cp(cpu.a, cpu.c));
		opcodes[0xBA] = create("CP D", 4, 1, () -> cpu.cp(cpu.a, cpu.d));
		opcodes[0xBB] = create("CP E", 4, 1, () -> cpu.cp(cpu.a, cpu.e));
		opcodes[0xBC] = create("CP H", 4, 1, () -> cpu.cp(cpu.a, cpu.h));
		opcodes[0xBD] = create("CP L", 4, 1, () -> cpu.cp(cpu.a, cpu.l));
		opcodes[0xBE] = create("CP (HL)", 8, 1, () -> cpu.cp(cpu.a, cpu.getMem(cpu.get16BitRegister("HL"))));
		opcodes[0xBF] = create("CP A", 4, 1, () -> cpu.cp(cpu.a, cpu.a));

		// 0xC0 - 0xCF
		opcodes[0xC0] = create("RET NZ", 20, 0, () -> {
			if (!cpu.getFlag(Cpu.FLAG_ZERO)) {
				cpu.pc = cpu.pop();
			} else {
				cpu.pc += 1;
			}
		});
		opcodes[0xC1] = create("POP BC", 12, 1, () -> cpu.set16BitRegister("B", "C", cpu.pop()));
		opcodes[0xC2] = create("JP NZ,a16", 16, 0, () -> {
			if (!cpu.getFlag(Cpu.FLAG_ZERO)) {
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xC3] = create("JP a16", 16, 0, () -> {
			cpu.pc = cpu.get16bitValue(cpu.pc + 1);
		});
		opcodes[0xC4] = create("CALL NZ,a16", 24, 0, () -> {
			if (!cpu.getFlag(Cpu.FLAG_ZERO)) {
				cpu.push(cpu.pc + 3);
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xC5] = create("PUSH BC", 16, 1, () -> cpu.push(cpu.get16BitRegister("BC")));
		opcodes[0xC6] = create("ADD A,d8", 8, 2, () -> cpu.addN(cpu.getMem(cpu.pc + 1)));
		opcodes[0xC7] = create("RST 00H", 16, 0, () -> {
			cpu.push(cpu.pc + 1);
			cpu.pc = 0x00;
		});
		opcodes[0xC8] = create("RET Z", 20, 0, () -> {
			if (cpu.getFlag(Cpu.FLAG_ZERO)) {
				cpu.pc = cpu.pop();
			} else {
				cpu.pc += 1;
			}
		});
		opcodes[0xC9] = create("RET", 16, 0, () -> {
			cpu.pc = cpu.pop();
		});
		opcodes[0xCA] = create("JP Z,a16", 16, 0, () -> {
			if (cpu.getFlag(Cpu.FLAG_ZERO)) {
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xCB] = create("PREFIX CB", 4, 1, () -> {
			// This is handled specially in exec()
		});
		opcodes[0xCC] = create("CALL Z,a16", 24, 0, () -> {
			if (cpu.getFlag(Cpu.FLAG_ZERO)) {
				cpu.push(cpu.pc + 3);
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xCD] = create("CALL a16", 24, 0, () -> {
			cpu.push(cpu.pc + 3);
			cpu.pc = cpu.get16bitValue(cpu.pc + 1);
		});
		opcodes[0xCE] = create("ADC A,d8", 8, 2, () -> cpu.adcN(cpu.getMem(cpu.pc + 1)));
		opcodes[0xCF] = create("RST 08H", 16, 0, () -> {
			cpu.push(cpu.pc + 1);
			cpu.pc = 0x08;
		});

		// 0xD0 - 0xDF
		opcodes[0xD0] = create("RET NC", 20, 0, () -> {
			if (!cpu.getFlag(Cpu.FLAG_CARRY)) {
				cpu.pc = cpu.pop();
			} else {
				cpu.pc += 1;
			}
		});
		opcodes[0xD1] = create("POP DE", 12, 1, () -> cpu.set16BitRegister("D", "E", cpu.pop()));
		opcodes[0xD2] = create("JP NC,a16", 16, 0, () -> {
			if (!cpu.getFlag(Cpu.FLAG_CARRY)) {
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xD3] = create("ILLEGAL_D3", 4, 1, () -> {});
		opcodes[0xD4] = create("CALL NC,a16", 24, 0, () -> {
			if (!cpu.getFlag(Cpu.FLAG_CARRY)) {
				cpu.push(cpu.pc + 3);
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xD5] = create("PUSH DE", 16, 1, () -> cpu.push(cpu.get16BitRegister("DE")));
		opcodes[0xD6] = create("SUB d8", 8, 2, () -> cpu.subN(cpu.getMem(cpu.pc + 1)));
		opcodes[0xD7] = create("RST 10H", 16, 0, () -> {
			cpu.push(cpu.pc + 1);
			cpu.pc = 0x10;
		});
		opcodes[0xD8] = create("RET C", 20, 0, () -> {
			if (cpu.getFlag(Cpu.FLAG_CARRY)) {
				cpu.pc = cpu.pop();
			} else {
				cpu.pc += 1;
			}
		});
		opcodes[0xD9] = create("RETI", 16, 0, () -> {
			cpu.pc = cpu.pop();
			if (interrupts != null) {
				interrupts.enableInterruptsImmediate();
			}
		});
		opcodes[0xDA] = create("JP C,a16", 16, 0, () -> {
			if (cpu.getFlag(Cpu.FLAG_CARRY)) {
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xDB] = create("ILLEGAL_DB", 4, 1, () -> {});
		opcodes[0xDC] = create("CALL C,a16", 24, 0, () -> {
			if (cpu.getFlag(Cpu.FLAG_CARRY)) {
				cpu.push(cpu.pc + 3);
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xDD] = create("ILLEGAL_DD", 4, 1, () -> {});
		opcodes[0xDE] = create("SBC A,d8", 8, 2, () -> cpu.sbcN(cpu.getMem(cpu.pc + 1)));
		opcodes[0xDF] = create("RST 18H", 16, 0, () -> {
			cpu.push(cpu.pc + 1);
			cpu.pc = 0x18;
		});

		// 0xE0 - 0xEF
		opcodes[0xE0] = create("LDH (a8),A", 12, 2, () -> {
			cpu.setMem(0xFF00 + cpu.getMem(cpu.pc + 1), cpu.a);
		});
		opcodes[0xE1] = create("POP HL", 12, 1, () -> cpu.set16BitRegister("H", "L", cpu.pop()));
		opcodes[0xE2] = create("LD (C),A", 8, 1, () -> {
			cpu.setMem(0xFF00 + cpu.c, cpu.a);
		});
		opcodes[0xE3] = create("ILLEGAL_E3", 4, 1, () -> {});
		opcodes[0xE4] = create("ILLEGAL_E4", 4, 1, () -> {});
		opcodes[0xE5] = create("PUSH HL", 16, 1, () -> cpu.push(cpu.get16BitRegister("HL")));
		opcodes[0xE6] = create("AND d8", 8, 2, () -> cpu.andN(cpu.getMem(cpu.pc + 1)));
		opcodes[0xE7] = create("RST 20H", 16, 0, () -> {
			cpu.push(cpu.pc + 1);
			cpu.pc = 0x20;
		});
		opcodes[0xE8] = create("ADD SP,r8", 16, 2, () -> {
			byte n = (byte) cpu.getMem(cpu.pc + 1);
			int result = cpu.sp + n;
			cpu.toogleFlag(Cpu.FLAG_ZERO, false);
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, false);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, ((cpu.sp & 0x0F) + (n & 0x0F)) > 0x0F);
			cpu.toogleFlag(Cpu.FLAG_CARRY, ((cpu.sp & 0xFF) + (n & 0xFF)) > 0xFF);
			cpu.sp = result & 0xFFFF;
		});
		opcodes[0xE9] = create("JP (HL)", 4, 0, () -> {
			cpu.pc = cpu.get16BitRegister("HL");
		});
		opcodes[0xEA] = create("LD (a16),A", 16, 3, () -> {
			cpu.setMem(cpu.get16bitValue(cpu.pc + 1), cpu.a);
		});
		opcodes[0xEB] = create("ILLEGAL_EB", 4, 1, () -> {});
		opcodes[0xEC] = create("ILLEGAL_EC", 4, 1, () -> {});
		opcodes[0xED] = create("ILLEGAL_ED", 4, 1, () -> {});
		opcodes[0xEE] = create("XOR d8", 8, 2, () -> cpu.xorN(cpu.getMem(cpu.pc + 1)));
		opcodes[0xEF] = create("RST 28H", 16, 0, () -> {
			cpu.push(cpu.pc + 1);
			cpu.pc = 0x28;
		});

		// 0x100 - 0x10F
		opcodes[0xF0] = create("LDH A,(a8)", 12, 2, () -> {
			cpu.a = cpu.getMem(0xFF00 + cpu.getMem(cpu.pc + 1));
		});
		opcodes[0xF1] = create("POP AF", 12, 1, () -> {
			cpu.set16BitRegister("A", "F", cpu.pop() & 0xFFF0);
		});
		opcodes[0xF2] = create("LD A,(C)", 8, 1, () -> {
			cpu.a = cpu.getMem(0xFF00 + cpu.c);
		});
		opcodes[0xF3] = create("DI", 4, 1, () -> {
			if (interrupts != null) {
				interrupts.disableInterrupts();
			}
		});
		opcodes[0xF4] = create("ILLEGAL_F4", 4, 1, () -> {});
		opcodes[0xF5] = create("PUSH AF", 16, 1, () -> cpu.push(cpu.get16BitRegister("AF")));
		opcodes[0xF6] = create("OR d8", 8, 2, () -> {
			short val = cpu.getMem(cpu.pc + 1);
			val = (short) (val | cpu.getRegister("A"));
			cpu.toogleFlag(Cpu.FLAG_ZERO, val == 0);
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, false);
			cpu.toogleFlag(Cpu.FLAG_CARRY, false);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, false);
			cpu.setRegister("A", val);
		});
		opcodes[0xF7] = create("RST 30H", 16, 0, () -> {
			cpu.push(cpu.pc + 1);
			cpu.pc = 0x30;
		});
		opcodes[0xF8] = create("LD HL,SP+r8", 12, 2, () -> {
			byte n = (byte) cpu.getMem(cpu.pc + 1);
			int result = cpu.sp + n;
			cpu.toogleFlag(Cpu.FLAG_ZERO, false);
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, false);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, ((cpu.sp & 0x0F) + (n & 0x0F)) > 0x0F);
			cpu.toogleFlag(Cpu.FLAG_CARRY, ((cpu.sp & 0xFF) + (n & 0xFF)) > 0xFF);
			cpu.set16BitRegister("H", "L", result & 0xFFFF);
		});
		opcodes[0xF9] = create("LD SP,HL", 8, 1, () -> {
			cpu.sp = cpu.get16BitRegister("HL");
		});
		opcodes[0xFA] = create("LD A,(a16)", 16, 3, () -> {
			cpu.setRegister("A", cpu.getMem(cpu.get16bitValue(cpu.pc + 1)));
		});
		opcodes[0xFB] = create("EI", 4, 1, () -> {
			if (interrupts != null) {
				interrupts.enableInterrupts();
			}
		});
		opcodes[0xFC] = create("ILLEGAL_FC", 4, 1, () -> {});
		opcodes[0xFD] = create("ILLEGAL_FD", 4, 1, () -> {});
		opcodes[0xFE] = create("CP d8", 8, 2, () -> cpu.cp(cpu.a, cpu.getMem(cpu.pc + 1)));
		opcodes[0xFF] = create("RST 38H", 16, 0, () -> {
			cpu.push(cpu.pc + 1);
			cpu.pc = 0x38;
		});
	}

	private void initCBOpcodes() {
		String[] regs = {"B", "C", "D", "E", "H", "L", "(HL)", "A"};
		int[] cycles = {8, 8, 8, 8, 8, 8, 16, 8};

		// 0x00-0x07: RLC r
		for (int i = 0; i < 8; i++) {
			final String reg = regs[i];
			final int c = cycles[i];
			opcodesCB[0x00 + i] = create("RLC " + reg, c, 2, () -> cpu.rlc(reg));
		}

		// 0x08-0x0F: RRC r
		for (int i = 0; i < 8; i++) {
			final String reg = regs[i];
			final int c = cycles[i];
			opcodesCB[0x08 + i] = create("RRC " + reg, c, 2, () -> cpu.rrc(reg));
		}

		// 0x10-0x17: RL r
		for (int i = 0; i < 8; i++) {
			final String reg = regs[i];
			final int c = cycles[i];
			opcodesCB[0x10 + i] = create("RL " + reg, c, 2, () -> cpu.rl(reg));
		}

		// 0x18-0x1F: RR r
		for (int i = 0; i < 8; i++) {
			final String reg = regs[i];
			final int c = cycles[i];
			opcodesCB[0x18 + i] = create("RR " + reg, c, 2, () -> cpu.rr(reg));
		}

		// 0x20-0x27: SLA r
		for (int i = 0; i < 8; i++) {
			final String reg = regs[i];
			final int c = cycles[i];
			opcodesCB[0x20 + i] = create("SLA " + reg, c, 2, () -> cpu.sla(reg));
		}

		// 0x28-0x2F: SRA r
		for (int i = 0; i < 8; i++) {
			final String reg = regs[i];
			final int c = cycles[i];
			opcodesCB[0x28 + i] = create("SRA " + reg, c, 2, () -> cpu.sra(reg));
		}

		// 0x30-0x37: SWAP r
		for (int i = 0; i < 8; i++) {
			final String reg = regs[i];
			final int c = cycles[i];
			opcodesCB[0x30 + i] = create("SWAP " + reg, c, 2, () -> {
				cpu.setRegister(reg, cpu.swap(cpu.getRegister(reg)));
			});
		}

		// 0x38-0x3F: SRL r
		for (int i = 0; i < 8; i++) {
			final String reg = regs[i];
			final int c = cycles[i];
			opcodesCB[0x38 + i] = create("SRL " + reg, c, 2, () -> {
				cpu.setRegister(reg, cpu.srl(cpu.getRegister(reg)));
			});
		}

		// 0x40-0x7F: BIT b,r
		for (int bit = 0; bit < 8; bit++) {
			for (int i = 0; i < 8; i++) {
				final String reg = regs[i];
				final int c = cycles[i];
				final int b = bit;
				opcodesCB[0x40 + bit * 8 + i] = create("BIT " + bit + "," + reg, c, 2, () -> {
					short val = cpu.getRegister(reg);
					cpu.toogleFlag(Cpu.FLAG_ZERO, !cpu.getBit(val, b));
					cpu.toogleFlag(Cpu.FLAG_SUBTRACT, false);
					cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, true);
				});
			}
		}

		// 0x80-0xBF: RES b,r
		for (int bit = 0; bit < 8; bit++) {
			for (int i = 0; i < 8; i++) {
				final String reg = regs[i];
				final int c = cycles[i];
				final int b = bit;
				opcodesCB[0x80 + bit * 8 + i] = create("RES " + bit + "," + reg, c, 2, () -> {
					short val = cpu.getRegister(reg);
					val = (short) cpu.resetBit(val, b);
					cpu.setRegister(reg, val);
				});
			}
		}

		// 0xC0-0xFF: SET b,r
		for (int bit = 0; bit < 8; bit++) {
			for (int i = 0; i < 8; i++) {
				final String reg = regs[i];
				final int c = cycles[i];
				final int b = bit;
				opcodesCB[0xC0 + bit * 8 + i] = create("SET " + bit + "," + reg, c, 2, () -> {
					short val = cpu.getRegister(reg);
					val = (short) cpu.setBit(val, b);
					cpu.setRegister(reg, val);
				});
			}
		}
	}

	public int exec() {
		Opcode opcode = null;
		int pc = cpu.pc;
		short i = cpu.getMem(pc);
		boolean isCB = i == 0xCB;
		
		if (isCB) {
			i = cpu.getMem(pc + 1);
			opcode = opcodesCB[i];
		} else {
			opcode = opcodes[i];
		}

		if (opcode == null || opcode.cycles == 0) {
			throw new RuntimeException("Unknown opcode: " + Integer.toHexString(i).toUpperCase() + 
				" at PC: " + Integer.toHexString(cpu.pc).toUpperCase() + " CB: " + isCB);
		}

		try {
			opcode.code.exec();
			cpu.pc += opcode.lenght;
			return opcode.cycles;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(
					"Error executing opcode: " + Integer.toHexString(i) + " - " + opcode.name + " : " + e.getMessage());
		}
	}

	public Opcode getOpcode(int index, boolean cb) {
		if (cb) {
			return opcodesCB[index];
		} else {
			return opcodes[index];
		}
	}

	private Opcode create(String name, int cycles, int lenght, Code code) {
		return new Opcode(name, cycles, lenght, code);
	}
}
