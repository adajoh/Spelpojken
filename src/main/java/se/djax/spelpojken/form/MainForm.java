package se.djax.spelpojken.form;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import se.djax.spelpojken.GameBoy;
import se.djax.spelpojken.Gpu;
import se.djax.spelpojken.Joypad;

public class MainForm extends ApplicationAdapter {

	private final GameBoy gameBoy;

	private SpriteBatch batch;
	private Pixmap pixmap;
	private Texture texture;
	private BitmapFont font;
	private OrthographicCamera camera;
	private Viewport viewport;

	public MainForm(GameBoy gameBoy) {
		this.gameBoy = gameBoy;
	}

	@Override
	public void create() {
		gameBoy.getApu().initAudio();
		batch = new SpriteBatch();
		pixmap = new Pixmap(Gpu.WIDTH, Gpu.HEIGHT, Pixmap.Format.RGBA8888);
		texture = new Texture(pixmap);
		font = new BitmapFont();
		camera = new OrthographicCamera();
		viewport = new FitViewport(Gpu.WIDTH, Gpu.HEIGHT, camera);
		viewport.apply();
		camera.position.set(Gpu.WIDTH / 2f, Gpu.HEIGHT / 2f, 0);
	}

	@Override
	public void render() {
		// Update GameBoy
		handleInput();
		gameBoy.runFrame();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		int[][] pixelData = gameBoy.getGpu().getPixelData();

		// Update pixmap with new frame data
		for (int x = 0; x < Gpu.WIDTH; x++) {
			for (int y = 0; y < Gpu.HEIGHT; y++) {
				pixmap.drawPixel(x, y, pixelData[x][y]);
			}
		}
		
		// Upload pixmap to texture
		texture.draw(pixmap, 0, 0);

		camera.update();
		batch.setProjectionMatrix(camera.combined);

		batch.begin();
		// Draw the texture. We try flipY = false now.
		batch.draw(texture, 0, 0, Gpu.WIDTH, Gpu.HEIGHT, 0, 0, Gpu.WIDTH, Gpu.HEIGHT, false, false);
		
		// Draw FPS
		font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 2, Gpu.HEIGHT - 2);
		
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		camera.position.set(Gpu.WIDTH / 2f, Gpu.HEIGHT / 2f, 0);
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
		pixmap.dispose();
		texture.dispose();
		if (font != null) font.dispose();
	}

}
