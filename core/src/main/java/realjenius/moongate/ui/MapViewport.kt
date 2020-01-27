package realjenius.moongate.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import realjenius.moongate.gamedata.actor.Actors
import realjenius.moongate.gamedata.map.Maps
import realjenius.moongate.gamedata.obj.GameObjects
import realjenius.moongate.gamedata.palette.Palettes
import realjenius.moongate.gamedata.tile.Tile
import realjenius.moongate.gamedata.tile.Tiles

class MapViewport(private val level: Int = 0, width: Int = DEFAULT_MAP_WIDTH, private val height: Int = DEFAULT_MAP_HEIGHT) : UIComponent {
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

  override fun input() {
    if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) shift(x = -1)
    if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) shift(x = 1)
    if (Gdx.input.isKeyPressed(Input.Keys.UP)) shift(y = -1)
    if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) shift(y = 1)
  }

  override fun update() {
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

  override fun render(camera: OrthographicCamera, batch: SpriteBatch) {
    shapes.begin(ShapeRenderer.ShapeType.Filled)
    renderMapTiles(batch)
    renderObjects(batch, TileRenderState.BottomObj)
    renderObjects(batch, TileRenderState.PlainObj)
    renderActors(batch)
    renderObjects(batch, TileRenderState.TopObj)
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
        else renderTile(tileIndex, batch, x, y, TileRenderState.Map)
      }
    }
  }

  private fun renderObjects(batch: SpriteBatch, renderState: TileRenderState) {
    (0 until numTilesY).forEach { y ->
      (0 until numTilesX).forEach { x ->
        val objsAt = GameObjects.objectsByLevel[level].get(originX + x, originY + y, 0)
        objsAt.asReversed().forEach { obj ->
          if (obj.isVisible()) renderTile(obj.tileId, batch, x, y, renderState)
        }
      }
    }
  }

  private fun renderActors(batch: SpriteBatch) {
    (0 until numTilesY).forEach { y ->
      (0 until numTilesX).forEach { x ->

        val actorsAt = Actors.spatialStore.get(originX + x, originY + y, 0)
        actorsAt.forEach {
          renderTile(GameObjects.baseTiles[it.objectId].tile + it.frameNumber, batch, x, y, TileRenderState.Actor)
        }
      }
    }
  }


  private fun renderTile(tileIndex: Int, batch: SpriteBatch, x: Int, y: Int, renderState: TileRenderState) {
    val tile = Tiles.tiles[tileIndex]
    val drawX = x * 16
    val drawY = (height) - y * 16
    val paletteIndex = Palettes.currentPalette()

    if (!renderState.shouldRender(tile)) return

    if (renderState == TileRenderState.Map && Tiles.isAnimatedMapTile(tileIndex))
      batch.draw(Tiles.getAnimatedBaseTile(tileIndex).spriteForPalette(paletteIndex), drawX.toFloat(), drawY.toFloat())

    batch.draw(tile.spriteForPalette(paletteIndex), drawX.toFloat(), drawY.toFloat())

    if(renderState.supportsMultiTile()) {
      var multiTileIndex = tileIndex
      val leftX = (x-1) * 16
      val upY = (height) - (y-1) * 16
      if (x > 0 && tile.isDoubleWidth()) {
        multiTileIndex--
        val leftTile = Tiles.tiles[multiTileIndex]
        batch.draw(leftTile.spriteForPalette(paletteIndex), leftX.toFloat(), drawY.toFloat())
      }
      if (y > 0 && tile.isDoubleHeight()) {
        multiTileIndex--
        val upTile = Tiles.tiles[multiTileIndex]
        batch.draw(upTile.spriteForPalette(paletteIndex), drawX.toFloat(), upY.toFloat())
      }
      if (x > 0 && y > 0 && tile.isDoubleWidth() && tile.isDoubleHeight()) {
        multiTileIndex--
        val diagTile = Tiles.tiles[multiTileIndex]
        batch.draw(diagTile.spriteForPalette(paletteIndex), leftX.toFloat(), upY.toFloat())
      }
    }
  }

  override fun dispose() {
    shapes.dispose()
  }

  companion object {
    const val DEFAULT_MAP_WIDTH = 320*16
    const val DEFAULT_MAP_HEIGHT = 200*16
  }

  private enum class TileRenderState {
    Map, BottomObj, PlainObj, TopObj, Actor;

    fun supportsMultiTile() = this != Map

    fun shouldRender(tile: Tile) : Boolean {
      return when (this) {
        BottomObj -> tile.isBottomTile()
        PlainObj -> !tile.isBottomTile() && !tile.isTopTile()
        TopObj -> tile.isTopTile()
        else -> true
      }
    }
  }
}