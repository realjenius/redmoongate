package realjenius.moongate.io

import okio.BufferedSource

fun BufferedSource.readByteToInt() = this.readByte().toInt()
fun BufferedSource.readUByte() = this.readByte().toUByte()
fun BufferedSource.readUByteToInt() = this.readUByte().toInt()
fun BufferedSource.readUIntLe() = this.readIntLe().toUInt()
fun BufferedSource.readUShortLe() = this.readShortLe().toUShort()
fun BufferedSource.readUShortLeToInt() = this.readUShortLe().toInt()