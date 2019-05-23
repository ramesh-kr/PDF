package marabillas.loremar.pdfparser.font

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class Type1Parser(val data: ByteArray) {
    private var encryptedSection: ByteArray? = null
    private val charStrings = HashMap<String, ByteArray>()
    private val charCommands = HashMap<String, HashMap<CharCommand, IntArray>>()
    private val stringBuilder = StringBuilder()

    init {
        encryptedSection = decryptEncryptedSection()
        val lenIV = getLenIV(encryptedSection as ByteArray)
        charStrings.forEach {
            charCommands[it.key] = decodeCharString(it.value, lenIV)
        }
    }

    private fun decryptEncryptedSection(): ByteArray {
        val start = locateEncryptedSection()
        val decrypted = decryptEexec(start)
        decryptCharStrings(decrypted)
        return decrypted
    }

    private fun locateEncryptedSection(): Int {
        val eexec = charArrayOf('e', 'e', 'x', 'e', 'c')
        val chars = CharArray(5)
        var pos = 0
        while (pos + 4 < data.size) {
            for (i in 0..4) {
                chars[i] = data[pos + i].toChar()
            }
            if (chars.contentEquals(eexec)) {
                pos += 5
                while (data[pos].toChar() == '\n' || data[pos].toChar() == '\r' || data[pos].toChar() == ' ') {
                    pos++
                }
                return pos
            }
            pos++
        }
        return pos
    }

    private fun decryptEexec(start: Int): ByteArray {
        var r = 55665
        val c1 = 52845
        val c2 = 22719
        var pos = start
        val plainText = ByteArrayOutputStream()
        while (pos < data.size) {
            val plain = (data[pos].toInt() and 0xff) xor (r ushr 8)
            plainText.write(plain)
            r = ((data[pos].toInt() and 0xff) + r) * c1 + c2
            pos++
        }
        return plainText.toByteArray()
    }

    private fun decryptCharStrings(data: ByteArray) {
        var pos = locateCharStrings(data)
        charStrings.clear()
        while (pos < data.size) {
            if (data[pos].toChar() == 'e' && data[pos + 1].toChar() == 'n' && data[pos + 2].toChar() == 'd')
                break

            if (data[pos].toChar() == '/') {
                pos++
                stringBuilder.clear()
                while (data[pos].toChar() != ' ') {
                    stringBuilder.append(data[pos].toChar())
                    pos++
                }
                pos++
                var length = 0
                while (data[pos].toChar() != ' ') {
                    val num = Character.getNumericValue(data[pos].toChar())
                    if (num < 0) throw NumberFormatException(
                        "Attempting to get length of charstring but can not" +
                                " evaluate to a number"
                    )
                    length = length * 10 + num
                    pos++
                }
                while (data[pos].toChar() != '/') {
                    if (data[pos].toChar() == 'R' && data[pos + 1].toChar() == 'D') {
                        // Skip RD and a space
                        pos += 3
                        decryptCharString(data, pos, length)
                        pos += length
                        break
                    }
                    pos++
                }
            }
            pos++
        }
    }

    private fun decryptCharString(data: ByteArray, start: Int, length: Int) {
        var r = 4330
        val c1 = 52845
        val c2 = 22719
        var pos = start
        val plainText = ByteArrayOutputStream()
        repeat(length) {
            val plain = (data[pos].toInt() and 0xff) xor (r ushr 8)
            plainText.write(plain)
            r = ((data[pos].toInt() and 0xff) + r) * c1 + c2
            pos++
        }
        charStrings[stringBuilder.toString()] = plainText.toByteArray()
    }

    private fun locateCharStrings(data: ByteArray): Int {
        val charStrings = charArrayOf('/', 'C', 'h', 'a', 'r', 'S', 't', 'r', 'i', 'n', 'g', 's')
        val ch = charArrayOf('/', 'C', 'h')
        val chars = CharArray(3)
        val bigChars = CharArray(12)
        var pos = 0
        while (pos + 11 < data.size) {
            for (i in 0..2) {
                chars[i] = data[pos + i].toChar()
            }
            if (chars.contentEquals(ch)) {
                for (i in 0..11) {
                    bigChars[i] = data[pos + i].toChar()
                }
                if (bigChars.contentEquals(charStrings))
                    return pos + 12
            }
            pos++
        }
        return pos
    }

    private fun decodeCharString(charString: ByteArray, skipBytes: Int = 4): HashMap<CharCommand, IntArray> {
        var i = skipBytes
        val operands = mutableListOf<Int>()
        val commands = HashMap<CharCommand, IntArray>()
        while (i < charString.size) {
            when (val byte = charString[i].toInt() and 0xFF) {
                in 32..246 -> operands.add(byte - 139)
                in 247..250 -> {
                    val w = charString[++i].toInt() and 0xFF
                    operands.add((byte - 247) * 256 + w + 108)
                }
                in 251..254 -> {
                    val w = charString[++i].toInt() and 0xFF
                    operands.add(-(byte - 247) * 256 - w - 108)
                }
                255 -> {
                    val buffer = ByteBuffer.allocate(4)
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    repeat(4) {
                        buffer.put((charString[++i].toInt() and 0xFF).toByte())
                    }
                    buffer.flip()
                    operands.add(buffer.int)
                }
                in 0..31 -> {
                    if (byte != 12) {
                        saveCommand(byte, operands, commands)
                    } else {
                        val commandPlus = charString[++i].toInt() and 0xFF
                        saveCommandPlus(commandPlus, operands, commands)
                    }
                }
            }
            i++
        }
        return commands
    }

    private fun saveCommand(byte: Int, operands: MutableList<Int>, commands: HashMap<CharCommand, IntArray>) {
        when (byte) {
            1 -> saveCommandAndClear(commands, CharCommand.HSTEM, operands, 1)
            3 -> saveCommandAndClear(commands, CharCommand.VSTEM, operands, 2)
            4 -> saveCommandAndClear(commands, CharCommand.VMOVETO, operands, 1)
            5 -> saveCommandAndClear(commands, CharCommand.RLINETO, operands, 2)
            6 -> saveCommandAndClear(commands, CharCommand.HLINETO, operands, 1)
            7 -> saveCommandAndClear(commands, CharCommand.VLINETO, operands, 1)
            8 -> saveCommandAndClear(commands, CharCommand.RRCURVETO, operands, 6)
            9 -> saveCommandAndClear(commands, CharCommand.CLOSEPATH, operands, 0)
            10 -> commands[CharCommand.CALLSUBR] = intArrayOf(operands.last())
            11 -> commands[CharCommand.RETURN] =
                intArrayOf() // TODO return needs to get result from callsubr and add to operands.
            13 -> saveCommandAndClear(commands, CharCommand.HSBW, operands, 2)
            14 -> saveCommandAndClear(commands, CharCommand.ENDCHAR, operands, 0)
            21 -> saveCommandAndClear(commands, CharCommand.RMOVETO, operands, 2)
            22 -> saveCommandAndClear(commands, CharCommand.HMOVETO, operands, 1)
            30 -> saveCommandAndClear(commands, CharCommand.VHCURVETO, operands, 4)
            31 -> saveCommandAndClear(commands, CharCommand.HVCURVETO, operands, 4)

        }
    }

    private fun saveCommandPlus(byte: Int, operands: MutableList<Int>, commands: HashMap<CharCommand, IntArray>) {
        when (byte) {
            0 -> saveCommandAndClear(commands, CharCommand.DOTSECTION, operands, 0)
            1 -> saveCommandAndClear(commands, CharCommand.VSTEM3, operands, 6)
            2 -> saveCommandAndClear(commands, CharCommand.HSTEM3, operands, 6)
            6 -> saveCommandAndClear(commands, CharCommand.SEAC, operands, 5)
            7 -> saveCommandAndClear(commands, CharCommand.SBW, operands, 4)
            12 -> {
                // Perform DIV
                val dividend = operands[operands.lastIndex - 1]
                val divisor = operands.last()
                val quotient = Math.round(dividend.toFloat() / divisor.toFloat())
                operands.remove(dividend)
                operands.remove(divisor)
                operands.add(quotient)
            }
            16 -> commands[CharCommand.CALLOTHERSUBR] = operands.toIntArray()
            17 -> commands[CharCommand.POP] =
                intArrayOf() // TODO pop needs to get result from callothersubr and add to operands.
            33 -> saveCommandAndClear(commands, CharCommand.SETCURRENTPOINT, operands, 2)
        }
    }

    private fun saveCommandAndClear(
        commands: HashMap<CharCommand, IntArray>,
        command: CharCommand,
        operands: MutableList<Int>,
        numOperands: Int
    ) {
        val input = IntArray(numOperands)
        if (numOperands > 0) {
            for (num in numOperands downTo 1) {
                input[numOperands - num] = operands[operands.count() - num]
            }
        }
        commands[command] = input
        operands.clear()
    }

    internal enum class CharCommand {
        ENDCHAR,
        HSBW,
        SEAC,
        SBW,
        CLOSEPATH,
        HLINETO,
        HMOVETO,
        HVCURVETO,
        RLINETO,
        RMOVETO,
        RRCURVETO,
        VHCURVETO,
        VLINETO,
        VMOVETO,
        DOTSECTION,
        HSTEM,
        HSTEM3,
        VSTEM,
        VSTEM3,
        DIV,
        CALLOTHERSUBR,
        CALLSUBR,
        POP,
        RETURN,
        SETCURRENTPOINT
    }

    private fun getLenIV(data: ByteArray): Int {
        var pos = 0
        while (pos + 5 < data.size) {
            if (
                data[pos].toChar() == '/'
                && data[pos + 1].toChar() == 'l'
                && data[pos + 2].toChar() == 'e'
                && data[pos + 3].toChar() == 'n'
                && data[pos + 4].toChar() == 'I'
                && data[pos + 5].toChar() == 'V'
            ) {
                // Move to position after next space
                pos += 7
                var lenIV = 0
                while (data[pos].toChar() != ' ') {
                    val num = Character.getNumericValue(data[pos].toChar())
                    if (num < 0) throw java.lang.NumberFormatException(
                        "Attempting to get value for lenIV but can not " +
                                "evaluate to a number"
                    )
                    lenIV = lenIV * 10 + num
                }
                return lenIV
            } else if (
                pos + 11 < data.size
                && data[pos].toChar() == '/'
                && data[pos + 1].toChar() == 'C'
                && data[pos + 2].toChar() == 'h'
                && data[pos + 3].toChar() == 'a'
                && data[pos + 4].toChar() == 'r'
                && data[pos + 5].toChar() == 'S'
                && data[pos + 6].toChar() == 't'
                && data[pos + 7].toChar() == 'r'
                && data[pos + 8].toChar() == 'i'
                && data[pos + 9].toChar() == 'n'
                && data[pos + 10].toChar() == 'g'
                && data[pos + 11].toChar() == 's'
            ) {
                break
            }
            pos++
        }

        return 4 // Return default number of random bytes in charstring
    }
}