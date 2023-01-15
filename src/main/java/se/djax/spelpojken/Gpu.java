package se.djax.spelpojken;

import org.junit.Assert;

public class Gpu {

	public static final int WIDTH = 160;
	public static final int HEIGHT = 144;

	private static final int CYCLES_PER_SCANLINE = 456;
	private static final int REG_LCD_CONTROL = 0xFF40;
	private static final int REG_CURRENT_SCANLINE = 0xFF44;

	private final Cpu cpu;
	private final int pixelData[][];

	private int scanLineCyclesCounter;

	public Gpu(Cpu cpu) {
		this.cpu = cpu;
		pixelData = new int[WIDTH][HEIGHT];
	}

	public void exec(int cycles) {

		// LCD enabled
		if (cpu.getBit(cpu.getMem(REG_LCD_CONTROL), 7)) {
			scanLineCyclesCounter -= cycles;
		} else {
			fillScreen(0); // set to black
			return;
		}

		// fillScreen(-1); // set to white

		// Time to draw scanline
		if (scanLineCyclesCounter <= 0) {
			scanLineCyclesCounter = CYCLES_PER_SCANLINE;

			// Update wich scanline we are on
			short currentLine = cpu.getMem(REG_CURRENT_SCANLINE);
			currentLine++;
			if (currentLine > 153) {
				currentLine = 0;
			}
			cpu.setMem(REG_CURRENT_SCANLINE, currentLine);

			// Check if vertical blank
			if (currentLine == 144) {
				// TODO fire interupt
			}

			// Should we draw it?
			if (currentLine < 144) {
				if (cpu.getBit(cpu.getMem(REG_LCD_CONTROL), 0)) {
					drawTiles(currentLine);
				}
				if (cpu.getBit(cpu.getMem(REG_LCD_CONTROL), 1)) {
					drawSprites();
				}
			}
		}
	}

	private void drawSprites() {
		System.out.println("draw sprites");
	}

	private void drawTiles(short currentLine) {

		short scrollY = cpu.getMem(0xFF42);
		short scrollX = cpu.getMem(0xFF43);
		// short windowY = cpu.getMem(0xFF4A);
		// short windowX = (short) (cpu.getMem(0xFF4B) - 7);

		// Window
		boolean window = cpu.getBit(cpu.getMem(REG_LCD_CONTROL), 5);
		Assert.assertFalse(window);

		// Which tile data?
		int tileStartAddress;
		if (cpu.getBit(cpu.getMem(REG_LCD_CONTROL), 4)) {
			tileStartAddress = 0x8000;
		} else {
			tileStartAddress = 0x8800;
			throw new RuntimeException("Implement unsigned");
		}

		// Which background address?
		int backgroundAddress;
		if (cpu.getBit(cpu.getMem(REG_LCD_CONTROL), 3)) {
			backgroundAddress = 0x9C00;
		} else {
			backgroundAddress = 0x9800;
		}

		int y = scrollY + currentLine;
		int tileRow = (y / 8) * 32;

		for (int pixel = 0; pixel < WIDTH; pixel++) {
			int x = pixel + scrollX;

			int tileCol = (x / 8);
			int tileNumber = cpu.getMem(backgroundAddress + tileRow + tileCol);

			int tileAddress = tileStartAddress + (tileNumber * 16);

			int line = y % 8;
			line *= 2;

			short data1 = cpu.getMem(tileAddress + line);
			short data2 = cpu.getMem(tileAddress + line + 1);

			int colourBit = x % 8;
			colourBit -= 7;
			colourBit *= -1;

			boolean b1 = cpu.getBit(data1, colourBit);
			boolean b2 = cpu.getBit(data2, colourBit);

			if (b1) {
				if (b2) {
					pixelData[pixel][currentLine] = 0;
				} else {
					pixelData[pixel][currentLine] = 1545487;
				}
			} else {
				if (b2) {
					pixelData[pixel][currentLine] = 2145234;
				} else {
					pixelData[pixel][currentLine] = -1;
				}
			}

		}

	}

	public int[][] getPixelData() {
		return pixelData;
	}

	private void fillScreen(int color) {
		for (int x = 0; x < pixelData.length; x++) {
			for (int y = 0; y < pixelData[x].length; y++) {
				pixelData[x][y] = color;
			}
		}
	}

}
