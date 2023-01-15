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

		public Opcode(String name, int cyckles, int lenght, Code code) {
			this.name = name;
			this.cycles = cyckles;
			this.code = code;
			this.lenght = lenght;
		}
	}

	// private static Logger LOG = Logger.getLogger(Opcodes.class.getName());
	private final Cpu cpu;
	private Opcode[] opcodes;
	private Opcode[] opcodesCB;

	public Opcodes(Cpu cpu) {
		this.cpu = cpu;
		opcodes = new Opcode[0x100];
		opcodesCB = new Opcode[0x100];

		opcodes[0x00] = create("NOP", 1, 4, () -> {

		});
		opcodes[0x01] = create("LD BC,d16", 3, 12, () -> {
			cpu.set16BitRegister("B", "C", cpu.get16bitValue(cpu.pc + 1));
		});
		opcodes[0x02] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x03] = create("INC BC", 1, 8, () -> {
			cpu.set16BitRegister("B", "C", cpu.add16Bit(cpu.get16BitRegister("BC"), 1, false));
		});
		opcodes[0x04] = create("INC B", 1, 4, () -> {
			cpu.inc("B");
		});
		opcodes[0x05] = create("DEC B", 1, 4, () -> {
			cpu.dec("B");
		});
		opcodes[0x06] = create("LD B,d8", 2, 8, () -> {
			cpu.b = cpu.rom[cpu.pc + 1];
		});
		opcodes[0x07] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x08] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x09] = create("ADD HL,BC", 1, 8, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), cpu.get16BitRegister("BC"), true));
		});
		opcodes[0x0A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x0B] = create("DEC BC", 1, 8, () -> {
			cpu.set16BitRegister("B", "C", cpu.sub16Bit(cpu.get16BitRegister("BC"), 1));
		});
		opcodes[0x0C] = create("INC C", 1, 4, () -> {
			cpu.inc("C");
		});
		opcodes[0x0D] = create("DEC C", 1, 4, () -> {
			cpu.dec("C");
		});
		opcodes[0x0E] = create("LD C,d8", 2, 8, () -> {
			cpu.c = cpu.rom[cpu.pc + 1];
		});
		opcodes[0x0F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x10] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x11] = create("LD DE,d16", 3, 12, () -> {
			cpu.set16BitRegister("D", "E", cpu.get16bitValue(cpu.pc + 1));
		});
		opcodes[0x12] = create("LD (DE),A", 1, 8, () -> {
			cpu.setRegister("(DE)", cpu.getRegister("A"));
		});
		opcodes[0x13] = create("INC DE", 1, 8, () -> {
			cpu.set16BitRegister("D", "E", cpu.add16Bit(cpu.get16BitRegister("DE"), 1, false));
		});
		opcodes[0x14] = create("INC D", 1, 4, () -> {
			cpu.inc("D");
		});
		opcodes[0x15] = create("DEC D", 1, 4, () -> {
			cpu.dec("D");
		});
		opcodes[0x16] = create("LD D,d8", 2, 8, () -> {
			cpu.d = cpu.rom[cpu.pc + 1];
		});
		opcodes[0x17] = create("RLA", 1, 4, () -> {
			cpu.rl("A");
		});
		opcodes[0x18] = create("JR r8", 2, 12, () -> {
			byte i = (byte) cpu.rom[cpu.pc + 1];
			cpu.pc += i;
		});
		opcodes[0x19] = create("ADD HL,DE", 1, 8, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), cpu.get16BitRegister("DE"), true));
		});
		opcodes[0x1A] = create("LD A,(DE)", 1, 8, () -> {
			cpu.setRegister("A", cpu.rom[cpu.get16BitRegister("DE")]);
		});
		opcodes[0x1B] = create("DEC DE", 1, 8, () -> {
			cpu.set16BitRegister("D", "E", cpu.sub16Bit(cpu.get16BitRegister("DE"), 1));
		});
		opcodes[0x1C] = create("INC E", 1, 4, () -> {
			cpu.inc("E");
		});
		opcodes[0x1D] = create("DEC E", 1, 4, () -> {
			cpu.dec("E");
		});
		opcodes[0x1E] = create("LD E,d8", 2, 8, () -> {
			cpu.e = cpu.rom[cpu.pc + 1];
		});
		opcodes[0x1F] = create("RRA", 1, 4, () -> {
			cpu.rr("A");
		});
		opcodes[0x20] = create("JR NZ,r8", 2, 12, () -> {
			if (!cpu.getFlag(Cpu.FLAG_ZERO)) {
				byte jp = (byte) cpu.rom[cpu.pc + 1]; // signed byte so we can jump both fwd and bwd
				cpu.pc += jp;
			}
		});
		opcodes[0x21] = create("LD HL,d16", 3, 12, () -> {
			cpu.set16BitRegister("H", "L", cpu.get16bitValue(cpu.pc + 1));
		});
		opcodes[0x22] = create("LD (HL+),A", 1, 8, () -> {
			int hl = cpu.get16BitRegister("HL");
			cpu.rom[hl] = cpu.getRegister("A");
			hl++;
			cpu.set16BitRegister("H", "L", hl);
		});
		opcodes[0x23] = create("INC HL", 1, 8, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), 1, false));
		});
		opcodes[0x24] = create("INC H", 1, 4, () -> {
			cpu.inc("H");
		});
		opcodes[0x25] = create("DEC H", 1, 4, () -> {
			cpu.dec("H");
		});
		opcodes[0x26] = create("LD H,d8", 2, 8, () -> {
			cpu.h = cpu.rom[cpu.pc + 1];
		});
		opcodes[0x27] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x28] = create("JR Z,r8", 2, 12, () -> {
			if (cpu.getFlag(Cpu.FLAG_ZERO)) {
				byte jp = (byte) cpu.rom[cpu.pc + 1]; // signed byte so we can jump both fwd and bwd
				cpu.pc += jp;
			}
		});
		opcodes[0x29] = create("ADD HL,HL", 1, 8, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), cpu.get16BitRegister("HL"), true));
		});
		opcodes[0x2A] = create("LD A,(HL+)", 1, 8, () -> {
			int hl = cpu.get16BitRegister("HL");
			cpu.setRegister("A", cpu.getMem(hl));
			cpu.set16BitRegister("H", "L", cpu.add16Bit(hl, 1, false));
		});
		opcodes[0x2B] = create("DEC HL", 1, 8, () -> {
			cpu.set16BitRegister("H", "L", cpu.sub16Bit(cpu.get16BitRegister("HL"), 1));
		});
		opcodes[0x2C] = create("INC L", 1, 4, () -> {
			cpu.inc("L");
		});
		opcodes[0x2D] = create("DEC L", 1, 4, () -> {
			cpu.dec("L");
		});
		opcodes[0x2E] = create("LD L,d8", 2, 8, () -> {
			cpu.setRegister("L", cpu.rom[cpu.pc + 1]);
		});
		opcodes[0x2F] = create("CPL", 1, 4, () -> {
			cpu.setRegister("A", cpu.cpl(cpu.getRegister("A")));
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, true);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, true);
		});
		opcodes[0x30] = create("JR NC,r8", 2, 12, () -> {
			if (!cpu.getFlag(Cpu.FLAG_CARRY)) {
				byte jp = (byte) cpu.rom[cpu.pc + 1]; // signed byte so we can jump both fwd and bwd
				cpu.pc += jp;
			}
		});
		opcodes[0x31] = create("LD SP,d16", 3, 12, () -> {
			int val = cpu.get16bitValue(cpu.pc + 1);
			cpu.sp = val;
		});
		opcodes[0x32] = create("LD (HL-),A", 1, 8, () -> {
			int hl = cpu.get16BitRegister("HL");
			cpu.rom[hl] = cpu.a;

			hl--;
			cpu.set16BitRegister("H", "L", hl);
		});
		opcodes[0x33] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x34] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x35] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x36] = create("LD (HL),d8", 2, 12, () -> {
			cpu.setMem(cpu.get16BitRegister("HL"), cpu.getMem(cpu.pc + 1));
		});
		opcodes[0x37] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x38] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x39] = create("ADD HL,SP", 1, 8, () -> {
			cpu.set16BitRegister("H", "L", cpu.add16Bit(cpu.get16BitRegister("HL"), cpu.sp, true));
		});
		opcodes[0x3A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x3B] = create("DEC SP", 1, 8, () -> {
			cpu.sp = cpu.sub16Bit(cpu.sp, 1);
		});
		opcodes[0x3C] = create("INC A", 1, 4, () -> {
			cpu.inc("A");
		});
		opcodes[0x3D] = create("DEC A", 1, 4, () -> {
			cpu.dec("A");
		});
		opcodes[0x3E] = create("LD A,d8", 2, 8, () -> {
			cpu.a = cpu.rom[cpu.pc + 1];
		});
		opcodes[0x3F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x40] = create("LD B,B", 1, 4, () -> {
			cpu.b = cpu.b;
		});
		opcodes[0x41] = create("LD B,C", 1, 4, () -> {
			cpu.b = cpu.c;
		});
		opcodes[0x42] = create("LD B,D", 1, 4, () -> {
			cpu.b = cpu.d;
		});
		opcodes[0x43] = create("LD B,E", 1, 4, () -> {
			cpu.b = cpu.e;
		});
		opcodes[0x44] = create("LD B,H", 1, 4, () -> {
			cpu.b = cpu.h;
		});
		opcodes[0x45] = create("LD B,L", 1, 4, () -> {
			cpu.b = cpu.l;
		});
		opcodes[0x46] = create("LD B,(HL)", 1, 8, () -> {
			cpu.setRegister("B", cpu.getMem(cpu.get16BitRegister("HL")));
		});
		opcodes[0x47] = create("LD B,A", 1, 4, () -> {
			cpu.b = cpu.a;
		});
		opcodes[0x48] = create("LD C,B", 1, 4, () -> {
			cpu.c = cpu.b;
		});
		opcodes[0x49] = create("LD C,C", 1, 4, () -> {
			cpu.c = cpu.c;
		});
		opcodes[0x4A] = create("LD C,D", 1, 4, () -> {
			cpu.c = cpu.d;
		});
		opcodes[0x4B] = create("LD C,E", 1, 4, () -> {
			cpu.c = cpu.e;
		});
		opcodes[0x4C] = create("LD C,H", 1, 4, () -> {
			cpu.c = cpu.h;
		});
		opcodes[0x4D] = create("LD C,L", 1, 4, () -> {
			cpu.c = cpu.l;
		});
		opcodes[0x4E] = create("LD C,(HL)", 1, 8, () -> {
			cpu.setRegister("C", cpu.getMem(cpu.get16BitRegister("HL")));
		});
		opcodes[0x4F] = create("LD C,A", 1, 4, () -> {
			cpu.c = cpu.a;
		});
		opcodes[0x50] = create("LD D,B", 1, 4, () -> {
			cpu.d = cpu.b;
		});
		opcodes[0x51] = create("LD D,C", 1, 4, () -> {
			cpu.d = cpu.c;
		});
		opcodes[0x52] = create("LD D,D", 1, 4, () -> {
			cpu.d = cpu.d;
		});
		opcodes[0x53] = create("LD D,E", 1, 4, () -> {
			cpu.d = cpu.e;
		});
		opcodes[0x54] = create("LD D,H", 1, 4, () -> {
			cpu.d = cpu.h;
		});
		opcodes[0x55] = create("LD D,L", 1, 4, () -> {
			cpu.d = cpu.l;
		});
		opcodes[0x56] = create("LD D,(HL)", 1, 8, () -> {
			cpu.setRegister("D", cpu.getMem(cpu.get16BitRegister("HL")));
		});
		opcodes[0x57] = create("LD D,A", 1, 4, () -> {
			cpu.d = cpu.a;
		});
		opcodes[0x58] = create("LD E,B", 1, 4, () -> {
			cpu.e = cpu.b;
		});
		opcodes[0x59] = create("LD E,C", 1, 4, () -> {
			cpu.e = cpu.c;
		});
		opcodes[0x5A] = create("LD E,D", 1, 4, () -> {
			cpu.e = cpu.d;
		});
		opcodes[0x5B] = create("LD E,E", 1, 4, () -> {
			cpu.e = cpu.e;
		});
		opcodes[0x5C] = create("LD E,H", 1, 4, () -> {
			cpu.e = cpu.h;
		});
		opcodes[0x5D] = create("LD E,L", 1, 4, () -> {
			cpu.e = cpu.l;
		});
		opcodes[0x5E] = create("LD E,(HL)", 1, 8, () -> {
			cpu.setRegister("E", cpu.getMem(cpu.get16BitRegister("HL")));
		});
		opcodes[0x5F] = create("LD E,A", 1, 4, () -> {
			cpu.e = cpu.a;
		});
		opcodes[0x60] = create("LD H,B", 1, 4, () -> {
			cpu.h = cpu.b;
		});
		opcodes[0x61] = create("LD H,C", 1, 4, () -> {
			cpu.h = cpu.c;
		});
		opcodes[0x62] = create("LD H,D", 1, 4, () -> {
			cpu.h = cpu.d;
		});
		opcodes[0x63] = create("LD H,E", 1, 4, () -> {
			cpu.h = cpu.e;
		});
		opcodes[0x64] = create("LD H,H", 1, 4, () -> {
			cpu.h = cpu.h;
		});
		opcodes[0x65] = create("LD H,L", 1, 4, () -> {
			cpu.h = cpu.l;
		});
		opcodes[0x66] = create("LD H,(HL)", 1, 8, () -> {
			cpu.setRegister("H", cpu.getMem(cpu.get16BitRegister("HL")));
		});
		opcodes[0x67] = create("LD H,A", 1, 4, () -> {
			cpu.h = cpu.a;
		});
		opcodes[0x68] = create("LD L,B", 1, 4, () -> {
			cpu.l = cpu.b;
		});
		opcodes[0x69] = create("LD L,C", 1, 4, () -> {
			cpu.l = cpu.c;
		});
		opcodes[0x6A] = create("LD L,D", 1, 4, () -> {
			cpu.l = cpu.d;
		});
		opcodes[0x6B] = create("LD L,E", 1, 4, () -> {
			cpu.l = cpu.e;
		});
		opcodes[0x6C] = create("LD L,H", 1, 4, () -> {
			cpu.l = cpu.h;
		});
		opcodes[0x6D] = create("LD L,L", 1, 4, () -> {
			cpu.l = cpu.l;
		});
		opcodes[0x6E] = create("LD L,(HL)", 1, 8, () -> {
			cpu.setRegister("L", cpu.getRegister("(HL)"));
		});
		opcodes[0x6F] = create("LD L,A", 1, 4, () -> {
			cpu.l = cpu.a;
		});
		opcodes[0x70] = create("LD (HL),B", 1, 8, () -> {
			cpu.setRegister("(HL)", cpu.getRegister("B"));
		});
		opcodes[0x71] = create("LD (HL),C", 1, 8, () -> {
			cpu.setRegister("(HL)", cpu.getRegister("C"));
		});
		opcodes[0x72] = create("LD (HL),D", 1, 8, () -> {
			cpu.setRegister("(HL)", cpu.getRegister("D"));
		});
		opcodes[0x73] = create("LD (HL),E", 1, 8, () -> {
			cpu.setRegister("(HL)", cpu.getRegister("E"));
		});
		opcodes[0x74] = create("LD (HL),H", 1, 8, () -> {
			cpu.setRegister("(HL)", cpu.getRegister("H"));
		});
		opcodes[0x75] = create("LD (HL),L", 1, 8, () -> {
			cpu.setRegister("(HL)", cpu.getRegister("L"));
		});
		opcodes[0x76] = create("HALT", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x77] = create("LD (HL),A", 1, 8, () -> {
			cpu.rom[cpu.get16BitRegister("HL")] = cpu.a;
		});
		opcodes[0x78] = create("LD A,B", 1, 4, () -> {
			cpu.a = cpu.b;
		});
		opcodes[0x79] = create("LD A,C", 1, 4, () -> {
			cpu.a = cpu.c;
		});
		opcodes[0x7A] = create("LD A,D", 1, 4, () -> {
			cpu.a = cpu.d;
		});
		opcodes[0x7B] = create("LD A,E", 1, 4, () -> {
			cpu.a = cpu.e;
		});
		opcodes[0x7C] = create("LD A,H", 1, 4, () -> {
			cpu.a = cpu.h;
		});
		opcodes[0x7D] = create("LD A,L", 1, 4, () -> {
			cpu.a = cpu.l;
		});
		opcodes[0x7E] = create("LD A,(HL)", 1, 8, () -> {
			cpu.setRegister("A", cpu.getRegister("(HL)"));
		});
		opcodes[0x7F] = create("LD A,A", 1, 4, () -> {
			cpu.a = cpu.a;
		});
		opcodes[0x80] = create("ADD A,B", 1, 4, () -> {
			cpu.addN(cpu.getRegister("B"));
		});
		opcodes[0x81] = create("ADD A,C", 1, 4, () -> {
			cpu.addN(cpu.getRegister("C"));
		});
		opcodes[0x82] = create("ADD A,D", 1, 4, () -> {
			cpu.addN(cpu.getRegister("D"));
		});
		opcodes[0x83] = create("ADD A,E", 1, 4, () -> {
			cpu.addN(cpu.getRegister("E"));
		});
		opcodes[0x84] = create("ADD A,H", 1, 4, () -> {
			cpu.addN(cpu.getRegister("H"));
		});
		opcodes[0x85] = create("ADD A,L", 1, 4, () -> {
			cpu.addN(cpu.getRegister("L"));
		});
		opcodes[0x86] = create("ADD A,(HL)", 1, 8, () -> {
			cpu.addN(cpu.getRegister("(HL)"));
		});
		opcodes[0x87] = create("ADD A,A", 1, 4, () -> {
			cpu.addN(cpu.getRegister("A"));
		});
		opcodes[0x88] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x89] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x8A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x8B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x8C] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x8D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x8E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x8F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x90] = create("SUB B", 1, 4, () -> {
			cpu.subN(cpu.getRegister("B"));
		});
		opcodes[0x91] = create("SUB C", 1, 4, () -> {
			cpu.subN(cpu.getRegister("C"));
		});
		opcodes[0x92] = create("SUB D", 1, 4, () -> {
			cpu.subN(cpu.getRegister("D"));
		});
		opcodes[0x93] = create("SUB E", 1, 4, () -> {
			cpu.subN(cpu.getRegister("E"));
		});
		opcodes[0x94] = create("SUB H", 1, 4, () -> {
			cpu.subN(cpu.getRegister("H"));
		});
		opcodes[0x95] = create("SUB L", 1, 4, () -> {
			cpu.subN(cpu.getRegister("L"));
		});
		opcodes[0x96] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x97] = create("SUB A", 1, 4, () -> {
			cpu.subN(cpu.getRegister("A"));
		});
		opcodes[0x98] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x99] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x9A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x9B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x9C] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x9D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x9E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0x9F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xA0] = create("AND B", 1, 4, () -> {
			cpu.andN(cpu.getRegister("B"));
		});
		opcodes[0xA1] = create("AND C", 1, 4, () -> {
			cpu.andN(cpu.getRegister("C"));
		});
		opcodes[0xA2] = create("AND D", 1, 4, () -> {
			cpu.andN(cpu.getRegister("D"));
		});
		opcodes[0xA3] = create("AND E", 1, 4, () -> {
			cpu.andN(cpu.getRegister("E"));
		});
		opcodes[0xA4] = create("AND H", 1, 4, () -> {
			cpu.andN(cpu.getRegister("H"));
		});
		opcodes[0xA5] = create("AND L", 1, 4, () -> {
			cpu.andN(cpu.getRegister("L"));
		});
		opcodes[0xA6] = create("AND (HL)", 1, 8, () -> {
			cpu.andN(cpu.getRegister("(HL)"));
		});
		opcodes[0xA7] = create("AND A", 1, 4, () -> {
			cpu.andN(cpu.getRegister("A"));
		});
		opcodes[0xA8] = create("XOR B", 1, 4, () -> {
			cpu.xorN(cpu.getRegister("B"));
		});
		opcodes[0xA9] = create("XOR C", 1, 4, () -> {
			cpu.xorN(cpu.getRegister("C"));
		});
		opcodes[0xAA] = create("XOR D", 1, 4, () -> {
			cpu.xorN(cpu.getRegister("D"));
		});
		opcodes[0xAB] = create("XOR E", 1, 4, () -> {
			cpu.xorN(cpu.getRegister("E"));
		});
		opcodes[0xAC] = create("XOR H", 1, 4, () -> {
			cpu.xorN(cpu.getRegister("H"));
		});
		opcodes[0xAD] = create("XOR L", 1, 4, () -> {
			cpu.xorN(cpu.getRegister("L"));
		});
		opcodes[0xAE] = create("XOR (HL)", 1, 8, () -> {
			cpu.xorN(cpu.getRegister("(HL)"));
		});
		opcodes[0xAF] = create("XOR A", 1, 4, () -> {
			cpu.xorN(cpu.getRegister("A"));
		});
		opcodes[0xB0] = create("OR B", 1, 4, () -> {
			cpu.orN("B");
		});
		opcodes[0xB1] = create("OR C", 1, 4, () -> {
			cpu.orN("C");
		});
		opcodes[0xB2] = create("OR D", 1, 4, () -> {
			cpu.orN("D");
		});
		opcodes[0xB3] = create("OR E", 1, 4, () -> {
			cpu.orN("E");
		});
		opcodes[0xB4] = create("OR H", 1, 4, () -> {
			cpu.orN("H");
		});
		opcodes[0xB5] = create("OR L", 1, 4, () -> {
			cpu.orN("L");
		});
		opcodes[0xB6] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xB7] = create("OR A", 1, 4, () -> {
			cpu.orN("A");
		});
		opcodes[0xB8] = create("CP A,B", 1, 4, () -> {
			cpu.cp(cpu.a, cpu.b);
		});
		opcodes[0xB9] = create("CP A,C", 1, 4, () -> {
			cpu.cp(cpu.a, cpu.c);
		});
		opcodes[0xBA] = create("CP A,D", 1, 4, () -> {
			cpu.cp(cpu.a, cpu.d);
		});
		opcodes[0xBB] = create("CP A,E", 1, 4, () -> {
			cpu.cp(cpu.a, cpu.e);
		});
		opcodes[0xBC] = create("CP A,H", 1, 4, () -> {
			cpu.cp(cpu.a, cpu.h);
		});
		opcodes[0xBD] = create("CP A,L", 1, 4, () -> {
			cpu.cp(cpu.a, cpu.l);
		});
		opcodes[0xBE] = create("CP A,(HL)", 1, 8, () -> {
			cpu.cp(cpu.a, cpu.getMem(cpu.get16BitRegister("HL")));
		});
		opcodes[0xBF] = create("CP A,A", 1, 4, () -> {
			cpu.cp(cpu.a, cpu.a);
		});
		opcodes[0xC0] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xC1] = create("POP BC", 1, 12, () -> {
			cpu.set16BitRegister("B", "C", cpu.pop());
		});
		opcodes[0xC2] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xC3] = create("JP a16", 0, 16, () -> {
			cpu.pc = cpu.get16bitValue(cpu.pc + 1);
		});
		opcodes[0xC4] = create("CALL NZ,a16", 0, 24, () -> {
			if (!cpu.getFlag(Cpu.FLAG_ZERO)) {
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xC5] = create("PUSH BC", 1, 16, () -> {
			cpu.push(cpu.get16BitRegister("BC"));
		});
		opcodes[0xC6] = create("ADD A,d8", 2, 8, () -> {
			cpu.addN(cpu.getMem(cpu.pc + 1));
		});
		opcodes[0xC7] = create("RST 00H", 0, 16, () -> {
			cpu.push(cpu.pc);
			cpu.pc = 0x00;
		});
		opcodes[0xC8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xC9] = create("RET", 0, 16, () -> {
			cpu.pc = cpu.pop();
		});
		opcodes[0xCA] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xCB] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xCC] = create("CALL Z,a16", 0, 24, () -> {
			if (cpu.getFlag(Cpu.FLAG_ZERO)) {
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xCD] = create("CALL a16", 0, 24, () -> {
			cpu.push(cpu.pc + 3);
			cpu.pc = cpu.get16bitValue(cpu.pc + 1);
		});
		opcodes[0xCE] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xCF] = create("RST 08H", 0, 16, () -> {
			cpu.push(cpu.pc);
			cpu.pc = 0x08;
		});
		opcodes[0xD0] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xD1] = create("POP DE", 1, 12, () -> {
			cpu.set16BitRegister("D", "E", cpu.pop());
		});
		opcodes[0xD2] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xD3] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xD4] = create("CALL NC,a16", 0, 24, () -> {
			if (!cpu.getFlag(Cpu.FLAG_CARRY)) {
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xD5] = create("PUSH DE", 1, 16, () -> {
			cpu.push(cpu.get16BitRegister("DE"));
		});
		opcodes[0xD6] = create("SUB d8", 2, 8, () -> {
			cpu.subN(cpu.getMem(cpu.pc + 1));
		});
		opcodes[0xD7] = create("RST 10H", 0, 16, () -> {
			cpu.push(cpu.pc);
			cpu.pc = 0x10;
		});
		opcodes[0xD8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xD9] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xDA] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xDB] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xDC] = create("CALL C,a16", 0, 24, () -> {
			if (cpu.getFlag(Cpu.FLAG_CARRY)) {
				cpu.pc = cpu.get16bitValue(cpu.pc + 1);
			} else {
				cpu.pc += 3;
			}
		});
		opcodes[0xDD] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xDE] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xDF] = create("RST 18H", 0, 16, () -> {
			cpu.push(cpu.pc);
			cpu.pc = 0x18;
		});
		opcodes[0xE0] = create("LDH (a8),A", 2, 12, () -> {
			cpu.rom[0xFF00 + cpu.rom[cpu.pc + 1]] = cpu.a;
		});
		opcodes[0xE1] = create("POP HL", 1, 12, () -> {
			cpu.set16BitRegister("H", "L", cpu.pop());
		});
		opcodes[0xE2] = create("LD (C),A", 2, 8, () -> {
			cpu.rom[0xFF00 + cpu.c] = cpu.a;
		});
		opcodes[0xE3] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xE4] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xE5] = create("PUSH HL", 1, 16, () -> {
			cpu.push(cpu.get16BitRegister("HL"));
		});
		opcodes[0xE6] = create("AND d8", 2, 8, () -> {
			cpu.andN(cpu.getMem(cpu.pc + 1));
		});
		opcodes[0xE7] = create("RST 20H", 0, 16, () -> {
			cpu.push(cpu.pc);
			cpu.pc = 0x20;
		});
		opcodes[0xE8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xE9] = create("JP (HL)", 0, 4, () -> {
			cpu.pc = cpu.get16BitRegister("HL");
		});
		opcodes[0xEA] = create("LD (a16),A", 3, 16, () -> {
			cpu.rom[cpu.get16bitValue(cpu.pc + 1)] = cpu.a;
		});
		opcodes[0xEB] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xEC] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xED] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xEE] = create("XOR d8", 2, 8, () -> {
			cpu.xorN(cpu.getMem(cpu.pc + 1));
		});
		opcodes[0xEF] = create("RST 28H", 0, 16, () -> {
			cpu.push(cpu.pc);
			cpu.pc = 0x28;
		});
		opcodes[0xF0] = create("LDH A,(a8)", 2, 12, () -> {
			cpu.a = cpu.rom[0xFF00 + cpu.rom[cpu.pc + 1]];
		});
		opcodes[0xF1] = create("POP AF", 1, 12, () -> {
			cpu.set16BitRegister("A", "F", cpu.pop() & 0xFFF0);
		});
		opcodes[0xF2] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xF3] = create("DI", 1, 4, () -> {
			// TODO nått med interupts
		});
		opcodes[0xF4] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xF5] = create("PUSH AF", 1, 16, () -> {
			cpu.push(cpu.get16BitRegister("AF"));
		});
		opcodes[0xF6] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xF7] = create("RST 30H", 0, 16, () -> {
			cpu.push(cpu.pc);
			cpu.pc = 0x30;
		});
		opcodes[0xF8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xF9] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xFA] = create("LD A,(a16)", 3, 16, () -> {
			cpu.setRegister("A", (short) cpu.get16bitValue(cpu.pc + 1));
		});
		opcodes[0xFB] = create("EI", 1, 4, () -> {
			// TODO nått med interupts
		});
		opcodes[0xFC] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xFD] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodes[0xFE] = create("CP d8", 2, 8, () -> {
			cpu.cp(cpu.a, cpu.rom[cpu.pc + 1]);
		});
		opcodes[0xFF] = create("RST 38H", 0, 16, () -> {
			cpu.push(cpu.pc);
			cpu.pc = 0x38;
		});

		opcodesCB[0x00] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x01] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x02] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x03] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x04] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x05] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x06] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x07] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x08] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x09] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x0A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x0B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x0C] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x0D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x0E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x0F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x10] = create("RL B", 2, 8, () -> {
			cpu.rl("B");
		});
		opcodesCB[0x11] = create("RL C", 2, 8, () -> {
			cpu.rl("C");
		});
		opcodesCB[0x12] = create("RL D", 2, 8, () -> {
			cpu.rl("D");
		});
		opcodesCB[0x13] = create("RL E", 2, 8, () -> {
			cpu.rl("E");
		});
		opcodesCB[0x14] = create("RL H", 2, 8, () -> {
			cpu.rl("H");
		});
		opcodesCB[0x15] = create("RL L", 2, 8, () -> {
			cpu.rl("L");
		});
		opcodesCB[0x16] = create("RL (HL)", 2, 8, () -> {
			cpu.rl("(HL)");
		});
		opcodesCB[0x17] = create("RL A", 2, 8, () -> {
			cpu.rl("A");
		});
		opcodesCB[0x18] = create("RR B", 2, 8, () -> {
			cpu.rr("B");
		});
		opcodesCB[0x19] = create("RR C", 2, 8, () -> {
			cpu.rr("C");
		});
		opcodesCB[0x1A] = create("RR D", 2, 8, () -> {
			cpu.rr("D");
		});
		opcodesCB[0x1B] = create("RR E", 2, 8, () -> {
			cpu.rr("E");
		});
		opcodesCB[0x1C] = create("RR H", 2, 8, () -> {
			cpu.rr("H");
		});
		opcodesCB[0x1D] = create("RR L", 2, 8, () -> {
			cpu.rr("L");
		});
		opcodesCB[0x1E] = create("RR (HL)", 2, 8, () -> {
			cpu.rr("(HL)");
		});
		opcodesCB[0x1F] = create("RR A", 2, 8, () -> {
			cpu.rr("A");
		});
		opcodesCB[0x20] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x21] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x22] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x23] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x24] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x25] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x26] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x27] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x28] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x29] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x2A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x2B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x2C] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x2D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x2E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x2F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x30] = create("SWAP B", 2, 8, () -> {
			cpu.setRegister("B", cpu.swap(cpu.getRegister("B")));
		});
		opcodesCB[0x31] = create("SWAP C", 2, 8, () -> {
			cpu.setRegister("C", cpu.swap(cpu.getRegister("C")));
		});
		opcodesCB[0x32] = create("SWAP D", 2, 8, () -> {
			cpu.setRegister("D", cpu.swap(cpu.getRegister("D")));
		});
		opcodesCB[0x33] = create("SWAP E", 2, 8, () -> {
			cpu.setRegister("E", cpu.swap(cpu.getRegister("E")));
		});
		opcodesCB[0x34] = create("SWAP H", 2, 8, () -> {
			cpu.setRegister("H", cpu.swap(cpu.getRegister("H")));
		});
		opcodesCB[0x35] = create("SWAP L", 2, 8, () -> {
			cpu.setRegister("L", cpu.swap(cpu.getRegister("L")));
		});
		opcodesCB[0x36] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x37] = create("SWAP A", 2, 8, () -> {
			cpu.setRegister("A", cpu.swap(cpu.getRegister("A")));
		});
		opcodesCB[0x38] = create("SRL B", 2, 8, () -> {
			cpu.setRegister("B", cpu.srl(cpu.getRegister("B")));
		});
		opcodesCB[0x39] = create("SRL C", 2, 8, () -> {
			cpu.setRegister("C", cpu.srl(cpu.getRegister("C")));
		});
		opcodesCB[0x3A] = create("SRL D", 2, 8, () -> {
			cpu.setRegister("D", cpu.srl(cpu.getRegister("D")));
		});
		opcodesCB[0x3B] = create("SRL E", 2, 8, () -> {
			cpu.setRegister("E", cpu.srl(cpu.getRegister("E")));
		});
		opcodesCB[0x3C] = create("SRL H", 2, 8, () -> {
			cpu.setRegister("D", cpu.srl(cpu.getRegister("D")));
		});
		opcodesCB[0x3D] = create("SRL L", 2, 8, () -> {
			cpu.setRegister("L", cpu.srl(cpu.getRegister("L")));
		});
		opcodesCB[0x3E] = create("SRL (HL)", 2, 8, () -> {
			cpu.setRegister("(HL)", cpu.srl(cpu.getRegister("(HL)")));
		});
		opcodesCB[0x3F] = create("SRL A", 2, 8, () -> {
			cpu.setRegister("A", cpu.srl(cpu.getRegister("A")));
		});
		opcodesCB[0x40] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x41] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x42] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x43] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x44] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x45] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x46] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x47] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x48] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x49] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x4A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x4B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x4C] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x4D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x4E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x4F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x50] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x51] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x52] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x53] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x54] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x55] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x56] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x57] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x58] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x59] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x5A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x5B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x5C] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x5D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x5E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x5F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x60] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x61] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x62] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x63] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x64] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x65] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x66] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x67] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x68] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x69] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x6A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x6B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x6C] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x6D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x6E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x6F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x70] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x71] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x72] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x73] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x74] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x75] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x76] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x77] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x78] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x79] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x7A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x7B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x7C] = create("BIT 7,H", 2, 8, () -> {
			cpu.toogleFlag(Cpu.FLAG_ZERO, !cpu.getBit(cpu.h, 7));
			cpu.toogleFlag(Cpu.FLAG_SUBTRACT, false);
			cpu.toogleFlag(Cpu.FLAG_HALF_CARRY, true);
		});
		opcodesCB[0x7D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x7E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x7F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x80] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x81] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x82] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x83] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x84] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x85] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x86] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x87] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x88] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x89] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x8A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x8B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x8C] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x8D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x8E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x8F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x90] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x91] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x92] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x93] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x94] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x95] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x96] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x97] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x98] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x99] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x9A] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x9B] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x9C] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x9D] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x9E] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0x9F] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA0] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA1] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA2] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA3] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA4] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA5] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA6] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA7] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xA9] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xAA] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xAB] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xAC] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xAD] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xAE] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xAF] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB0] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB1] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB2] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB3] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB4] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB5] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB6] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB7] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xB9] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xBA] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xBB] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xBC] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xBD] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xBE] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xBF] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xC0] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xC1] = create("NA", 0, 0, () -> {

		});
		opcodesCB[0xC2] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xC3] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xC4] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xC5] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xC6] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xC7] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xC8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xC9] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xCA] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xCB] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xCC] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xCD] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xCE] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xCF] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xD0] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xD1] = create("NA", 0, 0, () -> {

		});
		opcodesCB[0xD2] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xD3] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xD4] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xD5] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xD6] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xD7] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xD8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xD9] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xDA] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xDB] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xDC] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xDD] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xDE] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xDF] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xE0] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xE1] = create("NA", 0, 0, () -> {

		});
		opcodesCB[0xE2] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xE3] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xE4] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xE5] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xE6] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xE7] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xE8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xE9] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xEA] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xEB] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xEC] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xED] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xEE] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xEF] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xF0] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xF1] = create("NA", 0, 0, () -> {

		});
		opcodesCB[0xF2] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xF3] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xF4] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xF5] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xF6] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xF7] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xF8] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xF9] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xFA] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xFB] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xFC] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xFD] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});
		opcodesCB[0xFE] = create("NA", 0, 0, () -> {
			throw new RuntimeException();
		});

	}

	public int exec() {

		System.out.println("PC:" + Integer.toHexString(cpu.pc));

		Opcode opcode = null;
		short i = cpu.rom[cpu.pc];
		boolean isCB = i == 0xCB;
		if (isCB) {
			i = cpu.rom[cpu.pc + 1];
			opcode = opcodesCB[i];
		} else {
			opcode = opcodes[i];
		}

		if (opcode.cycles == 0) {
			throw new RuntimeException("Cycles is 0, opcode:" + Integer.toHexString(i).toUpperCase() + " location:"
					+ Integer.toHexString(cpu.pc).toUpperCase() + " CB:" + isCB);
		}

		try {
			opcode.code.exec();
			cpu.pc += opcode.lenght;
			return opcode.cycles;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(
					"Error executing opcode:" + Integer.toHexString(i) + " - " + opcode.name + " : " + e.getMessage());
		}

	}

	public Opcode getOpcode(int index, boolean cb) {
		if (cb) {
			return opcodesCB[index];
		} else {
			return opcodes[index];
		}
	}

	private Opcode create(String name, int lenght, int cycles, Code code) {
		return new Opcode(name, cycles, lenght, code);
	}
}
