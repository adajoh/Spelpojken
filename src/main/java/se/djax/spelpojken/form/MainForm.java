package se.djax.spelpojken.form;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;

import se.djax.spelpojken.GameBoy;
import se.djax.spelpojken.Gpu;
import se.djax.spelpojken.Joypad;

import java.nio.IntBuffer;

public class MainForm extends ApplicationAdapter {

	private final GameBoy gameBoy;

	private SpriteBatch batch;
	private Texture texture;
	private Pixmap pixmap;
	private IntBuffer pixelBuffer;

	public MainForm(GameBoy gameBoy) {
		this.gameBoy = gameBoy;
	}

	@Override
	public void create() {
		gameBoy.getApu().initAudio();
		batch = new SpriteBatch();
		pixmap = new Pixmap(Gpu.WIDTH, Gpu.HEIGHT, Pixmap.Format.RGBA8888);
		texture = new Texture(pixmap);
		pixelBuffer = BufferUtils.newIntBuffer(Gpu.WIDTH * Gpu.HEIGHT);
	}

	@Override
	public void render() {
		// Update GameBoy
		handleInput();
		gameBoy.runFrame();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		int[] pixelData = gameBoy.getGpu().getPixelData();
		
		// Update texture efficiently
		pixelBuffer.clear();
		pixelBuffer.put(pixelData);
		pixelBuffer.flip();
		
		pixmap.getPixels().asIntBuffer().put(pixelBuffer);
		texture.draw(pixmap, 0, 0);

		batch.begin();
		// Draw texture scaled to window size
		batch.draw(texture, 0, Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), -Gdx.graphics.getHeight());
		batch.end();
	}

	private void handleInput() {
		checkKey(Input.Keys.UP, Joypad.Button.UP);
		checkKey(Input.Keys.DOWN, Joypad.Button.DOWN);
		checkKey(Input.Keys.LEFT, Joypad.Button.LEFT);
		checkKey(Input.Keys.RIGHT, Joypad.Button.RIGHT);
		checkKey(Input.Keys.Z, Joypad.Button.A);
		checkKey(Input.Keys.X, Joypad.Button.B);
		checkKey(Input.Keys.ENTER, Joypad.Button.START);
		checkKey(Input.Keys.SPACE, Joypad.Button.SELECT);
	}

	private void checkKey(int gdxKey, Joypad.Button gbButton) {
		if (Gdx.input.isKeyJustPressed(gdxKey)) {
			gameBoy.pressButton(gbButton);
		} else if (!Gdx.input.isKeyPressed(gdxKey)) {
			gameBoy.releaseButton(gbButton);
		}
	}
	
	@Override
	public void dispose() {
		batch.dispose();
		texture.dispose();
		pixmap.dispose();
	}

}
