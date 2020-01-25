package realjenius.moongate.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import realjenius.moongate.gamedata.map.Maps
import realjenius.moongate.gamedata.obj.GameObjects
import realjenius.moongate.gamedata.tile.Tiles

class MapViewport(private val level: Int = 0, width: Int = DEFAULT_MAP_WIDTH, private val height: Int = DEFAULT_MAP_HEIGHT) {
  private val viewableMap = ByteArray(width * height)
  private val numTilesX = width / 16
  private val numTilesY = height / 16
  private var originX: Int = 0
  private var originY: Int = 0
  private var needsRebuild = true
  private var shapes = ShapeRenderer()

  init {
    update()
  }

  fun moveTo(x: Int, y: Int) = this.apply {
    needsRebuild = x != originX || y != originY
    originX = x
    originY = y
  }

  fun shift(x: Int = 0, y: Int = 0) = moveTo(originX + x, originY + y)

  fun update() {
    if (!needsRebuild) return
    needsRebuild = false
    val mapRowLength = Maps.diameter(level)
    var viewportIdx = 0
    (0 until numTilesY).forEach { y ->
      (0 until numTilesX).forEach { x ->
        val sourceMapX = (originX + x)
        val sourceMapY = (originY + y)
        viewableMap[viewportIdx] = Maps.mapForLevel(level)[sourceMapY * mapRowLength + sourceMapX]
        viewportIdx++
      }
    }
  }

  fun render(batch: SpriteBatch) {
    shapes.begin(ShapeRenderer.ShapeType.Filled)
    renderMapTiles(batch)
    renderObjects(batch)
    shapes.end()
  }

  private fun renderMapTiles(batch: SpriteBatch) {
    (0 until numTilesY).forEach { y ->
      (0 until numTilesX).forEach { x ->
        val drawX = x * 16
        val drawY = (height) - y * 16
        val tileIndex = viewableMap[(y * numTilesX) + x].toUByte().toInt()
        if (tileIndex == 0)
          shapes.rect(drawX.toFloat(), drawY.toFloat(), 16.toFloat(), 16.toFloat(), Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK)
        else renderTile(tileIndex, batch, drawX, drawY)
      }
    }
  }

  private fun renderObjects(batch: SpriteBatch) {
    (0 until numTilesY).forEach { y ->
      (0 until numTilesX).forEach { x ->
        val drawX = x * 16
        val drawY = (height) - y * 16
        val objsAt = GameObjects.objectsByLevel[level].get(originX + x, originY + y, 0)
        objsAt.forEach { renderTile(it.tileId, batch, drawX, drawY) }
      }
    }
  }

  private fun renderTile(tileIndex: Int, batch: SpriteBatch, drawX: Int, drawY: Int) {
    if (Tiles.isAnimatedMapTile(tileIndex))
      batch.draw(Tiles.getAnimatedBaseTile(tileIndex).sprite, drawX.toFloat(), drawY.toFloat())
    batch.draw(Tiles.tiles[tileIndex].sprite, drawX.toFloat(), drawY.toFloat())
  }

  fun dispose() {
    shapes.dispose()
  }

  companion object {
    const val DEFAULT_MAP_WIDTH = 320*16
    const val DEFAULT_MAP_HEIGHT = 200*16
  }
}