package marabillas.loremar.pdfparser.font.encoding

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.utils.exts.copyOf
import marabillas.loremar.pdfparser.utils.exts.octalToDecimalKeys
import marabillas.loremar.pdfparser.utils.exts.set

internal class MacRomanEncoding {
    companion object : EncodingSource {
        private val encoding = SparseArrayCompat<String>()

        init {
            encoding[101] = "A"
            encoding[256] = "AE"
            encoding[347] = "Aacute"
            encoding[345] = "Acircumflex"
            encoding[200] = "Adieresis"
            encoding[313] = "Agrave"
            encoding[201] = "Aring"
            encoding[314] = "Atilde"
            encoding[102] = "B"
            encoding[103] = "C"
            encoding[202] = "Ccedilla"
            encoding[104] = "D"
            encoding[105] = "E"
            encoding[203] = "Eacute"
            encoding[346] = "Ecircumflex"
            encoding[350] = "Edieresis"
            encoding[351] = "Egrave"
            encoding[106] = "F"
            encoding[107] = "G"
            encoding[110] = "H"
            encoding[111] = "I"
            encoding[352] = "Iacute"
            encoding[353] = "Icircumflex"
            encoding[354] = "Idieresis"
            encoding[355] = "Igrave"
            encoding[112] = "J"
            encoding[113] = "K"
            encoding[114] = "L"
            encoding[115] = "M"
            encoding[116] = "N"
            encoding[204] = "Ntilde"
            encoding[117] = "O"
            encoding[316] = "OE"
            encoding[356] = "Oacute"
            encoding[357] = "Ocircumflex"
            encoding[205] = "Odieresis"
            encoding[361] = "Ograve"
            encoding[257] = "Oslash"
            encoding[315] = "Otilde"
            encoding[120] = "P"
            encoding[121] = "Q"
            encoding[122] = "R"
            encoding[123] = "S"
            encoding[124] = "T"
            encoding[125] = "U"
            encoding[362] = "Uacute"
            encoding[363] = "Ucircumflex"
            encoding[206] = "Udieresis"
            encoding[364] = "Ugrave"
            encoding[126] = "V"
            encoding[127] = "W"
            encoding[130] = "X"
            encoding[131] = "Y"
            encoding[331] = "Ydieresis"
            encoding[132] = "Z"
            encoding[141] = "a"
            encoding[207] = "aacute"
            encoding[211] = "acircumflex"
            encoding[253] = "acute"
            encoding[212] = "adieresis"
            encoding[276] = "ae"
            encoding[210] = "agrave"
            encoding[46] = "ampersand"
            encoding[214] = "aring"
            encoding[136] = "asciicircum"
            encoding[176] = "asciitilde"
            encoding[52] = "asterisk"
            encoding[100] = "at"
            encoding[213] = "atilde"
            encoding[142] = "b"
            encoding[134] = "backslash"
            encoding[174] = "bar"
            encoding[173] = "braceleft"
            encoding[175] = "braceright"
            encoding[133] = "bracketleft"
            encoding[135] = "bracketright"
            encoding[371] = "breve"
            encoding[245] = "bullet"
            encoding[143] = "c"
            encoding[377] = "caron"
            encoding[215] = "ccedilla"
            encoding[374] = "cedilla"
            encoding[242] = "cent"
            encoding[366] = "circumflex"
            encoding[72] = "colon"
            encoding[54] = "comma"
            encoding[251] = "copyright"
            encoding[333] = "currency"
            encoding[144] = "d"
            encoding[240] = "dagger"
            encoding[340] = "daggerdbl"
            encoding[241] = "degree"
            encoding[254] = "dieresis"
            encoding[326] = "divide"
            encoding[44] = "dollar"
            encoding[372] = "dotaccent"
            encoding[365] = "dotlessi"
            encoding[145] = "e"
            encoding[216] = "eacute"
            encoding[220] = "ecircumflex"
            encoding[221] = "edieresis"
            encoding[217] = "egrave"
            encoding[70] = "eight"
            encoding[311] = "ellipsis"
            encoding[321] = "emdash"
            encoding[320] = "endash"
            encoding[75] = "equal"
            encoding[41] = "exclam"
            encoding[301] = "exclamdown"
            encoding[146] = "f"
            encoding[336] = "fi"
            encoding[65] = "five"
            encoding[337] = "fl"
            encoding[304] = "florin"
            encoding[64] = "flour"
            encoding[332] = "fraction"
            encoding[147] = "g"
            encoding[247] = "germandbls"
            encoding[140] = "grave"
            encoding[76] = "greater"
            encoding[307] = "guillemotleft"
            encoding[310] = "guillemotright"
            encoding[334] = "guilsinglleft"
            encoding[335] = "guilsinglright"
            encoding[150] = "h"
            encoding[375] = "hungarumlaut"
            encoding[55] = "hyphen"
            encoding[151] = "i"
            encoding[222] = "iacute"
            encoding[224] = "icircumflex"
            encoding[225] = "idieresis"
            encoding[223] = "igrave"
            encoding[152] = "j"
            encoding[153] = "k"
            encoding[154] = "l"
            encoding[74] = "less"
            encoding[302] = "logicalnot"
            encoding[155] = "m"
            encoding[370] = "macron"
            encoding[265] = "mu"
            encoding[156] = "n"
            encoding[71] = "nine"
            encoding[226] = "ntilde"
            encoding[43] = "numbersign"
            encoding[157] = "o"
            encoding[227] = "oacute"
            encoding[231] = "ocircumflex"
            encoding[232] = "odieresis"
            encoding[317] = "oe"
            encoding[376] = "ogonek"
            encoding[230] = "ograve"
            encoding[61] = "one"
            encoding[273] = "ordfeminine"
            encoding[274] = "ordmasculine"
            encoding[277] = "oslash"
            encoding[233] = "otilde"
            encoding[160] = "p"
            encoding[246] = "paragraph"
            encoding[50] = "parenleft"
            encoding[51] = "parenright"
            encoding[45] = "percent"
            encoding[56] = "period"
            encoding[341] = "periodcentered"
            encoding[344] = "perthousand"
            encoding[53] = "plus"
            encoding[261] = "plusminus"
            encoding[161] = "q"
            encoding[77] = "question"
            encoding[300] = "questiondown"
            encoding[42] = "quotedbl"
            encoding[343] = "quotedblbase"
            encoding[322] = "quotedblleft"
            encoding[323] = "quotedblright"
            encoding[324] = "quoteleft"
            encoding[325] = "quoteright"
            encoding[342] = "quotesinglbase"
            encoding[47] = "quotesingle"
            encoding[162] = "r"
            encoding[250] = "registered"
            encoding[373] = "ring"
            encoding[163] = "s"
            encoding[244] = "section"
            encoding[73] = "semicolon"
            encoding[67] = "seven"
            encoding[66] = "six"
            encoding[57] = "slash"
            encoding[40] = "space"
            encoding[243] = "sterling"
            encoding[164] = "t"
            encoding[63] = "three"
            encoding[367] = "tilde"
            encoding[252] = "trademark"
            encoding[62] = "two"
            encoding[165] = "u"
            encoding[234] = "uacute"
            encoding[236] = "ucircumflex"
            encoding[237] = "udieresis"
            encoding[235] = "ugrave"
            encoding[137] = "underscore"
            encoding[166] = "v"
            encoding[167] = "w"
            encoding[170] = "x"
            encoding[171] = "y"
            encoding[330] = "ydieresis"
            encoding[264] = "yen"
            encoding[172] = "z"
            encoding[60] = "zero"

            encoding.octalToDecimalKeys()
        }

        fun copyOf(): SparseArrayCompat<String> {
            return encoding.copyOf()
        }

        override fun putAllTo(target: SparseArrayCompat<String>) {
            target.putAll(encoding)
        }
    }
}