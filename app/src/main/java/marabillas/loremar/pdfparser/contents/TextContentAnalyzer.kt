package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.Numeric
import marabillas.loremar.pdfparser.objects.PDFArray
import marabillas.loremar.pdfparser.objects.PDFString
import marabillas.loremar.pdfparser.objects.toPDFString
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

internal class TextContentAnalyzer(private val textObjects: ArrayList<TextObject>) {
    internal val contentGroups = ArrayList<ContentGroup>()

    fun analyze() {
        // Sort according to descending Ty and then to ascending Tx
        textObjects.sortWith(
            compareBy(
                { -it.td[1] },
                { it.td[0] })
        )

        // If tj values are arrays resulting from TJ operator, determine from the number values between strings
        // whether to add space or not while concatenating strings. First to get glyph width for space, get all the
        // negative numbers and identify the negative number with most occurrences. Rule: If the absolute value of a
        // negative number is less than 15% of the space width, don't add space. If it is greater than 115%,
        // then add double space. Otherwise, add space. If number is positive don't add space.
        handleTJArrays()

        // Check for multi-column texts. Get all text objects with equal Tx origin and flag each as "columned".
        // If there is more than one multi-columned adjacent lines, then these text objects form a table.
        // Text in adjacent columns of the same row are concatenated after each other and are separated by " || ".
        // The next row begins on the next paragraph. If "columned" text objects don't form a table, then they are
        // arranged according to their Tx origins. All "columned" text objects with lesser Tx are displayed first before those with greater Tx.
        handleMultiColumnTexts()

        // Group texts in the same line or in adjacent lines with line-spacing less than font size.
        groupTexts()

        // Check if lines end with a period. If yes, then lines stay as they were. If not, then proceed analysis.
        checkForListTypeTextGroups()

        // Estimate the width of the page by getting the largest width of a line of texts
        val w = getLargestWidth()

        // If a line ends with '-', then append the next line to this line and remove the '-' character.
        concatenateDividedByHyphen()

        // If line is almost as long as the width of page, then append the next line in the TextGroup.
        formParagraphs(w)
    }

    internal fun handleTJArrays() {
        textObjects.forEach { texObj ->
                val spW = getSpaceWidth(texObj)
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
                    val sb = StringBuilder("(")
                    (textElement.tj).forEach {
                            if (it is PDFString)
                                sb.append(it.value) // If string, append
                            else if (it is Numeric) {
                                val num = -it.value.toFloat()
                                if (num >= 1.15 * width)
                                    sb.append("  ") // If more than 115% of space width, add double space
                                else if (num >= 0.15 * width)
                                    sb.append(" ") // If between 15% or 115% of space width, add space
                            }
                        }
                    sb.append(")")
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

    internal fun handleMultiColumnTexts() {
        checkForColumnsAndRows()
        //sortColumnedTexts()
    }

    private fun checkForColumnsAndRows() {
        val txes = HashMap<Float, ArrayList<TextObject>>()
        val tys = HashMap<Float, ArrayList<TextObject>>()
        val allTys = HashSet<Float>()
        textObjects.forEach { textObj ->
            // Group together TextObjects with equal Tx values.
            val tx = textObj.td[0]
            val arr = txes[tx] ?: ArrayList()
            arr.add(textObj)
            txes[tx] = arr
            allTys.add(textObj.td[1])
        }
        txes.forEach {
            // If more than one TextObjects have the same Tx value, then flag each as columned.
            if (it.value.size > 1)
                it.value.forEach { textObj ->
                    textObj.columned = true

                    // Group together columned TextObjects with equal Ty values.
                    val ty = textObj.td[1]
                    val arr = tys[ty] ?: ArrayList()
                    arr.add(textObj)
                    tys[ty] = arr
                }
        }
        val allTysSorted = allTys.toSortedSet(compareByDescending { it }).toList()
        var prevIsMultiColumned = false
        var prevTyIndex = -1
        tys.toSortedMap(compareByDescending { it })
            .forEach {
                // If more than one columned TextObjects have the same Ty value, then flag each as rowed. This helps in
                // creating tables.
                if (it.value.size > 1) {
                    val currTyIndex = allTysSorted.indexOf(it.key)
                    // If previous line is multi-columned and adjacent to this current line, All TextObjects of this current
                    // line and previous line are flagged as rowed.
                    if (currTyIndex - 1 == prevTyIndex && prevIsMultiColumned) {
                        it.value.forEach { textObj ->
                            textObj.rowed = true
                        }
                        val prevTy = allTysSorted[prevTyIndex]
                        tys[prevTy]?.forEach { textObj ->
                            textObj.rowed = true
                        }
                    }
                    prevTyIndex = currTyIndex
                    prevIsMultiColumned = true
                } else {
                    prevIsMultiColumned = false
                }
            }
    }

    /*private fun sortColumnedTexts() {
        val columned = textObjects.filter {
                it.columned && !it.rowed
            }
        val firstColumnedY = columned.first().td[1]
        val lastColumnedY = columned.last().td[1]
        val multiColumned = textObjects.filter {
            it.td[1] in lastColumnedY..firstColumnedY
        }
        sortByColumn(columned as ArrayList<TextObject>)
    }

    private fun sortByColumn(columnedTexts: ArrayList<TextObject>) {
        val skipped = ArrayList<TextObject>() // TextObjects that don't belong to the current column
        var skipping = false // True if skipping the rest of the line
        var prev: TextObject? = null // The first TextObject in the previous line
        columnedTexts.forEach {
            if (prev == null)
                prev = it
            else {
                if (it.td[1] < (prev as TextObject).td[1]) {
                    prev = it
                    // Move to the end of the current column and before any skipped TextObjects
                    val fromIndex = textObjects.indexOf(it)
                    val toIndex = textObjects.indexOf(skipped.first())
                    textObjects.removeAt(fromIndex)
                    textObjects.add(toIndex, it)

                    // Stop skipping. Next TextObjects belong to the current column until a columned TextObject is
                    // encountered in the iteration
                    skipping = false
                } else {
                    if (!skipping) {
                        if (it.columned) {
                            // This and the rest of the line doesn't belong to the current column
                            skipping = true
                            skipped.add(it)
                        } else {
                            // Current textObject belongs to the current column
                            // Move to the end of the current column and before any skipped TextObjects
                            val fromIndex = textObjects.indexOf(it)
                            val toIndex = textObjects.indexOf(skipped.first())
                            textObjects.removeAt(fromIndex)
                            textObjects.add(toIndex, it)
                        }
                    } else {
                        skipped.add(it)
                    }
                }
            }
        }

        // Sort remaining TextObjects
        if (skipped.size > 0) sortByColumn(skipped)
    }*/

    internal fun groupTexts() {
        var currTextGroup = TextGroup()
        contentGroups.add(currTextGroup)
        var table = Table()
        var currLine = ArrayList<TextElement>()

        fun sameLine(dty: Float): Boolean {
            return dty == 0f
        }

        fun near(dty: Float, fSize: Float): Boolean {
            return dty < fSize * 2
        }

        fun newLine(textElement: TextElement) {
            currLine = ArrayList()
            currLine.add(textElement)
            currTextGroup.add(currLine)
        }

        fun newTextGroup(textElement: TextElement) {
            currTextGroup = TextGroup()
            newLine(textElement)

            if (table.count() > 0) {
                table.last().last().add(currTextGroup)
            } else {
                contentGroups.add(currTextGroup)
            }
        }

        fun sortGroup(textElement: TextElement, dty: Float, fSize: Float) {
            when {
                currTextGroup.count() == 0 -> newLine(textElement)
                sameLine(dty) -> currLine.add(textElement)
                near(dty, fSize) -> newLine(textElement)
                else -> newTextGroup(textElement)
            }
        }

        textObjects.forEachIndexed { index, textObj ->
            var prevTextObj: TextObject? = null
            if (index > 0)
                prevTextObj = textObjects[index - 1]

            when {
                textObj.rowed -> {
                    currTextGroup = TextGroup()
                    val cell = Table.Cell()
                    cell.add(currTextGroup)

                    // If first cell of table or if not in the same row, then add new row, else add cell to last row.
                    if (table.count() == 0 || textObj.td[1] != (prevTextObj as TextObject).td[1]) {
                        if (table.count() == 0) {
                            contentGroups.add(table)
                        }

                        val row = Table.Row()
                        row.add(cell)
                        table.add(row)

                    } else {
                        table.last().add(cell)
                    }

                    textObj.forEach {
                        var dty = -it.td[1]
                        if (textObj.first() == it) dty = 0f
                        val fSize = it.tf.substringAfter(" ").toFloat()
                        sortGroup(it, dty, fSize)
                    }
                }
                table.count() > 0 -> {
                    table = Table() // Reset to empty table
                    currTextGroup = TextGroup()
                    contentGroups.add(currTextGroup)
                    textObj.forEach {
                        var dty = -it.td[1]
                        if (textObj.first() == it) dty = 0f
                        val fSize = it.tf.substringAfter(" ").toFloat()
                        sortGroup(it, dty, fSize)
                    }
                }
                /*textObj.columned -> {
                    // TODO Handle column
                }*/
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
                        val fSize = it.tf.substringAfter(" ").toFloat()
                        sortGroup(it, dty, fSize)
                    }
                }
            }
        }
    }

    internal fun checkForListTypeTextGroups() {
        fun checkIfAllLinesEndWithPeriods(textGroup: TextGroup) {
            textGroup.isAList = true
            textGroup.forEach {
                // For each line, check if the last element ends with a period.
                val s = (it.last().tj as PDFString).value
                if (!s.endsWith("."))
                    textGroup.isAList = false
            }
        }
        contentGroups.forEach {
            when (it) {
                is TextGroup -> checkIfAllLinesEndWithPeriods(it)
                is Table -> {
                    it.forEach { row ->
                        row.forEach { cell ->
                            cell.forEach { textGroup ->
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
                g.forEach { line ->
                    var charCount = 0
                    line.forEach { e ->
                        charCount += (e.tj as PDFString).value.length
                    }
                    if (charCount > maxWidth)
                        maxWidth = charCount
                }
            }
        return maxWidth
    }

    internal fun concatenateDividedByHyphen() {
        fun findHyphenAndConcatenate(textGroup: TextGroup) {
            var i = 0
            while (i + 1 < textGroup.count()) {
                val line = textGroup[i]
                val last = line.last().tj as PDFString
                if (last.value.endsWith(("-"))) {
                    val s = "(${last.value.substringBeforeLast("-")})"
                    val e = TextElement(
                        tf = line.last().tf,
                        tj = s.toPDFString(),
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
                    it.forEach { row ->
                        row.forEach { cell ->
                            cell.forEach { texGroup ->
                                findHyphenAndConcatenate(texGroup)
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
            .filter { it is TextGroup }
            .forEach {
                var i = 0
                val g = it as TextGroup
                var toCount = g[0]

                // Iterate until the second last of the list. The last line will be appended to it if necessary.
                while (i + 1 < g.count()) {
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
                        val s = "( ${next.first().tj as PDFString})"
                        val e = TextElement(
                            tf = next.first().tf,
                            tj = s.toPDFString(),
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
}