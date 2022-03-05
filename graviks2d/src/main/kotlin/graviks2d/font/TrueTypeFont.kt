package graviks2d.font

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.*

private class TrueTypeTable(
    val tag: String,
    val offset: Int,
    val length: Int
) {
    lateinit var content: ByteArray

    override fun toString(): String {
        return "Table($tag,offset=$offset,length=$length)"
    }
}

class TrueTypeFont(dataInputStream: DataInputStream) {

    val ofaScaler: Int

    init {
        var currentStreamOffset = 0
        ofaScaler = dataInputStream.readInt()

        val numTables = dataInputStream.readUnsignedShort()

        // Skip searchRange, entrySelector, and rangeShift
        dataInputStream.skipNBytes(6)

        currentStreamOffset += 12

        val tables = (0 until numTables).map {
            val rawTag = (0 until 4).map { dataInputStream.readUnsignedByte().toChar() }
            val tag = "${rawTag[0]}${rawTag[1]}${rawTag[2]}${rawTag[3]}".lowercase(Locale.ROOT)

            // Ignore checksum for now
            dataInputStream.skipNBytes(4)

            val offset = dataInputStream.readInt()
            val length = dataInputStream.readInt()
            currentStreamOffset += 16

            TrueTypeTable(tag, offset, length)
        }.sortedBy { it.offset }

        println("Tables are:")
        for (table in tables) {
            println("  $table")
        }

        for (table in tables) {
            dataInputStream.skipNBytes((table.offset - currentStreamOffset).toLong())
            currentStreamOffset = table.offset

            table.content = dataInputStream.readNBytes(table.length)
            currentStreamOffset += table.length
        }

        for (table in tables) {
            val dataInput = DataInputStream(ByteArrayInputStream(table.content))
            if (table.tag == "name") {
                parseNameTable(dataInput)
            }

            if (table.tag == "cmap") {
                parseCmapTable(dataInput)
            }

            if (table.tag == "glyf") {
                parseGlyfTable(dataInput)
            }
        }
    }

    private fun parseNameTable(dataInput: DataInputStream) {
        println("Name table:")
        val format = dataInput.readUnsignedShort()
        val count = dataInput.readUnsignedShort()
        val stringOffset = dataInput.readUnsignedShort()
        var currentOffset = 6
        println("format is $format and count is $count and stringOffset is $stringOffset")

        class NameRecord(
            val platformId: Int,
            val encoding: Int,
            val languageId: Int,
            val nameId: Int,
            val length: Int,
            val offset: Int
        ) {
            lateinit var name: String
        }

        val nameRecords = (0 until count).map {
            val platformId = dataInput.readUnsignedShort()
            val encoding = dataInput.readUnsignedShort()
            val languageId = dataInput.readUnsignedShort()
            val nameId = dataInput.readUnsignedShort()
            val length = dataInput.readUnsignedShort()
            val offset = dataInput.readUnsignedShort()
            currentOffset += 12
            NameRecord(
                platformId = platformId, encoding = encoding, languageId = languageId, nameId = nameId, length = length, offset = offset
            )
        }

        if (stringOffset - currentOffset > 0) {
            dataInput.skipNBytes((stringOffset - currentOffset).toLong())
        }
        val stringBytes = dataInput.readAllBytes()

        for (nameRecord in nameRecords) {
            val nameInput = DataInputStream(ByteArrayInputStream(stringBytes.sliceArray(nameRecord.offset until nameRecord.offset + nameRecord.length)))
            nameRecord.name = (0 until nameRecord.length / 2).map { nameInput.readChar() }.toCharArray().concatToString()
        }
        println()
    }

    private fun parseCmapTable(dataInput: DataInputStream) {
        println("Cmap table:")
        val versionNumber = dataInput.readUnsignedShort()
        val numCmapTables = dataInput.readUnsignedShort()
        var currentOffset = 4
        println("cmap version number is $versionNumber and #cmap tables is $numCmapTables")

        class CmapTable(
            val platformId: Int,
            val encoding: Int,
            val offset: Int
        ) {
            override fun toString(): String {
                return "CmapTable(platformId=$platformId,encoding=$encoding,offset=$offset)"
            }
        }

        val cmapTables = (0 until numCmapTables).map {
            val platformId = dataInput.readUnsignedShort()
            val encoding = dataInput.readUnsignedShort()
            val offset = dataInput.readInt()
            currentOffset += 8
            CmapTable(platformId = platformId, encoding = encoding, offset = offset)
        }
        println("cmap tables are:")
        for (table in cmapTables) {
            println("  $table")
        }

        val remainingBytes = dataInput.readAllBytes()
        for (table in cmapTables) {
            if (table.platformId == 0) {
                val startIndex = table.offset - currentOffset
                val tableInput = DataInputStream(ByteArrayInputStream(remainingBytes.sliceArray(startIndex until remainingBytes.size)))

                val format = tableInput.readUnsignedShort()
                // TODO Maybe add support for format 4
                if (format == 12) {
                    tableInput.readUnsignedShort() // Skip reserved bytes
                    tableInput.readInt() // We don't need to know the table length
                    tableInput.readInt() // Language should simply be 0

                    class CmapGroup(
                        val startCharCode: Int,
                        val endCharCode: Int,
                        val startGlyphCode: Int
                    ) {
                        override fun toString(): String {
                            return "CmapGroup(charRange=[$startCharCode,$endCharCode],glyphCode=$startGlyphCode)"
                        }
                    }

                    val numGroups = tableInput.readInt()
                    val groups = (0 until numGroups).map {
                        val startCharCode = tableInput.readInt()
                        val endCharCode = tableInput.readInt()
                        val startGlyphCode = tableInput.readInt()
                        CmapGroup(
                            startCharCode = startCharCode,
                            endCharCode = endCharCode,
                            startGlyphCode = startGlyphCode
                        )
                    }

//                    println("The groups are:")
//                    for (group in groups) {
//                        println("  $group")
//                    }
                }
            }
            // TODO maybe add support for other platforms
        }

        println()
    }

    private fun parseGlyfTable(dataInput: DataInputStream) {
        // TODO I'm not certain at all that this is how I should interpret it
        val numContours = dataInput.readShort().toInt()

        if (numContours >= 0) {
            println("Parse simple glyph...")
            val xMin = dataInput.readShort()
            val yMin = dataInput.readShort()
            val xMax = dataInput.readShort()
            val yMax = dataInput.readShort()
            println("#contours is $numContours, range is ($xMin,$yMin,$xMax,$yMax)")
            val endContourIndices = Array(numContours) { dataInput.readUnsignedShort() }
            val numInstructions = dataInput.readUnsignedByte()
            val instructions = Array(numInstructions) { dataInput.readUnsignedByte() }
            println("endContourIndices are ${endContourIndices.contentToString()} and instructions are ${instructions.contentToString()}")
        } else {
            println("Parse compound glyph...")
        }

    }
}
