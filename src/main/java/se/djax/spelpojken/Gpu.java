package se.djax.spelpojken;

import java.util.ArrayList;
import java.util.List;

/**
 * Game Boy GPU/PPU implementation.
 * Handles rendering of tiles, sprites, and LCD timing.
 */
public class Gpu {

	public static final int WIDTH = 160;
	public static final int HEIGHT = 144;

	// LCD timings (in CPU cycles)
	private static final int CYCLES_OAM_SEARCH = 80;      // Mode 2
	private static final int CYCLES_PIXEL_TRANSFER = 172; // Mode 3
	private static final int CYCLES_HBLANK = 204;         // Mode 0
	private static final int CYCLES_VBLANK_LINE = 456;    // Mode 1 (10 lines)

	// LCD modes
	private static final int MODE_HBLANK = 0;
	private static final int MODE_VBLANK = 1;
	private static final int MODE_OAM_SEARCH = 2;
	private static final int MODE_PIXEL_TRANSFER = 3;

	// LCD registers
	private static final int REG_LCDC = 0xFF40;  // LCD Control
	private static final int REG_STAT = 0xFF41;  // LCD Status
	private static final int REG_SCY = 0xFF42;   // Scroll Y
	private static final int REG_SCX = 0xFF43;   // Scroll X
	private static final int REG_LY = 0xFF44;    // Current Scanline
	private static final int REG_LYC = 0xFF45;   // LY Compare
	private static final int REG_BGP = 0xFF47;   // BG Palette
	private static final int REG_OBP0 = 0xFF48;  // Object Palette 0
	private static final int REG_OBP1 = 0xFF49;  // Object Palette 1
	private static final int REG_WY = 0xFF4A;    // Window Y
	private static final int REG_WX = 0xFF4B;    // Window X

	// Colors (RGBA8888)
	private static final int[] COLORS = {
		0xFFFFFFFF,  // White
		0xAAAAAAFF,  // Light gray
		0x555555FF,  // Dark gray
		0x000000FF   // Black
	};

	private final Cpu cpu;
	private final int[][] pixelData;
	private final int[][] bgPixelColorIndex;
	private Interrupts interrupts;

	private int scanLineCyclesCounter = 0;
	private int currentMode = MODE_OAM_SEARCH;
	private int windowLineCounter = 0;

	public Gpu(Cpu cpu) {
		this.cpu = cpu;
		pixelData = new int[WIDTH][HEIGHT];
		bgPixelColorIndex = new int[WIDTH][HEIGHT];
	}

	public void setInterrupts(Interrupts interrupts) {
		this.interrupts = interrupts;
	}

	/**
	 * Execute GPU for the given number of CPU cycles.
	 */
	public void exec(int cycles) {
		// Check if LCD is enabled
		if (!isLCDEnabled()) {
			// LCD disabled - reset everything
			scanLineCyclesCounter = 0;
			cpu.setRawMem(REG_LY, (short) 0);
			setMode(MODE_HBLANK);
			return;
		}

		scanLineCyclesCounter += cycles;

		switch (currentMode) {
			case MODE_OAM_SEARCH:
				if (scanLineCyclesCounter >= CYCLES_OAM_SEARCH) {
					scanLineCyclesCounter -= CYCLES_OAM_SEARCH;
					setMode(MODE_PIXEL_TRANSFER);
				}
				break;

			case MODE_PIXEL_TRANSFER:
				if (scanLineCyclesCounter >= CYCLES_PIXEL_TRANSFER) {
					scanLineCyclesCounter -= CYCLES_PIXEL_TRANSFER;
					
					// Draw the current scanline
					int ly = cpu.getRawMem(REG_LY);
					if (ly < HEIGHT) {
						drawScanline(ly);
					}
					
					setMode(MODE_HBLANK);
					
					// Request STAT interrupt for HBlank if enabled
					if (interrupts != null && (cpu.getRawMem(REG_STAT) & 0x08) != 0) {
						interrupts.requestInterrupt(Interrupts.LCD_STAT);
					}
				}
				break;

			case MODE_HBLANK:
				if (scanLineCyclesCounter >= CYCLES_HBLANK) {
					scanLineCyclesCounter -= CYCLES_HBLANK;
					
					// Move to next line
					int ly = cpu.getRawMem(REG_LY) + 1;
					cpu.setRawMem(REG_LY, (short) ly);
					
					checkLYC();
					
					if (ly >= HEIGHT) {
						// Enter VBlank
						setMode(MODE_VBLANK);
						windowLineCounter = 0;
						
						// Request VBlank interrupt
						if (interrupts != null) {
							interrupts.requestInterrupt(Interrupts.VBLANK);
							
							// Also STAT interrupt if VBlank flag is set
							if ((cpu.getRawMem(REG_STAT) & 0x10) != 0) {
								interrupts.requestInterrupt(Interrupts.LCD_STAT);
							}
						}
					} else {
						setMode(MODE_OAM_SEARCH);
						
						// Request STAT interrupt for OAM if enabled
						if (interrupts != null && (cpu.getRawMem(REG_STAT) & 0x20) != 0) {
							interrupts.requestInterrupt(Interrupts.LCD_STAT);
						}
					}
				}
				break;

			case MODE_VBLANK:
				if (scanLineCyclesCounter >= CYCLES_VBLANK_LINE) {
					scanLineCyclesCounter -= CYCLES_VBLANK_LINE;
					
					int ly = cpu.getRawMem(REG_LY) + 1;
					
					if (ly > 153) {
						// VBlank finished, start new frame
						ly = 0;
						cpu.setRawMem(REG_LY, (short) 0);
						setMode(MODE_OAM_SEARCH);
						
						// Request STAT interrupt for OAM if enabled
						if (interrupts != null && (cpu.getRawMem(REG_STAT) & 0x20) != 0) {
							interrupts.requestInterrupt(Interrupts.LCD_STAT);
						}
					} else {
						cpu.setRawMem(REG_LY, (short) ly);
					}
					
					checkLYC();
				}
				break;
		}
	}

	private void setMode(int mode) {
		currentMode = mode;
		int stat = cpu.getRawMem(REG_STAT);
		stat = (stat & 0xFC) | mode;
		cpu.setRawMem(REG_STAT, (short) stat);
	}

	private void checkLYC() {
		int ly = cpu.getRawMem(REG_LY);
		int lyc = cpu.getRawMem(REG_LYC);
		int stat = cpu.getRawMem(REG_STAT);
		
		if (ly == lyc) {
			// Set coincidence flag
			stat |= 0x04;
			cpu.setRawMem(REG_STAT, (short) stat);
			
			// Request STAT interrupt if LYC=LY interrupt is enabled
			if (interrupts != null && (stat & 0x40) != 0) {
				interrupts.requestInterrupt(Interrupts.LCD_STAT);
			}
		} else {
			// Clear coincidence flag
			stat &= ~0x04;
			cpu.setRawMem(REG_STAT, (short) stat);
		}
	}

	private boolean isLCDEnabled() {
		return cpu.getBit(cpu.getRawMem(REG_LCDC), 7);
	}

	private void drawScanline(int line) {
		int lcdc = cpu.getRawMem(REG_LCDC);
		
		// Draw background
		if ((lcdc & 0x01) != 0) {
			drawBackground(line);
			
			// Draw window
			if ((lcdc & 0x20) != 0) {
				drawWindow(line);
			}
		} else {
			// Fill with white if BG disabled
			for (int x = 0; x < WIDTH; x++) {
				pixelData[x][line] = COLORS[0];
				bgPixelColorIndex[x][line] = 0;
			}
		}
		
		// Draw sprites
		if ((lcdc & 0x02) != 0) {
			drawSprites(line);
		}
	}

	private void drawBackground(int line) {
		int lcdc = cpu.getRawMem(REG_LCDC);
		int scrollY = cpu.getRawMem(REG_SCY);
		int scrollX = cpu.getRawMem(REG_SCX);
		int bgp = cpu.getRawMem(REG_BGP);
		
		// Tile data address
		int tileDataAddress = (lcdc & 0x10) != 0 ? 0x8000 : 0x8800;
		boolean signedTileNumbers = (lcdc & 0x10) == 0;
		
		// Tile map address
		int tileMapAddress = (lcdc & 0x08) != 0 ? 0x9C00 : 0x9800;
		
		int y = (scrollY + line) & 0xFF;
		int tileRow = (y / 8) * 32;
		
		for (int pixel = 0; pixel < WIDTH; pixel++) {
			int x = (scrollX + pixel) & 0xFF;
			int tileCol = x / 8;
			
			int tileIndex = cpu.getRawMem(tileMapAddress + tileRow + tileCol);
			
			int tileAddress;
			if (signedTileNumbers) {
				tileAddress = tileDataAddress + ((byte) tileIndex + 128) * 16;
			} else {
				tileAddress = tileDataAddress + tileIndex * 16;
			}
			
			int tileY = (y % 8) * 2;
			int data1 = cpu.getRawMem(tileAddress + tileY);
			int data2 = cpu.getRawMem(tileAddress + tileY + 1);
			
			int colorBit = 7 - (x % 8);
			int colorNum = ((data2 >> colorBit) & 1) << 1 | ((data1 >> colorBit) & 1);
			int color = (bgp >> (colorNum * 2)) & 0x03;
			
			pixelData[pixel][line] = COLORS[color];
			bgPixelColorIndex[pixel][line] = colorNum;
		}
	}

	private void drawWindow(int line) {
		int lcdc = cpu.getRawMem(REG_LCDC);
		int windowY = cpu.getRawMem(REG_WY);
		int windowX = cpu.getRawMem(REG_WX) - 7;
		
		// Check if window is visible on this line
		if (line < windowY || windowX >= WIDTH) {
			return;
		}
		
		int bgp = cpu.getRawMem(REG_BGP);
		
		// Tile data address
		int tileDataAddress = (lcdc & 0x10) != 0 ? 0x8000 : 0x8800;
		boolean signedTileNumbers = (lcdc & 0x10) == 0;
		
		// Window tile map address
		int tileMapAddress = (lcdc & 0x40) != 0 ? 0x9C00 : 0x9800;
		
		int y = windowLineCounter;
		int tileRow = (y / 8) * 32;
		
		for (int pixel = Math.max(0, windowX); pixel < WIDTH; pixel++) {
			int x = pixel - windowX;
			int tileCol = x / 8;
			
			int tileIndex = cpu.getRawMem(tileMapAddress + tileRow + tileCol);
			
			int tileAddress;
			if (signedTileNumbers) {
				tileAddress = tileDataAddress + ((byte) tileIndex + 128) * 16;
			} else {
				tileAddress = tileDataAddress + tileIndex * 16;
			}
			
			int tileY = (y % 8) * 2;
			int data1 = cpu.getRawMem(tileAddress + tileY);
			int data2 = cpu.getRawMem(tileAddress + tileY + 1);
			
			int colorBit = 7 - (x % 8);
			int colorNum = ((data2 >> colorBit) & 1) << 1 | ((data1 >> colorBit) & 1);
			int color = (bgp >> (colorNum * 2)) & 0x03;
			
			pixelData[pixel][line] = COLORS[color];
			bgPixelColorIndex[pixel][line] = colorNum;
		}
		
		windowLineCounter++;
	}


	private void drawSprites(int line) {
		int lcdc = cpu.getRawMem(REG_LCDC);
		int spriteHeight = (lcdc & 0x04) != 0 ? 16 : 8;
		
		// Collect up to 10 sprites on this line
		class SpriteInfo {
			int index;
			int x;
			int y;
			int tile;
			int attr;
			SpriteInfo(int i, int x, int y, int t, int a) {
				this.index = i; this.x = x; this.y = y; this.tile = t; this.attr = a;
			}
		}
		List<SpriteInfo> spritesOnLine = new ArrayList<>();
		
		for (int sprite = 0; sprite < 40; sprite++) {
			int oamAddress = 0xFE00 + sprite * 4;
			int spriteY = cpu.getRawMem(oamAddress) - 16;
			int spriteX = cpu.getRawMem(oamAddress + 1) - 8;
			int tileIndex = cpu.getRawMem(oamAddress + 2);
			int attributes = cpu.getRawMem(oamAddress + 3);
			
			if (line >= spriteY && line < spriteY + spriteHeight) {
				spritesOnLine.add(new SpriteInfo(sprite, spriteX, spriteY, tileIndex, attributes));
				if (spritesOnLine.size() >= 10) break;
			}
		}
		
		// Sort sprites: largest X first, then largest index first (to draw them in reverse priority)
		spritesOnLine.sort((s1, s2) -> {
			if (s1.x != s2.x) return Integer.compare(s2.x, s1.x);
			return Integer.compare(s2.index, s1.index);
		});
		
		for (SpriteInfo sprite : spritesOnLine) {
			int spriteX = sprite.x;
			int spriteY = sprite.y;
			int tileIndex = sprite.tile;
			int attributes = sprite.attr;
			
			boolean flipY = (attributes & 0x40) != 0;
			boolean flipX = (attributes & 0x20) != 0;
			boolean priority = (attributes & 0x80) != 0;
			int palette = (attributes & 0x10) != 0 ? cpu.getRawMem(REG_OBP1) : cpu.getRawMem(REG_OBP0);
			
			if (spriteHeight == 16) {
				tileIndex &= 0xFE;
			}
			
			int tileY = line - spriteY;
			if (flipY) {
				tileY = spriteHeight - 1 - tileY;
			}
			
			if (tileY >= 8) {
				tileIndex++;
				tileY -= 8;
			}
			
			int tileAddress = 0x8000 + tileIndex * 16 + tileY * 2;
			int data1 = cpu.getRawMem(tileAddress);
			int data2 = cpu.getRawMem(tileAddress + 1);
			
			for (int pixelX = 0; pixelX < 8; pixelX++) {
				int x = spriteX + pixelX;
				if (x < 0 || x >= WIDTH) continue;
				
				int colorBit = flipX ? pixelX : 7 - pixelX;
				int colorNum = ((data2 >> colorBit) & 1) << 1 | ((data1 >> colorBit) & 1);
				
				if (colorNum == 0) continue;
				
				// Priority: if bit 7 is set, sprite is hidden by BG colors 1, 2, 3
				if (priority && bgPixelColorIndex[x][line] != 0) continue;
				
				int color = (palette >> (colorNum * 2)) & 0x03;
				pixelData[x][line] = COLORS[color];
			}
		}
	}

	/**
	 * Perform DMA transfer.
	 */
	public void doDMATransfer(int sourceHigh) {
		int sourceAddress = sourceHigh << 8;
		for (int i = 0; i < 160; i++) {
			cpu.setRawMem(0xFE00 + i, cpu.getRawMem(sourceAddress + i));
		}
	}

	public int[][] getPixelData() {
		return pixelData;
	}
}
