package marabillas.loremar.pdfparser.filters

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ASCIIHexTest {
    @Test
    fun printHexMap() {
        for ((key, value) in ASCIIHex.hexMap.toSortedMap()) {
            println("$key = $value")
        }
    }

    @Test
    fun testDecode() {
        var hexString = "48656c6c6f20576f726c64"
        val expectedString = "Hello World"
        val s = ASCIIHex().decodeToString(hexString)
        assertThat(s, `is`(expectedString))
        println("Given hexadecimal $hexString was successfully decoded to $expectedString.")

        hexString = "1323"
        val expectedNumber = 4899.toBigInteger()
        val big = ASCIIHex().decodeToBigInteger(hexString)
        assertThat(big, `is`(expectedNumber))
        println("Given hexadecimal $hexString was successfully decoded to $expectedNumber.")
    }
}