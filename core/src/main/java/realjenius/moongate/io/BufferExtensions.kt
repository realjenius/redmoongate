package realjenius.moongate.io

import okio.Buffer

fun Buffer.readUByte() = this.readByte().toUByte()
fun Buffer.readUIntLe() = this.readIntLe().toUInt()