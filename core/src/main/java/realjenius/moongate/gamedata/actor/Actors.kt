package realjenius.moongate.gamedata.actor

import okio.Buffer
import realjenius.moongate.gamedata.map.Maps
import realjenius.moongate.gamedata.obj.GameObjectStatus
import realjenius.moongate.gamedata.obj.SpatialStore
import realjenius.moongate.io.readUByteToInt
import realjenius.moongate.io.readUShortLeToInt
import java.util.*

object Actors {
  val actors = (0 until 256).map { Actor(it) }
  val actorsByLevel = Maps.levelSpecs.map { SpatialStore<Actor>(it.level) }

  fun load(buffer: Buffer) {
    parseStage(buffer, ::objectFlags) // 0x000 - 0x100 = object flags (torches and such)
    parseStage(buffer, ::coords) // 0x100 - 0x400 = actor positions
    parseStage(buffer, ::objectRefs) // 0x400 - 0x600 = object and frame numbers
    buffer.skip(0x200) // TODO -- 0x600 - 0x800 = actor qty
    parseStage(buffer, ::statusFlags) // 0x800 - 0x900 = npc flags
    parseStage(buffer, ::strength) // 0x900 - 0xa00 - strength
    parseStage(buffer, ::dexterity) // 0xa00 - 0xb00 - dexterity
    parseStage(buffer, ::intelligence) // 0xb00 - 0xc00 - intelligence
    parseStage(buffer, ::experience) // 0xc00 - 0xe00 - experience
    parseStage(buffer, ::health) // 0xe00 - 0xf00 - health
    // -- then we move on to party stuff
    // TODO - party stuff.
    // 0ff1 - level
    // 12f1-13f1 - combat mode
    // 13f1-14f1 - magic
    // 15f1-17f1 - base obj+frame
  }

  private inline fun parseStage(buffer: Buffer, stageParser: (buffer: Buffer, actor: Actor) -> Any?) {
    for (it in actors) { stageParser(buffer, it) }
  }

  private fun objectFlags(buffer: Buffer, actor: Actor) {
    actor.objectFlags = GameObjectStatus.fromFlag(buffer.readUByteToInt())
  }

  private fun coords(buffer: Buffer, actor: Actor) {
    // TODO - duplicated logic with gameobject positions... consolidate somewhere
    val coord1 = buffer.readUByteToInt()
    val coord2 = buffer.readUByteToInt()
    val coord3 = buffer.readUByteToInt()

    actor.x = coord1 + ((coord2 and 0x3) shl 8)
    actor.y = ((coord2 and 0xfc) shr 2) + ((coord3 and 0xf) shl 6)
    actor.z = (coord3 and 0xf0) shr 4
    actorsByLevel[actor.z].add(actor.x, actor.y, actor)
  }

  private fun objectRefs(buffer: Buffer, actor: Actor) {
    val objId1 = buffer.readUByteToInt()
    val objId2 = buffer.readUByteToInt()

    actor.objectId = objId1 + ((objId2 and 0x3) shl 8)
    actor.frameNumber = (objId2 and 0xfc) shr 2

  }

  private fun statusFlags(buffer: Buffer, actor: Actor) {
    val status = buffer.readUByteToInt()

  }

  private fun strength(buffer: Buffer, actor: Actor) {
    actor.strength = buffer.readUByteToInt()
  }

  private fun dexterity(buffer: Buffer, actor: Actor) {
    actor.dexterity = buffer.readUByteToInt()
  }

  private fun intelligence(buffer: Buffer, actor: Actor) {
    actor.intelligence = buffer.readUByteToInt()
  }

  private fun experience(buffer: Buffer, actor: Actor) {
    actor.experience = buffer.readUShortLeToInt()
  }

  private fun health(buffer: Buffer, actor: Actor) {
    actor.health = buffer.readUByteToInt()
  }
}

// TODO -- val direction = frameNum / 4
data class Actor(var id: Int = 0,
                 var temp: Boolean = false,
                 var objectId: Int = 0,
                 var frameNumber: Int = 0,
                 var objectFlags: EnumSet<GameObjectStatus> = GameObjectStatus.emptySet(),
                 var x: Int = 0,
                 var y: Int = 0,
                 var z: Int = 0,
                 var strength: Int = 0,
                 var dexterity: Int = 0,
                 var intelligence: Int = 0,
                 var experience: Int = 0,
                 var health: Int = 0)