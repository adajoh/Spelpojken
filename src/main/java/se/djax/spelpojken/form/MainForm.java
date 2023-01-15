package se.djax.spelpojken.form;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import se.djax.spelpojken.GameBoy;
import se.djax.spelpojken.Gpu;

public class MainForm extends ApplicationAdapter {

	private final GameBoy gameBoy;

	ShapeRenderer shapeRenderer;
	OrthographicCamera camera;

	public MainForm(GameBoy gameBoy) {
		this.gameBoy = gameBoy;
	}

	@Override
	public void create() {
		shapeRenderer = new ShapeRenderer();
		camera = new OrthographicCamera(Gpu.WIDTH, Gpu.HEIGHT);
		camera.translate(camera.viewportWidth / 2, camera.viewportHeight / 2);
		camera.update();
		shapeRenderer.setProjectionMatrix(camera.combined);
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 1, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		shapeRenderer.begin(ShapeType.Filled);

		int pixelData[][] = gameBoy.getGpu().getPixelData();

		for (int x = 0; x < pixelData.length; x++) {
			for (int y = 0; y < pixelData[x].length; y++) {
				Color color = new Color(pixelData[x][y]);
				shapeRenderer.setColor(color);
				shapeRenderer.rect(x, Gpu.HEIGHT - y - 1, 1, 1);
			}
		}
		shapeRenderer.end();
	}

}
