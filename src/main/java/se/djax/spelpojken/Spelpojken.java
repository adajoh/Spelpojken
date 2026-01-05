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

			if (args.length > 0) {
				File romFile = new File(args[0]);
				if (!romFile.exists()) {
					System.err.println("Filen hittades inte: " + args[0]);
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
