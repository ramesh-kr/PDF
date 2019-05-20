package marabillas.loremar.pdfparser.contents.text

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.contents.ContentGroup
import marabillas.loremar.pdfparser.font.Font
import marabillas.loremar.pdfparser.objects.Numeric
import marabillas.loremar.pdfparser.objects.PDFArray
import marabillas.loremar.pdfparser.objects.PDFString
import marabillas.loremar.pdfparser.objects.toPDFString
import marabillas.loremar.pdfparser.toDouble
import marabillas.loremar.pdfparser.toInt
import java.util.*
import kotlin.collections.ArrayList

internal class TextContentAnalyzer(textObjs: MutableList<TextObject> = mutableListOf()) {
    internal val contentGroups = ArrayList<ContentGroup>()
    private val textObjects = mutableListOf<TextObject>()
    private val sb = StringBuilder()

    private var currTextGroup = TextGroup()
    private var table = Table()
    private var currLine = ArrayList<TextElement>()

    private val prevLineWideSpaces = mutableListOf<WideSpace>()
    private val fonts = SparseArrayCompat<Font>()

    init {
        textObjects.clear()
        textObjects.addAll(textObjs)
    }

    private fun resetAnalyzer() {
        contentGroups.clear()
        textObjects.clear()
        sb.clear()
        currTextGroup = TextGroup()
        table = Table()
        currLine.clear()
        prevLineWideSpaces.clear()
        fonts.clear()
    }

    fun analyze(
        textObjs: MutableList<TextObject>,
        fonts: SparseArrayCompat<Font>
    ): ArrayList<ContentGroup> {
        resetAnalyzer()

        textObjects.addAll(textObjs)

        for (i in 0 until fonts.size()) {
            this.fonts.put(
                fonts.keyAt(i),
                fonts[fonts.keyAt(i)]
            )
        }

        // If tj values are arrays resulting from TJ operator, determine from the number values between strings
        // whether to add space or not while concatenating strings. First to get glyph width for space, get all the
        // negative numbers and identify the negative number with most occurrences. Rule: If the absolute value of a
        // negative number is less than 15% of the space width, don't add space. If it is greater than 115%,
        // then add double space. Otherwise, add space. If number is positive don't add space.
        handleTJArrays()

        // Tables are detected by looking for wide spaces placed on top of each other. These wide spaces serve as
        // dividers between table columns. Tables are also detected by looking for multi-linear TextObjects placed
        // horizontally adjacent to each other.
        detectTableComponents()

        // Group texts in the same line or in adjacent lines with line-spacing less than font size.
        groupTexts()

        // Check if lines end with a period. If yes, then lines stay as they were. If not, then proceed analysis.
        checkForListTypeTextGroups()

        // Estimate the width of the page by getting the largest width of a line of texts
        val w = getLargestWidth()
        // TODO Get width of each line using given character widths. Else if character widths are not provided then use
        // previous method for getting largest width.

        // If a line ends with '-', then append the next line to this line and remove the '-' character.
        concatenateDividedByHyphen()

        // If line is almost as long as the width of page, then append the next line in the TextGroup.
        formParagraphs(w)

        // Convert adjacent elements with same tf into one element
        mergeElementsWithSameFont()

        deleteBlankLines()

        return contentGroups
    }

    internal fun handleTJArrays() {
        textObjects.forEach { texObj ->
                val spW = getSpaceWidth(texObj)
            println("space width = $spW")
                handleSpacing(spW, texObj)
            }
    }

    private fun getSpaceWidth(textObj: TextObject): Float {
        var top = 0f
        val negs = HashMap<Float, Int>()
        textObj
            .asSequence()
            .filter { textElement ->
                // Get all arrays
                textElement.tj is PDFArray
            }
            .forEach { tjArray ->
                (tjArray.tj as PDFArray)
                    .filter {
                        // Get all negative numbers in array
                        it is Numeric && it.value.toFloat() < 0
                    }
                    .forEach { neg ->
                        // Increment a negative number's count. If this number has the biggest count(bigger than the
                        // current top number's count), then save it as the top number.
                        val num = -(neg as Numeric).value.toFloat()
                        val count = negs[num] ?: 0
                        negs[num] = count + 1
                        if (negs[num] ?: 0 > negs[top] ?: 0) {
                            top = num
                        }
                    }
            }
        // The number save as top is considered as the width of space.
        return top
    }

    private fun handleSpacing(width: Float, textObj: TextObject) {
        textObj.forEachIndexed { index, textElement ->
                if (textElement.tj is PDFArray) {
                    sb.clear().append('(')
                    (textElement.tj).forEach {
                            if (it is PDFString)
                                sb.append(it.value) // If string, append
                            else if (it is Numeric) {
                                val num = -it.value.toFloat()
                                if (num >= 1.15 * width)
                                    sb.append(' ').append(' ') // If more than 115% of space width, add double space
                                else if (num >= 0.15 * width)
                                    sb.append(' ') // If between 15% or 115% of space width, add space
                            }
                        }
                    sb.append(')')
                    val transformed = TextElement(
                        td = textElement.td,
                        tf = textElement.tf,
                        ts = textElement.ts,
                        tj = sb.toString().toPDFString()
                    )
                    textObj.update(transformed, index)
                }
            }
    }

    internal fun detectTableComponents() {
        if (fonts.size() > 0) {
            detectTables()
        }
        detectMultiLinearColumns()
    }

    private fun detectTables() {
        val currLineWideSpaces = mutableListOf<WideSpace>()
        var belowColumn = 0
        var aboveColumn = 0
        var w = 0
        textObjects.forEachIndexed { i, textObj ->
            // Check if another text object exists following the current text object on the same line
            if (i + 1 < textObjects.count() && textObj.td[1] == textObjects[i + 1].td[1]) {
                val rbArr = getLocationAndSpaceWidthOfRightBoundary(textObj)
                val diff = textObjects[i + 1].td[0] - rbArr[0]

                // Check if the distance between the two TextObjects is wider than 3 space characters.
                if (rbArr[1] != Float.MAX_VALUE && diff > (rbArr[1] * 3)) {
                    val currWideSpace = WideSpace(
                        rbArr[0],
                        textObjects[i + 1].td[0],
                        i,
                        i + 1
                    )
                    currLineWideSpaces.add(currWideSpace)

                    // Iterate through wide spaces of the previous line.
                    while (w < prevLineWideSpaces.count()) {
                        val aboveWideSpace = prevLineWideSpaces[w]

                        // Check if current WideSpace and selected WideSpace from previous line forms a divider between
                        // two columns in a table.
                        if (isFormingColumnDivider(currWideSpace, aboveWideSpace, rbArr[1])) {
                            aboveWideSpace.isDivider = true
                            currWideSpace.isDivider = true

                            // Set column number for text objects at the left of column boundary and whose column numbers
                            // are not set.
                            // Set column number for text objects in previous line.
                            setColumnNumberForTextObjectsToLeft(aboveWideSpace.leftTextObj, aboveColumn)
                            // Set column number for text objects in current line.
                            setColumnNumberForTextObjectsToLeft(i, belowColumn)

                            aboveColumn++
                            belowColumn = aboveColumn

                            // If column number is already set for text objects to the right and is less than  or equal to the
                            // current column number, adjust their column numbers.
                            var r = aboveWideSpace.rightTextObj
                            if (textObjects[r].column != -1 && textObjects[r].column <= aboveColumn) {
                                val inc = aboveColumn - textObjects[r].column + 1
                                textObjects[r].column += inc
                                r++
                                while (r < textObjects.count()) {
                                    if (textObjects[r].column == -1)
                                        break
                                    if (textObjects[r].td[1] != textObjects[r - 1].td[1])
                                        break
                                    else
                                        textObjects[r].column += inc
                                    r++
                                }
                            }
                            break
                        } else if (aboveWideSpace.left > currWideSpace.right) {
                            // Allow the previous WideSpace to be checked in the next iteration.
                            w--
                            break
                        } else if (aboveWideSpace.isDivider) {
                            aboveColumn++
                            w++
                        } else {
                            w++
                        }
                    }
                }
            } else { // If current TextObject is the last one in the line.
                if (belowColumn > 0) {
                    setColumnNumberForTextObjectsToLeft(i, belowColumn)
                    // Set column number for remaining text objects from previous line whose column number is not set yet
                    val aboveWideSpace = prevLineWideSpaces[w]
                    setColumnNumberForTextObjectsToLeft(aboveWideSpace.leftTextObj, aboveColumn)
                    var j = aboveWideSpace.rightTextObj
                    while (j < textObjects.count()) {
                        if (
                            textObjects[j].column >= 0 ||
                            textObjects[j].td[1] != textObjects[j - 1].td[1]
                        ) break

                        textObjects[j].column = aboveColumn
                        j++
                    }
                }
                belowColumn = 0
                aboveColumn = 0
                w = 0
                prevLineWideSpaces.clear()
                prevLineWideSpaces.addAll(currLineWideSpaces)
                currLineWideSpaces.clear()
            }
        }
    }

    private fun setColumnNumberForTextObjectsToLeft(start: Int, column: Int) {
        var i = start
        while (i >= 0) {
            if (
                textObjects[i].column >= 0 ||
                textObjects[i].td[1] != textObjects[i + 1].td[1]
            ) break

            textObjects[i].column = column
            i--
        }
    }

    private fun isFormingColumnDivider(below: WideSpace, above: WideSpace, spaceWidth: Float): Boolean {
        // Check if spaces are positioned on top of one another.
        if (below.left > above.right || below.right < above.left)
            return false

        // Get the left and right boundary for the space common to the two.
        val left = if (below.left > above.left)
            below.left else above.left
        val right = if (below.right < above.right)
            below.right else above.right

        // Check if it is wide enough.
        val hole = right - left
        return hole > (spaceWidth * 3)
    }

    private fun getLocationAndSpaceWidthOfRightBoundary(textObj: TextObject): FloatArray {
        var rightmost = textObj.first().td[0]
        var spaceWidth = Float.MAX_VALUE
        var currX = textObj.first().td[0]
        textObj.forEachIndexed { i, e ->
            if (e != textObj.first())
                currX += e.td[0]
            if (i + 1 == textObj.count() || textObj.elementAt(i + 1).td[1] != 0f) {
                val elemWdth = getElementWidth(e, textObj.scaleX)
                if (elemWdth == Float.MAX_VALUE) {
                    spaceWidth = Float.MAX_VALUE
                } else {
                    val rightBound = currX + elemWdth
                    if (rightBound > rightmost) {
                        rightmost = rightBound
                        val fKey = sb.clear().append(e.tf, 2, e.tf.indexOf(' ')).toInt()
                        val fSize = sb.clear().append(e.tf, e.tf.indexOf(' ') + 1, e.tf.length).toDouble().toFloat()
                        val widths = fonts[fKey]?.widths
                        val spWdth = widths?.get(32)
                        val missingWidth = widths?.get(-1)
                        spaceWidth = spWdth ?: missingWidth ?: Float.MAX_VALUE
                        if (spaceWidth != Float.MAX_VALUE) {
                            spaceWidth = (spaceWidth / 1000) * fSize * textObj.scaleX
                        }
                    }
                }
            }
        }
        return floatArrayOf(rightmost, spaceWidth)
    }

    private fun getElementWidth(textElement: TextElement, scaleX: Float): Float {
        // Get font size
        sb.clear().append(textElement.tf, textElement.tf.indexOf(' ') + 1, textElement.tf.length)
        val fSize = sb.toDouble().toFloat()

        // Get glyph widths and missing character width
        sb.clear().append(textElement.tf, 2, textElement.tf.indexOf(' '))
        val fKey = sb.toInt()
        val widths = fonts[fKey]?.widths
        val missingWidth = widths?.get(-1)

        // Initialize total element width as 0. Will accumulate width for every character in string
        var elementWidth = 0f

        val string = textElement.tj as PDFString
        return if (widths != null && missingWidth != null) {
            string.value.forEach { c ->
                // Convert character to its integer value
                val cInt = c.toInt()

                // Get character's corresponding width and multiply to font size and horizontal scaling
                val width = if (widths.containsKey(cInt)) {
                    val cWidth = widths[cInt] as Float
                    (cWidth / 1000) * fSize * scaleX
                } else {
                    (missingWidth / 1000) * fSize * scaleX
                }

                // Accumulate width
                elementWidth += width
            }
            // return
            elementWidth
        } else {
            // Return a very large total width if can't obtain widths for characters. This will make sure wide space is
            // not formed.
            Float.MAX_VALUE
        }
    }

    private fun detectMultiLinearColumns() {
        var i = 0
        while (i < textObjects.count() && textObjects[i].column == -1) {
            if (isTextObjectMultiLinear(textObjects[i])) {
                val rowStart = findRowStart(i)
                val rowEnd = findRowEnd(i)
                if (i > rowStart || i < rowEnd) {
                    var column = 0
                    for (j in rowStart..rowEnd) {
                        textObjects[j].column = column++
                    }
                }
                i = rowEnd + 1
            } else {
                i++
            }
        }
    }

    private fun isTextObjectMultiLinear(textObj: TextObject): Boolean {
        var prevY = textObj.first().td[1]
        textObj.forEach {
            if (it.td[1] != prevY)
                return true
            prevY = it.td[1]
        }
        return false
    }

    private fun findRowStart(current: Int): Int {
        var i = current
        while (i >= 0) {
            if (i - 1 < 0 || textObjects[i].td[1] != textObjects[i - 1].td[1])
                return i
            i--
        }
        return 0
    }

    private fun findRowEnd(current: Int): Int {
        var i = current
        while (i < textObjects.count()) {
            if (i + 1 > textObjects.lastIndex || textObjects[i].td[1] != textObjects[i + 1].td[1])
                return i
            i++
        }
        return textObjects.lastIndex
    }

    internal fun groupTexts() {
        currTextGroup = TextGroup()
        contentGroups.add(currTextGroup)
        table = Table()
        currLine = ArrayList()

        textObjects.forEachIndexed { index, textObj ->
            var prevTextObj: TextObject? = null
            if (index > 0)
                prevTextObj = textObjects[index - 1]

            when {
                textObj.column >= 0 -> {
                    currTextGroup = TextGroup()
                    val cell = Table.Cell()
                    cell.add(currTextGroup)

                    // If first cell of table or if not in the same row, then add new row, else add cell to last row.
                    if (table.size() == 0 || textObj.td[1] != (prevTextObj as TextObject).td[1]) {
                        if (table.size() == 0) {
                            contentGroups.add(table)
                        }
                        val row = Table.Row()
                        row.add(cell)
                        table.add(row)
                    } else {
                        table[table.size() - 1].add(cell)
                    }

                    textObj.forEach {
                        var dty = -it.td[1]
                        if (textObj.first() == it) dty = 0f
                        sb.clear().append(it.tf, it.tf.indexOf(' ') + 1, it.tf.length)
                        val fSize = sb.toDouble().toFloat() * textObj.scaleY
                        sortGroup(it, dty, fSize)
                    }
                }
                table.size() > 0 -> {
                    table = Table() // Reset to empty table
                    currTextGroup = TextGroup()
                    contentGroups.add(currTextGroup)
                    textObj.forEach {
                        var dty = -it.td[1]
                        if (textObj.first() == it) dty = 0f
                        sb.clear().append(it.tf, it.tf.indexOf(' ') + 1, it.tf.length)
                        val fSize = sb.toDouble().toFloat() * textObj.scaleY
                        sortGroup(it, dty, fSize)
                    }
                }
                else -> {
                    textObj.forEach {
                        var dty = -it.td[1]
                        if (textObj.first() == it) {
                            dty = if (prevTextObj == null)
                                0f
                            else {
                                var yOfLast = prevTextObj.td[1]
                                prevTextObj.forEach { e ->
                                    if (prevTextObj.first() != e)
                                        yOfLast += e.td[1]
                                }
                                yOfLast - it.td[1]
                            }
                        }
                        sb.clear().append(it.tf, it.tf.indexOf(' ') + 1, it.tf.length)
                        val fSize = sb.toDouble().toFloat() * textObj.scaleY
                        sortGroup(it, dty, fSize)
                    }
                }
            }
        }
    }

    private fun sameLine(dty: Float): Boolean {
        return dty == 0f
    }

    private fun near(dty: Float, fSize: Float): Boolean {
        return dty < fSize * 2
    }

    private fun newLine(textElement: TextElement) {
        currLine = ArrayList()
        currLine.add(textElement)
        currTextGroup.add(currLine)
    }

    private fun newTextGroup(textElement: TextElement) {
        currTextGroup = TextGroup()
        newLine(textElement)

        if (table.size() > 0) {
            val lastRow = table[table.size() - 1]
            lastRow[lastRow.size() - 1].add(currTextGroup)
        } else {
            contentGroups.add(currTextGroup)
        }
    }

    private fun sortGroup(textElement: TextElement, dty: Float, fSize: Float) {
        when {
            currTextGroup.size() == 0 -> newLine(textElement)
            sameLine(dty) -> currLine.add(textElement)
            near(dty, fSize) -> newLine(textElement)
            else -> newTextGroup(textElement)
        }
    }

    internal fun checkForListTypeTextGroups() {
        fun checkIfAllLinesEndWithPeriods(textGroup: TextGroup) {
            textGroup.isAList = true
            for (i in 0 until textGroup.size()) {
                // For each line, check if the last element ends with a period.
                val line = textGroup[i]
                val s = (line[line.size - 1].tj as PDFString).value
                if (!s.endsWith('.'))
                    textGroup.isAList = false
            }
        }
        contentGroups.forEach {
            when (it) {
                is TextGroup -> checkIfAllLinesEndWithPeriods(it)
                is Table -> {
                    for (i in 0 until it.size()) {
                        for (j in 0 until it[i].size()) {
                            for (k in 0 until it[i][j].size()) {
                                val textGroup = it[i][j][k]
                                checkIfAllLinesEndWithPeriods(textGroup)
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun getLargestWidth(): Int {
        var maxWidth = 0
        contentGroups
            .asSequence()
            .filter { it is TextGroup }
            .forEach {
                val g = it as TextGroup
                for (i in 0 until g.size()) {
                    var charCount = 0
                    val line = g[i]
                    for (j in 0 until line.size) {
                        charCount += (line[j].tj as PDFString).value.length
                    }
                    if (charCount > maxWidth)
                        maxWidth = charCount
                }
            }
        return maxWidth
    }

    internal fun concatenateDividedByHyphen() {
        fun findHyphenAndConcatenate(textGroup: TextGroup) {
            if (textGroup.isAList)
                return

            var i = 0
            while (i + 1 < textGroup.size()) {
                val line = textGroup[i]
                val last = line.last().tj as PDFString
                if (last.value.endsWith(('-'))) {
                    sb.clear().append(last.value, 0, last.value.lastIndex)
                    sb.insert(0, '(')
                    sb.append(')')
                    val e = TextElement(
                        tf = line.last().tf,
                        tj = sb.toPDFString(),
                        td = line.last().td.copyOf(),
                        ts = line.last().ts
                    )
                    line.remove(line.last())
                    line.add(e)
                    val next = textGroup[i + 1]
                    line.addAll(next)
                    textGroup.remove(next)
                } else {
                    i++
                }
            }
        }
        contentGroups.forEach {
            when (it) {
                is TextGroup -> findHyphenAndConcatenate(it)
                is Table -> {
                    for (i in 0 until it.size()) {
                        val row = it[i]
                        for (j in 0 until row.size()) {
                            val cell = row[j]
                            for (k in 0 until cell.size()) {
                                val textGroup = cell[k]
                                findHyphenAndConcatenate(textGroup)
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun formParagraphs(width: Int) {
        contentGroups
            .asSequence()
            .filter { it is TextGroup && !it.isAList }
            .forEach {
                var i = 0
                val g = it as TextGroup
                var toCount = g[0]

                // Iterate until the second last of the list. The last line will be appended to it if necessary.
                while (i + 1 < g.size()) {
                    val line = g[i]

                    // Count the number of characters of the text in toCount variable.
                    var charCount = 0
                    toCount.forEach { e ->
                        charCount += (e.tj as PDFString).value.length
                    }

                    // If almost equal to estimated page width, append next line to current line and the number of lines
                    // in TextGroup is reduced by 1. Else, evaluate the next line.
                    if (charCount.toFloat() >= (0.8 * (width.toFloat()))) {
                        val next = g[i + 1]

                        // Add space in between when appending.
                        sb.clear().append('(').append(' ').append((next.first().tj as PDFString).value).append(')')
                        val e = TextElement(
                            tf = next.first().tf,
                            tj = sb.toPDFString(),
                            td = next.first().td.copyOf(),
                            ts = next.first().ts
                        )
                        next.remove(next.first())
                        next.add(0, e)

                        // Append next line to current line. The appended line will be removed from the TextGroup's list.
                        // The line following it in the list will be the next to append in case.
                        line.addAll(next)
                        g.remove(next)

                        // Do not increment i but the text that was just appended will be assigned to toCount variable
                        // which will be evaluated for the next iteration.
                        toCount = next
                    } else {
                        i++
                        toCount = g[i]
                    }
                }
            }
    }

    internal fun mergeElementsWithSameFont() {
        contentGroups.forEach {
            when (it) {
                is TextGroup -> mergeElementsInTextGroup(it)
                is Table -> {
                    for (i in 0 until it.size()) {
                        val row = it[i]
                        for (j in 0 until row.size()) {
                            val cell = row[j]
                            for (k in 0 until cell.size()) {
                                val textGroup = cell[k]
                                mergeElementsInTextGroup(textGroup)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun mergeElementsInTextGroup(textGroup: TextGroup) {
        for (i in 0 until textGroup.size()) {
            val line = textGroup[i]
            var first = 0
            var j = 1
            sb.clear()
            while (j < line.size) {
                if (line[j].tf == line[j - 1].tf) {
                    if (sb.isEmpty()) {
                        first = j - 1
                        sb.append(
                            (line[j - 1].tj as PDFString).value
                        )
                        sb.append(
                            (line[j].tj as PDFString).value
                        )
                    } else {
                        sb.append(
                            (line[j].tj as PDFString).value
                        )
                    }
                } else {
                    if (sb.isNotEmpty()) {
                        mergeTextElements(line, first, j)
                        sb.clear()
                    }
                    first = j
                }
                j++
            }
            if (sb.isNotEmpty()) {
                mergeTextElements(line, first, line.size)
                sb.clear()
            }
        }
    }

    private fun mergeTextElements(line: ArrayList<TextElement>, start: Int, end: Int) {
        for (k in (end - 1) downTo (start + 1)) {
            line.removeAt(k)
        }
        sb.insert(0, '(')
        sb.append(')')
        val newTextElement = TextElement(
            tf = line[start].tf,
            td = line[start].td.copyOf(),
            ts = line[start].ts,
            tj = sb.toPDFString()
        )
        line.removeAt(start)
        line.add(start, newTextElement)
    }

    private fun deleteBlankLines() {
        var i = 0
        while (i < contentGroups.size) {
            val textGroup = contentGroups[i]
            if (textGroup is TextGroup) {
                for (j in 0 until textGroup.size()) {
                    val line = textGroup[j]
                    sb.clear()
                    line.forEach { e ->
                        sb.append((e.tj as PDFString).value)
                    }
                    if (sb.isBlank())
                        textGroup.remove(line)
                }

                if (textGroup.size() == 0)
                    contentGroups.remove(textGroup)
            }
            // Ignore table, since a blank line may mean an empty cell.
            i++
        }
    }
}

data class WideSpace(
    val left: Float,
    val right: Float,
    val leftTextObj: Int,
    val rightTextObj: Int,
    var isDivider: Boolean = false
)