package marabillas.loremar.pdfparser

import marabillas.loremar.pdfparser.exceptions.NoDocumentException
import marabillas.loremar.pdfparser.exceptions.UnsupportedPDFElementException
import marabillas.loremar.pdfparser.objects.*
import java.io.RandomAccessFile

class PDFParser {
    private var fileReader: PDFFileReader? = null
    private var objects: HashMap<String, XRefEntry>? = null

    fun loadDocument(file: RandomAccessFile): PDFParser {
        val fileReader = PDFFileReader(file)
        this.fileReader = fileReader
        ObjectIdentifier.referenceResolver = ReferenceResolverImpl()

        objects = fileReader.getLastXRefData()

        // Process trailer
        val trailerEntries = fileReader.getTrailerEntries(true)
        size = (trailerEntries["Size"] as Numeric).value.toInt()
        documentCatalog = trailerEntries["Root"] as Dictionary
        info = trailerEntries["Info"] as Dictionary
        if (trailerEntries["Encrypt"] != null) throw UnsupportedPDFElementException(
            "PDFParser library does not support encrypted pdf files yet."
        )

        return this
    }

    internal var size: Int? = null
        get() = field ?: throw NoDocumentException()

    internal var documentCatalog: Dictionary? = null
        get() = field ?: throw NoDocumentException()

    internal var info: Dictionary? = null

    private inner class ReferenceResolverImpl : ReferenceResolver {
        override fun resolveReference(reference: Reference): PDFObject? {
            val fileReader = fileReader ?: throw NoDocumentException()
            val objects = objects ?: throw NoDocumentException()
            val obj = objects["${reference.obj} ${reference.gen}"] ?: return null

            if (obj.compressed) {
                val objStmEntry = objects["${obj.objStm} 0"]
                val objStm = objStmEntry?.pos?.let { fileReader.getObjectStream(it) }
                return objStm?.getObject(obj.index)
            } else {
                val content = fileReader.getIndirectObject(obj.pos).extractContent()
                if (content == "" || content == "null") return null
                return content.toPDFObject(false) ?: reference
            }
        }

    }
}