package se.djax.spelpojken;

import java.io.File;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import se.djax.spelpojken.form.MainForm;
import se.djax.spelpojken.form.MemoryForm;

public class Spelpojken {

	public static void main(String[] args) {
		try {
			File file = new File(Spelpojken.class.getResource("/cpu_instrs.gb").toURI());
			GameBoy gameBoy = new GameBoy();
			gameBoy.loadRom(file);
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
