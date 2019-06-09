package marabillas.loremar.pdfparser.font.encoding

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.utils.exts.copyOf
import marabillas.loremar.pdfparser.utils.exts.octalToDecimalKeys
import marabillas.loremar.pdfparser.utils.exts.set

internal class SymbolEncoding {
    companion object : EncodingSource {
        private val encoding = SparseArrayCompat<String>()

        init {
            encoding[101] = "Alpha"
            encoding[102] = "Bravo"
            encoding[103] = "Chi"
            encoding[104] = "Delta"
            encoding[105] = "Epsilon"
            encoding[110] = "Eta"
            encoding[240] = "Euro"
            encoding[107] = "Gamma"
            encoding[301] = "Ifraktur"
            encoding[111] = "Iota"
            encoding[113] = "Kappa"
            encoding[114] = "Lambda"
            encoding[115] = "Mu"
            encoding[116] = "Nu"
            encoding[127] = "Omega"
            encoding[117] = "Omicron"
            encoding[106] = "Phi"
            encoding[120] = "Pi"
            encoding[131] = "Psi"
            encoding[302] = "Rfraktur"
            encoding[122] = "Rho"
            encoding[123] = "Sigma"
            encoding[124] = "Tau"
            encoding[121] = "Theta"
            encoding[125] = "Upsilon"
            encoding[241] = "Upsilon1"
            encoding[130] = "Xi"
            encoding[132] = "Zeta"
            encoding[300] = "aleph"
            encoding[141] = "alpha"
            encoding[46] = "ampersand"
            encoding[320] = "angle"
            encoding[341] = "angleleft"
            encoding[361] = "angleright"
            encoding[273] = "approxequal"
            encoding[253] = "arrowboth"
            encoding[333] = "arrowdblboth"
            encoding[337] = "arrowdbldown"
            encoding[334] = "arrowdblleft"
            encoding[336] = "arrowdblright"
            encoding[335] = "arrowdblup"
            encoding[257] = "arrowdown"
            encoding[276] = "arrowhorizex"
            encoding[254] = "arrowleft"
            encoding[256] = "arrowright"
            encoding[255] = "arrowup"
            encoding[275] = "arrowvertex"
            encoding[52] = "asteriskmath"
            encoding[174] = "bar"
            encoding[142] = "beta"
            encoding[173] = "braceleft"
            encoding[175] = "braceright"
            encoding[354] = "bracelefttp"
            encoding[355] = "braceleftmid"
            encoding[356] = "braceleftbt"
            encoding[374] = "bracerighttp"
            encoding[375] = "bracerightmid"
            encoding[376] = "bracerightbt"
            encoding[357] = "braceex"
            encoding[133] = "bracketleft"
            encoding[135] = "bracketright"
            encoding[351] = "bracketlefttp"
            encoding[352] = "bracketleftex"
            encoding[353] = "bracketleftbt"
            encoding[371] = "bracketrighttp"
            encoding[372] = "bracketrightex"
            encoding[373] = "bracketrightbt"
            encoding[267] = "bullet"
            encoding[277] = "carriagereturn"
            encoding[143] = "chi"
            encoding[304] = "circlemultiply"
            encoding[305] = "circleplus"
            encoding[247] = "club"
            encoding[72] = "colon"
            encoding[54] = "comma"
            encoding[100] = "congruent"
            encoding[343] = "copyrightsans"
            encoding[323] = "copyrightserif"
            encoding[260] = "degree"
            encoding[144] = "delta"
            encoding[250] = "diamond"
            encoding[270] = "divide"
            encoding[327] = "dotmath"
            encoding[70] = "eight"
            encoding[316] = "element"
            encoding[274] = "ellipsis"
            encoding[306] = "emptyset"
            encoding[145] = "epsilon"
            encoding[75] = "equal"
            encoding[272] = "equivalence"
            encoding[150] = "eta"
            encoding[41] = "exclam"
            encoding[44] = "existential"
            encoding[65] = "five"
            encoding[246] = "florin"
            encoding[64] = "four"
            encoding[244] = "fraction"
            encoding[147] = "gamma"
            encoding[321] = "gradient"
            encoding[76] = "greater"
            encoding[263] = "greaterequal"
            encoding[251] = "heart"
            encoding[245] = "infinity"
            encoding[362] = "integral"
            encoding[363] = "integraltp"
            encoding[364] = "integralex"
            encoding[365] = "integralbt"
            encoding[307] = "intersection"
            encoding[151] = "iota"
            encoding[153] = "kappa"
            encoding[154] = "lambda"
            encoding[74] = "less"
            encoding[243] = "lessequal"
            encoding[331] = "logicaland"
            encoding[330] = "logicalnot"
            encoding[332] = "logicalor"
            encoding[340] = "lozenge"
            encoding[55] = "minus"
            encoding[242] = "minute"
            encoding[155] = "mu"
            encoding[264] = "multiply"
            encoding[71] = "nine"
            encoding[317] = "notelement"
            encoding[271] = "notequal"
            encoding[313] = "notsubset"
            encoding[156] = "nu"
            encoding[43] = "numbersign"
            encoding[167] = "omega"
            encoding[166] = "omega1"
            encoding[157] = "omicron"
            encoding[61] = "one"
            encoding[50] = "parenleft"
            encoding[51] = "parenright"
            encoding[346] = "parenlefttp"
            encoding[347] = "parenleftex"
            encoding[350] = "parentleftbt"
            encoding[366] = "parenrighttp"
            encoding[367] = "parenrightex"
            encoding[370] = "parenrightbt"
            encoding[266] = "partialdiff"
            encoding[45] = "percent"
            encoding[56] = "period"
            encoding[136] = "perpendicular"
            encoding[146] = "phi"
            encoding[152] = "phil"
            encoding[160] = "pi"
            encoding[53] = "plus"
            encoding[261] = "plusminus"
            encoding[325] = "product"
            encoding[314] = "propersubset"
            encoding[311] = "propersuperset"
            encoding[265] = "proportional"
            encoding[171] = "psi"
            encoding[77] = "question"
            encoding[326] = "radical"
            encoding[140] = "radicalex"
            encoding[315] = "reflexsubset"
            encoding[312] = "reflexsuperset"
            encoding[342] = "registersans"
            encoding[322] = "registerserif"
            encoding[162] = "rho"
            encoding[262] = "second"
            encoding[73] = "semicolon"
            encoding[67] = "seven"
            encoding[163] = "sigma"
            encoding[126] = "sigma1"
            encoding[176] = "similar"
            encoding[66] = "six"
            encoding[57] = "slash"
            encoding[40] = "space"
            encoding[252] = "spade"
            encoding[47] = "suchthat"
            encoding[345] = "summation"
            encoding[164] = "tau"
            encoding[134] = "therefore"
            encoding[161] = "theta"
            encoding[112] = "theta1"
            encoding[63] = "three"
            encoding[344] = "trademarksans"
            encoding[324] = "trademarkserif"
            encoding[62] = "two"
            encoding[137] = "underscore"
            encoding[310] = "union"
            encoding[42] = "universal"
            encoding[165] = "upsilon"
            encoding[303] = "weierstrass"
            encoding[170] = "xi"
            encoding[60] = "zero"
            encoding[172] = "zeta"

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