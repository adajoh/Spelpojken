package se.djax.spelpojken;

import java.io.File;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import se.djax.spelpojken.form.MainForm;
import se.djax.spelpojken.form.MemoryForm;

public class Spelpojken {

	public static void main(String[] args) {
		try {
			GameBoy gameBoy = new GameBoy();
			boolean headless = false;
			String romPath = null;

			for (String arg : args) {
				if (arg.equals("--headless")) {
					headless = true;
				} else if (romPath == null) {
					romPath = arg;
				}
			}

			if (romPath != null) {
				File romFile = new File(romPath);
				if (!romFile.exists()) {
					System.err.println("Filen hittades inte: " + romPath);
					System.exit(1);
				}
				gameBoy.loadRom(romFile);
			} else {
				var romStream = Spelpojken.class.getResourceAsStream("/cpu_instrs.gb");
				if (romStream == null) {
					throw new RuntimeException("Could not find internal cpu_instrs.gb and no ROM provided in arguments");
				}
				byte[] romData = romStream.readAllBytes();
				gameBoy.loadRom(romData);
			}

			if (headless) {
				System.out.println("Running headless mode...");
				try {
					long steps = 0;
					while (true) {
						gameBoy.step();
						steps++;
						if (steps > 50000000) break;
					}
					System.out.println("\nHeadless run finished.");
					System.exit(0);
				} catch (Exception e) {
					System.err.println("Exception at PC: 0x" + Integer.toHexString(gameBoy.getCpu().pc).toUpperCase());
					throw e;
				}
			}

			new MemoryForm(gameBoy);

			Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
			config.setWindowedMode(Gpu.WIDTH, Gpu.HEIGHT);
			config.setWindowPosition(500, 800);

			new Lwjgl3Application(new MainForm(gameBoy), config);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
