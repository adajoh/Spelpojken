package se.djax.spelpojken.model;

import javax.swing.table.AbstractTableModel;

import se.djax.spelpojken.Cpu;
import se.djax.spelpojken.GameBoy.InstructionListener;

public class CpuModel extends AbstractTableModel implements InstructionListener {

	private static final long serialVersionUID = -6640363614485626603L;

	private final Cpu cpu;

	public CpuModel(Cpu cpu) {
		this.cpu = cpu;
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public int getRowCount() {
		return 14;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {

		if (rowIndex == 0) {
			if (columnIndex == 0) {
				return "A";
			} else {
				return Integer.toHexString(cpu.a).toUpperCase();
			}
		}

		if (rowIndex == 1) {
			if (columnIndex == 0) {
				return "B";
			} else {
				return Integer.toHexString(cpu.b).toUpperCase();
			}
		}

		if (rowIndex == 2) {
			if (columnIndex == 0) {
				return "C";
			} else {
				return Integer.toHexString(cpu.c).toUpperCase();
			}
		}

		if (rowIndex == 3) {
			if (columnIndex == 0) {
				return "D";
			} else {
				return Integer.toHexString(cpu.d).toUpperCase();
			}
		}

		if (rowIndex == 4) {
			if (columnIndex == 0) {
				return "E";
			} else {
				return Integer.toHexString(cpu.e).toUpperCase();
			}
		}

		if (rowIndex == 5) {
			if (columnIndex == 0) {
				return "F";
			} else {
				return Integer.toHexString(cpu.getRegister("F")).toUpperCase();
			}
		}

		if (rowIndex == 6) {
			if (columnIndex == 0) {
				return "H";
			} else {
				return Integer.toHexString(cpu.h).toUpperCase();
			}
		}

		if (rowIndex == 7) {
			if (columnIndex == 0) {
				return "L";
			} else {
				return Integer.toHexString(cpu.l).toUpperCase();
			}
		}

		if (rowIndex == 8) {
			if (columnIndex == 0) {
				return "SP";
			} else {
				return Integer.toHexString(cpu.sp).toUpperCase();
			}
		}

		if (rowIndex == 9) {
			if (columnIndex == 0) {
				return "PC";
			} else {
				return Integer.toHexString(cpu.pc).toUpperCase();
			}
		}

		if (rowIndex == 10) {
			if (columnIndex == 0) {
				return "Z  - Zero Flag (.7)";
			} else {
				return cpu.getFlag(Cpu.FLAG_ZERO);
			}
		}

		if (rowIndex == 11) {
			if (columnIndex == 0) {
				return "N - Subtract Flag (.6)";
			} else {
				return cpu.getFlag(Cpu.FLAG_SUBTRACT);
			}
		}

		if (rowIndex == 12) {
			if (columnIndex == 0) {
				return "H - Half Carry Flag (.5)";
			} else {
				return cpu.getFlag(Cpu.FLAG_HALF_CARRY);
			}
		}

		if (rowIndex == 13) {
			if (columnIndex == 0) {
				return "C - Carry Flag (.4)";
			} else {
				return cpu.getFlag(Cpu.FLAG_CARRY);
			}
		}

		return "nada";
	}

	@Override
	public void onExecution() {
		fireTableDataChanged();
	}

}
