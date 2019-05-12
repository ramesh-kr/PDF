package marabillas.loremar.pdfparser.contents.text

import android.support.v4.util.SparseArrayCompat
import android.text.SpannableString
import android.text.SpannableStringBuilder
import marabillas.loremar.pdfparser.contents.ContentGroup
import marabillas.loremar.pdfparser.contents.PageContent
import marabillas.loremar.pdfparser.font.CustomTypefaceSpan
import marabillas.loremar.pdfparser.font.Font
import marabillas.loremar.pdfparser.font.FontMappings
import marabillas.loremar.pdfparser.font.FontName
import marabillas.loremar.pdfparser.objects.PDFString
import marabillas.loremar.pdfparser.toInt

internal class TextContentAdapter {
    private var spanBuilder = SpannableStringBuilder()
    private val pageContents = ArrayList<PageContent>()
    private val sb = StringBuilder()

    private fun resetAdapter() {
        spanBuilder = SpannableStringBuilder()
        pageContents.clear()
        sb.clear()
    }

    fun getContents(contentGroups: ArrayList<ContentGroup>, fonts: SparseArrayCompat<Font>): ArrayList<PageContent> {
        resetAdapter()
        var contentGroup: ContentGroup
        for (i in 0 until contentGroups.size) {
            contentGroup = contentGroups[i]
            if (contentGroup is TextGroup) {
                if (i > 0 && contentGroups[i - 1] is TextGroup) {
                    spanBuilder.append("\n\n")
                } else if (i > 0 && contentGroups[i - 1] is Table) {
                    spanBuilder = SpannableStringBuilder()
                }
                processTextGroup(contentGroup, fonts)
            } else if (contentGroup is Table) {
                if (spanBuilder.isNotEmpty()) {
                    val textContent = TextContent(spanBuilder)
                    pageContents.add(textContent)
                }
                val table = processTable(contentGroup, fonts)
                pageContents.add(table)
            }
        }

        if (spanBuilder.isNotEmpty()) {
            val textContent = TextContent(spanBuilder)
            pageContents.add(textContent)
        }

        return pageContents
    }

    private fun processTextGroup(textGroup: TextGroup, fonts: SparseArrayCompat<Font>) {
        var line: ArrayList<TextElement>
        for (i in 0 until textGroup.size()) {
            line = textGroup[i]
            if (line != textGroup[0])
                spanBuilder.append('\n')
            for (j in 0 until line.size) {
                val s = SpannableString((line[j].tj as PDFString).value)
                sb.clear().append(line[j].tf)
                val fEnd = sb.indexOf(' ')
                sb.delete(fEnd, sb.length)
                sb.delete(0, 2)
                val t = fonts[sb.toInt()]?.typeface ?: FontMappings[FontName.DEFAULT]

                val span = CustomTypefaceSpan(t)
                s.setSpan(span, 0, s.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)

                spanBuilder.append(s)
            }
        }
    }

    private fun processTable(table: Table, fonts: SparseArrayCompat<Font>): TableContent {
        val content = TableContent()
        for (i in 0 until table.size()) {
            val rowContent = TableContent.Row()
            for (j in 0 until table[i].size()) {
                spanBuilder = SpannableStringBuilder()
                for (k in 0 until table[i][j].size()) {
                    processTextGroup(table[i][j][k], fonts)
                }
                val cellContent = TableContent.Cell(spanBuilder)
                rowContent.cells.add(cellContent)
            }
            content.rows.add(rowContent)
        }
        return content
    }
}