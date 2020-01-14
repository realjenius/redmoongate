package realjenius.moongate

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import realjenius.moongate.converse.Conversation
import realjenius.moongate.converse.Conversations
import realjenius.moongate.io.GameFiles
import realjenius.moongate.io.LZW
import realjenius.moongate.io.LibraryIO
import realjenius.moongate.io.LibraryType
import realjenius.moongate.screen.Maps
import realjenius.moongate.screen.Palette
import realjenius.moongate.screen.Palettes
import realjenius.moongate.screen.Tiles

class RedMoongate : ApplicationAdapter() {
  private val MAP_WIDTH = 320
  private val MAP_HEIGHT = 200
  private lateinit var camera: OrthographicCamera
  private lateinit var batch: SpriteBatch
  private lateinit var shapes: ShapeRenderer
  private lateinit var tileTextures: List<Texture>
  private val viewableMap = ByteArray(320 * 200 * 16)

  private var originX = 0
  private var originY = 0
  override fun create() {
    camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    batch = SpriteBatch()
    shapes = ShapeRenderer()
    Conversations.load()
    Palettes.load()
    Tiles.load()
    Maps.load()
    tileTextures = Tiles.tiles.map {
      val pixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
      (0 until 16).forEach { row ->
        (0 until 16).forEach { col ->
          val value = it.data[row + col*16]
          val color = Palettes.gamePalette.values[value.toUByte().toInt()].let {
            Color.argb8888(it.red, it.green, it.blue, 1.toFloat())
          }
          pixmap.drawPixel(row, col, color)
        }
      }
      Texture(pixmap).apply { pixmap.dispose() }
    }
    rebuildMap()
  }

  private fun rebuildMap() {
    val mapRowLength = Maps.getRowWidth(0)
    var viewportIdx = 0
    (0 until MAP_HEIGHT).forEach { y ->
      (0 until MAP_WIDTH).forEach { x ->
        val sourceMapX = (originX + x)// and 1023
        val sourceMapY = (originY + y) //and 1023 // TODO - is this right??
        viewableMap[viewportIdx] = Maps.surfaceMap[sourceMapY * mapRowLength + sourceMapX]
        viewportIdx++
      }
    }
  }

  override fun render() {
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    var changed = false
    if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) { originX = (originX-1).coerceAtLeast(0); changed = true }
    if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) { originX += 1; changed = true }
    if (Gdx.input.isKeyPressed(Input.Keys.UP)) { originY = (originY-1).coerceAtLeast(0); changed = true }
    if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) { originY += 1; changed = true }

    if(changed) rebuildMap()

    camera.update()
    batch.projectionMatrix = camera.combined
    batch.begin()
    (0 until MAP_HEIGHT).forEach { y ->
      (0 until MAP_WIDTH).forEach { x ->
        val drawX = x * 16
        val drawY = (MAP_HEIGHT * 16) - y * 16

        val tile = tileTextures[viewableMap[(y * MAP_WIDTH) + x].toUByte().toInt()]

        batch.draw(tile, drawX.toFloat(), drawY.toFloat())
      }
    }

    batch.end()
  }

  private fun renderPalette(xOffset: Int, pal: Palette) {
    pal.values.forEachIndexed { index, rgb ->
      val y = index % 100 * 10
      val x = index / 100 * 10 + xOffset
      shapes.setColor(rgb.red, rgb.green, rgb.blue, 1.toFloat())
      shapes.rect(x.toFloat(), y.toFloat(), 10.toFloat(), 10.toFloat())
    }
  }

  override fun dispose() {
    batch.dispose()
    shapes.dispose()
  }
}
