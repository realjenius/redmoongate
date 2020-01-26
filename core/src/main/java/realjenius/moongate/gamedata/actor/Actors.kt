package realjenius.moongate.gamedata.actor

import okio.Buffer
import realjenius.moongate.io.readUByteToInt

object Actors {
  private val actors = (0 until 256).map { Actor(it) }

  fun load(buffer: Buffer) {
    parseStage(buffer, ::actorFlags) // 0x000 - 0x100 = object flags (torches and such)
    parseStage(buffer, ::actorCoords) // 0x100 - 0x400 = actor positions
    // 0x400 - 0x600 = object and frame numbers

    // 0x600 - 0x800 = actor qty

    // 0x800 - 0x900 = npc flags

    // 0x900 - 0xa00 = npc strength
    // 0x900 - 0xb00 = npc dexterity
    // 0x900 - 0xc00 = npc intelligence
    // 0xc00 - 0xe00 = Experience
    // 0xe00 - 0xf00 = Health
    // -- then we move on to party stuff
  }

  private inline fun parseStage(buffer: Buffer, stageParser: (buffer: Buffer, actor: Actor) -> Unit) {
    actors.forEach { stageParser(buffer, it) }
  }

  private fun actorFlags(buffer: Buffer, actor: Actor) {
    actor.objectFlags = buffer.readUByteToInt()
  }

  private fun actorCoords(buffer: Buffer, actor: Actor) {
    // TODO - duplicated logic with gameobject positions... consolidate somewhere
    val coord1 = buffer.readUByteToInt()
    val coord2 = buffer.readUByteToInt()
    val coord3 = buffer.readUByteToInt()

    val x = coord1 + ((coord2 and 0x3) shl 8)
    val y = ((coord2 and 0xfc) shr 2) + ((coord3 and 0xf) shl 6)
    val z = (coord3 and 0xf0) shr 4
  }

  private fun actorObjectRefs(buffer: Buffer, actor: Actor) {
    /*
    b1 = objlist->read1();
    b2 = objlist->read1();
    actors[i]->obj_n = b1;
    actors[i]->obj_n += (b2 & 0x3) << 8;

    actors[i]->frame_n = (b2 & 0xfc) >> 2;
    actors[i]->direction = actors[i]->frame_n / 4;
    if(actors[i]->obj_n == 0) //Hack to get rid of Exodus.
    {
    	actors[i]->x = 0;
    	actors[i]->y = 0;
    	actors[i]->z = 0;
    }
   }
     */
  }
}

data class Actor(var id: Int = 0,
                 var temp: Boolean = false,
                 var objectFlags: Int = 0)