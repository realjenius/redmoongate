package realjenius.moongate.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import realjenius.moongate.gamedata.tile.Tiles
import realjenius.moongate.ui.UIComponent

class TileView : UIComponent {

  private var lastSelected = 0
  private var selected = 0
  private var palette = 0
  private var font = BitmapFont().apply {
    this.color = Color.WHITE
  }
  private var shapes = ShapeRenderer()

  override fun input() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) if (selected > 0) selected--
    if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) if (selected < 2047) selected++
    if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) if (selected - 64 >= 0) selected -= 64
    if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) if (selected + 64 <= 2047) selected += 64
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) palette = 0
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) palette = 1
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) palette = 2
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) palette = 3
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) palette = 4
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) palette = 5
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) palette = 6
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) palette = 7

  }

  override fun update() {
    if (lastSelected != selected) {
      Gdx.app.log("tileview", "$selected : ${Tiles.tiles[selected]}")
      lastSelected = selected
    }
  }

  override fun render(camera: OrthographicCamera, batch: SpriteBatch) {
    shapes.begin(ShapeRenderer.ShapeType.Line)
    (0 until 2048).forEach { index ->
      val x = index % 64
      val y = 32 - (index / 64)
      val drawX = 10 + (18 * x)
      val drawY = 1100 - ((18 * 31) - (18 * y))
      batch.draw(Tiles.tiles[index].spriteForPalette(palette), drawX.toFloat(), drawY.toFloat())
      if (selected == index) {
        shapes.rect(drawX-1f, drawY-1f, 18f, 18f, Color.RED, Color.RED, Color.RED, Color.RED)
      }
    }

    shapes.end()
  }
}