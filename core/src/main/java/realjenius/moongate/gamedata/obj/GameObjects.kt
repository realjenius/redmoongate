package realjenius.moongate.gamedata.obj

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okio.Buffer
import okio.buffer
import okio.source
import realjenius.moongate.gamedata.actor.Actors
import realjenius.moongate.gamedata.map.Maps
import realjenius.moongate.io.GameFiles
import realjenius.moongate.io.LZW
import realjenius.moongate.io.readUByteToInt
import realjenius.moongate.io.readUShortLeToInt
import java.util.*

object GameObjects {
  private const val WEIGHTS_SKIP = 4096L

  lateinit var baseTiles: List<BaseTile>

  val objectsByLevel = Maps.levelSpecs.map {
    SpatialStore<GameObject>(it.level)
  }

  fun load() {
    loadBaseTiles()
    loadObjectWeights()

    Buffer().apply {
      LZW.decompressInto(GameFiles.loadExternal("LZOBJBLK"), this)
      (0 until Maps.levelSpecs[0].chunkCount).forEach { _ -> readObjectChunk(this, objectsByLevel[0]) }
    }

    val dungeonState = Buffer().apply {
      LZW.decompressInto(GameFiles.loadExternal("LZDNGBLK"), this)
      (1 until objectsByLevel.size).forEach {
        readObjectChunk(this, objectsByLevel[it])
      }
    }
    Actors.load(dungeonState)


    // TODO - read the rest of the save game stuff.
    /*
    f00 - party names
    fe0 - party roster
    ff0 - number in party
    14f1 - movement points
    17f1 - talk flags
    19f1 - movement flags
    1bf1 - quest flag
    1bf3 - game time
    1bf9 - karma
    1bfa - wind direction
    1c03 - timers
    1c12 - eclipse
    1c17 - alcohol
    1c1b - moonstones
    1c5f - gargish lang
    1c69 - combat mode
    1c6a - solo mode
    1c6c - command bar
    1c72 - portrait byte


    // potrait 0x1c72 (after the rest) -- single byte
    // clock
    // actors
    // party
    // player
    // weather
    // command bar 0x1c6c - single byte

     */
  }

  private fun loadObjectWeights() {
    GameFiles.loadExternal("TILEFLAG").source().buffer().use { weights ->
      weights.skip(WEIGHTS_SKIP)
      baseTiles.forEach { template ->
        template.weight = weights.readUByteToInt()
      }
    }
  }

  private fun loadBaseTiles() {
    val types = GameFiles.loadInternal("object_types.json").read().use {
      jacksonObjectMapper().readValue(it, GameObjectTypes::class.java)
    }

    baseTiles = GameFiles.loadExternal("BASETILE").source().buffer().use { basetiles ->
      (0 until 1024).map {
        val tileId = basetiles.readUShortLeToInt()
        BaseTile(it,types.byId[it] ?: GameObjectType(it, "UNKNOWN"), tileId)
      }
    }
  }

  private fun readObjectChunk(gameState: Buffer, store: SpatialStore<GameObject>) : Int {
    val objCount = gameState.readUShortLeToInt()
    (0 until objCount).forEach { _ ->

      val status = gameState.readUByteToInt()

      val coord1 = gameState.readUByteToInt()
      val coord2 = gameState.readUByteToInt()
      val coord3 = gameState.readUByteToInt()
      val x = coord1 + ((coord2 and 0x3) shl 8)
      val y = ((coord2 and 0xfc) shr 2) + ((coord3 and 0xf) shl 6)
      val z = (coord3 and 0xf0) shr 4

      val objectId1 = gameState.readUByteToInt()
      val objectId2 = gameState.readUByteToInt()

      val objId = objectId1 + ((objectId2 and 0x3) shl 8)
      val frameNum = (objectId2 and 0xfc) shr 2

      val quantity = gameState.readUByteToInt()
      val quality = gameState.readUByteToInt()

      val template = baseTiles[objId]
      store.add(x, y, GameObject(template, GameObjectStatus.fromFlag(status), frameNum))
    }
    return objCount
  }
}

enum class GameObjectStatus(private val id: Int) {
  Invisible(0x2), Charmed(0x4), OnMap(0x0), InContainer(0x8),
  InInventory(0x10), Readied(0x18), Temporary(0x20), Lit(0x80);

  fun matches(flag: Int) = this.id and flag > 0

  companion object {
    fun fromFlag(flag: Int) = emptySet().apply { addTo(this, flag) }
    fun emptySet() = EnumSet.noneOf(GameObjectStatus::class.java)
    fun addTo(set: EnumSet<GameObjectStatus>, flag: Int) = values().filterTo(set) { it.matches(flag) }
  }
}

data class BaseTile(val objectTemplateId: Int, val type: GameObjectType, val tile: Int, var weight: Int = 0)

data class GameObject(val objectTemplate: BaseTile, var status: EnumSet<GameObjectStatus> = GameObjectStatus.emptySet(), var frameNum: Int = 0) {
  val tileId: Int
    get() = objectTemplate.tile + frameNum

  fun isVisible() = !status.contains(GameObjectStatus.Invisible)
}

data class GameObjectTypes(val types: List<GameObjectType>) {
  val byId: Map<Int, GameObjectType> by lazy { types.map { it.id to it }.toMap() }
}

data class GameObjectType(val id: Int,
                          val name: String,
                          val weightModifier: Double? = null,
                          val container: Boolean = false,
                          val containerCondition: String? = null,
                          val stackable: Boolean = false,
                          val stackCondition: String? = null,
                          val breakable: Boolean = false,
                          val corpse: Boolean = false,
                          val uses: List<GameObjectUse> = listOf())

data class GameObjectUse(val triggers: List<String>, val frame: Int = -1, val distance: Int = 0, val action: String)