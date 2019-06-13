package marabillas.loremar.pdfparser.font.ttf

import marabillas.loremar.pdfparser.utils.exts.set

internal class TTFCMap14(val data: ByteArray, val pos: Long) : TTFCMapDefault() {
    init {
        val numVarSelectorRecords = TTFParser.getUInt32At(data, pos.toInt() + 6)
        var start = pos + 10
        for (i in 0 until numVarSelectorRecords) {
            // NOTE: Default UVS table wil be ignored for now since it requires to use another CMap table of 4 or 12 format.
            // For now all characters not mapped using non-default UVS table are treated as missing characters.

            val nonDefaultUVSOffset = TTFParser.getUInt32At(data, start.toInt() + 7)
            val numUVSMappings = TTFParser.getUInt32At(data, nonDefaultUVSOffset.toInt())
            var mappingStart = nonDefaultUVSOffset + 4
            for (j in 0 until numUVSMappings) {
                val c = getUInt24At(data, mappingStart.toInt())
                val glyphIndex = TTFParser.getUInt16At(data, mappingStart.toInt() + 3)
                map[c.toInt()] = glyphIndex
                mappingStart += 5
            }
            start += 11
        }
    }

    private fun getUInt24At(data: ByteArray, start: Int): Long {
        var num = 0L
        num = num or (data[start].toInt() and 0xff).toLong()
        num = num shl 8
        num = num or (data[start + 1].toInt() and 0xff).toLong()
        num = num shl 8
        num = num or (data[start + 2].toInt() and 0xff).toLong()
        return num
    }
}