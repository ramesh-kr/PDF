package marabillas.loremar.pdfparser.contents.text

import marabillas.loremar.pdfparser.contents.PageContent
import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.toPDFString

internal class TextElement internal constructor(
    val tj: PDFObject = "()".toPDFString(),
    val tf: String = "",
    val td: FloatArray = FloatArray(2),
    val ts: Float = 0f,
    val rgb: FloatArray = floatArrayOf(-1f, -1f, -1f)
) : PageContent