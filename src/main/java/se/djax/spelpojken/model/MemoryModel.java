package se.djax.spelpojken.model;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import se.djax.spelpojken.Cpu;
import se.djax.spelpojken.GameBoy;
import se.djax.spelpojken.GameBoy.InstructionListener;

public class MemoryModel extends AbstractTableModel implements InstructionListener {

	private String[] descriptions;

	@SuppressWarnings("serial")
	public class MemoryModelRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			// Default
			c.setBackground(Color.LIGHT_GRAY);
			c.setForeground(Color.BLACK);

			if (column > 0 && column < 17) {

				int address = column - 1 + row * 16;

				// Description
				if (descriptions[address] != null) {
					c.setForeground(Color.YELLOW);
				}

				// pointers
				if (address == cpu.get16BitRegister("HL")) {
					c.setBackground(Color.WHITE);
				}
				if (address == cpu.get16BitRegister("DE")) {
					c.setBackground(Color.WHITE);
				}

				// PC
				if (address == cpu.pc) {
					c.setBackground(Color.GREEN);
				}

				// SP
				if (address == cpu.sp) {
					c.setBackground(Color.PINK);
				}

			}

			return c;
		}
	}

	private static final long serialVersionUID = 2358831714116678507L;
	private final Cpu cpu;
	private final GameBoy gameBoy;

	public MemoryModel(GameBoy gameBoy) {
		this.cpu = gameBoy.getCpu();
		this.gameBoy = gameBoy;

		descriptions = new String[GameBoy.MEMORY_SIZE];
		descriptions[0x0040] = "Vertical Blank Interrupt Start Address";
		descriptions[0x0048] = "LCDC Status Interrupt Start Address";
		descriptions[0x0050] = "Timer Overflow Interrupt Start Address";
		descriptions[0x0058] = "Serial Transfer Completion Interrupt Start Address";
		descriptions[0x0060] = "High-to-Low of P10-P13 Interrupt Start Address";

		descriptions[0x0148] = "ROM size";
		descriptions[0x0149] = "RAM size";
		descriptions[0x014D] = "Checksum";

		descriptions[0xFF00] = "Joypad";

		descriptions[0xFF04] = "Divider Register";
		descriptions[0xFF05] = "Timer Counter";
		descriptions[0xFF06] = "Timer Modulo";
		descriptions[0xFF07] = "Timer Control";

		descriptions[0xFF10] = "Sound Mode 1 register sweep";
		descriptions[0xFF11] = "Sound Mode 1 register lenght";
		descriptions[0xFF12] = "Sound Mode 1 register Envelope";
		descriptions[0xFF13] = "Sound Mode 1 register Frequency lo";
		descriptions[0xFF14] = "Sound Mode 1 register Frequency hi";

		descriptions[0xFF25] = "Selection of Sound output terminal";
		descriptions[0xFF26] = "Sound on/off";

		descriptions[0xFF40] = "LCD Control register";
		descriptions[0xFF41] = "LCD Status";
		descriptions[0xFF42] = "BG Scroll Y";
		descriptions[0xFF43] = "BG Scroll X";
		descriptions[0xFF44] = "LCD Current Scanline";
		descriptions[0xFF45] = "LY Compare";
		descriptions[0xFF46] = "OAM DMA Transfer";
		descriptions[0xFF47] = "BG & Window Palette Data";
		descriptions[0xFF4A] = "Window Y Position";
		descriptions[0xFF4B] = "Window X Position";
		descriptions[0xFF4D] = "KEY1 – GBC Mode – Speed Switch (R/W)";

		descriptions[0xFF4F] = "VBK – GBC Mode – VRAM Bank (R/W)";
		descriptions[0xFF70] = "SVBK – GBC Mode – WRAM Bank (R/W)";
		descriptions[0xFF0F] = "Interrupt Flags (R/W)";
		descriptions[0xFFFF] = "Interrupt enable flags (R/W)";

	}

	@Override
	public int getColumnCount() {
		return 1 + 16 + 16;
	}

	@Override
	public int getRowCount() {
		return cpu.rom.length / 16;
	}

	@Override
	public String getColumnName(int column) {
		if (column == 0) {
			return "";
		}
		if (column < 17) {
			return Integer.toHexString(column - 1).toUpperCase();
		}
		return "";
	}

	@Override
	public Object getValueAt(int row, int column) {

		if (column == 0) {
			int startAddress = row * 16;
			String s = Integer.toHexString(startAddress).toUpperCase();

			if (startAddress >= 0x0000 && startAddress <= 0x3FFF) {
				s += ":ROM0";
			}
			if (startAddress >= 0x4000 && startAddress <= 0x7FFF) {
				s += ":ROMX";
			}
			if (startAddress >= 0x8000 && startAddress <= 0x9FFF) {
				s += ":VRAM";
			}
			if (startAddress >= 0xA000 && startAddress <= 0xBFFF) {
				s += ":SRAM";
			}
			if (startAddress >= 0xC000 && startAddress <= 0xCFFF) {
				s += ":WRAM0";
			}
			if (startAddress >= 0xD000 && startAddress <= 0xDFFF) {
				s += ":WRAMX";
			}
			if (startAddress >= 0xE000 && startAddress <= 0xFDFF) {
				s += ":ECHO";
			}
			if (startAddress >= 0xFE00 && startAddress <= 0xFE9F) {
				s += ":OAM";
			}
			if (startAddress >= 0xFEA0 && startAddress <= 0xFEFF) {
				s += ":UNUSED";
			}
			if (startAddress >= 0xFF00 && startAddress <= 0xFF7F) {
				s += ":I/O";
			}
			if (startAddress >= 0xFF80 && startAddress <= 0xFFFE) {
				s += ":HRAM";
			}

			return s;
		} else if (column < 17) {
			return Integer.toHexString(cpu.rom[column + row * 16 - 1]).toUpperCase();
		} else {
			return (char) cpu.rom[column + row * 16 - 17];
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return super.getColumnClass(columnIndex);
	}

	@Override
	public void onExecution() {
		fireTableDataChanged();
	}

	public String getInfo(int row, int col) {
		if (col > 0 && col < 17) {

			int address = col - 1 + row * 16;
			String s = "0x" + Integer.toHexString(row * 16 + col - 1).toUpperCase();

			if (descriptions[address] != null) {
				s += " - " + descriptions[address];
			}
			if (cpu.rom[address] < 0xFF) {
				s += " [" + gameBoy.getOpcodes().getOpcode(cpu.rom[address], false).name + "]";
			}

			return s;
		}

		return "";
	}

}
