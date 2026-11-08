package io.github.dwzg.rustle.ui.code

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

enum class TokenKind { Keyword, Type, Function, Macro, String, Number, Comment, Lifetime, Attribute, Plain }

data class Token(val kind: TokenKind, val start: Int, val end: Int)

/** The code panels are always dark, whatever the app theme. */
object CodeColors {
    val Background = Color(0xFF151821)
    val Gutter = Color(0xFF4B5263)
    val Plain = Color(0xFFD7DAE0)
    val Keyword = Color(0xFFFF8A4C)
    val Type = Color(0xFF6CD4FF)
    val Function = Color(0xFFFFD866)
    val Macro = Color(0xFFC792EA)
    val Str = Color(0xFFA5E075)
    val Number = Color(0xFFF78C6C)
    val Comment = Color(0xFF6A7387)
    val Lifetime = Color(0xFF4FD6BE)
    val Attribute = Color(0xFF89DDFF)
    val Blank = Color(0xFF2B3040)
    val Selected = Color(0x33FF8A4C)
}

/** A small, forgiving Rust tokenizer, good enough for teaching snippets. */
object RustHighlighter {
    private val keywords = setOf(
        "as", "async", "await", "break", "const", "continue", "crate", "dyn", "else", "enum",
        "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod", "move",
        "mut", "pub", "ref", "return", "self", "Self", "static", "struct", "super", "trait", "true",
        "type", "unsafe", "use", "where", "while",
    )
    private val primitives = setOf(
        "i8", "i16", "i32", "i64", "i128", "isize", "u8", "u16", "u32", "u64", "u128", "usize",
        "f32", "f64", "bool", "char", "str",
    )

    fun tokenize(code: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        val n = code.length
        var lastWord = ""
        fun add(kind: TokenKind, start: Int, end: Int) {
            if (end > start) out += Token(kind, start, end)
        }
        while (i < n) {
            val c = code[i]
            when {
                c == '/' && i + 1 < n && code[i + 1] == '/' -> {
                    val end = code.indexOf('\n', i).let { if (it < 0) n else it }
                    add(TokenKind.Comment, i, end); i = end
                }
                c == '/' && i + 1 < n && code[i + 1] == '*' -> {
                    val end = code.indexOf("*/", i + 2).let { if (it < 0) n else it + 2 }
                    add(TokenKind.Comment, i, end); i = end
                }
                c == '"' -> {
                    var j = i + 1
                    while (j < n && code[j] != '"') j += if (code[j] == '\\') 2 else 1
                    val end = (j + 1).coerceAtMost(n)
                    add(TokenKind.String, i, end); i = end
                }
                c == '\'' -> {
                    // char literal ('a', '\n') or lifetime ('a, 'static)
                    val charEnd = when {
                        i + 2 < n && code[i + 1] == '\\' -> code.indexOf('\'', i + 2).takeIf { it in (i + 2)..(i + 10) }
                        i + 2 < n && code[i + 2] == '\'' -> i + 2
                        else -> null
                    }
                    if (charEnd != null) {
                        add(TokenKind.String, i, charEnd + 1); i = charEnd + 1
                    } else {
                        var j = i + 1
                        while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                        add(TokenKind.Lifetime, i, j); i = maxOf(j, i + 1)
                    }
                }
                c == '#' && i + 1 < n && (code[i + 1] == '[' || code[i + 1] == '!') -> {
                    val end = code.indexOf(']', i).let { if (it < 0) n else it + 1 }
                    add(TokenKind.Attribute, i, end); i = end
                }
                c.isDigit() -> {
                    var j = i
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '_' ||
                            (code[j] == '.' && j + 1 < n && code[j + 1].isDigit()))
                    ) j++
                    add(TokenKind.Number, i, j); i = j
                }
                c.isLetter() || c == '_' -> {
                    var j = i
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    val word = code.substring(i, j)
                    var k = j
                    while (k < n && code[k] == ' ') k++
                    val next = if (k < n) code[k] else ' '
                    val kind = when {
                        j < n && code[j] == '!' && word !in keywords -> TokenKind.Macro
                        word in keywords -> TokenKind.Keyword
                        word in primitives -> TokenKind.Type
                        word[0].isUpperCase() -> TokenKind.Type
                        next == '(' || lastWord == "fn" -> TokenKind.Function
                        else -> TokenKind.Plain
                    }
                    val end = if (kind == TokenKind.Macro) j + 1 else j
                    add(kind, i, end); i = end
                    lastWord = word
                }
                else -> i++
            }
        }
        return out
    }

    fun color(kind: TokenKind): Color = when (kind) {
        TokenKind.Keyword -> CodeColors.Keyword
        TokenKind.Type -> CodeColors.Type
        TokenKind.Function -> CodeColors.Function
        TokenKind.Macro -> CodeColors.Macro
        TokenKind.String -> CodeColors.Str
        TokenKind.Number -> CodeColors.Number
        TokenKind.Comment -> CodeColors.Comment
        TokenKind.Lifetime -> CodeColors.Lifetime
        TokenKind.Attribute -> CodeColors.Attribute
        TokenKind.Plain -> CodeColors.Plain
    }

    fun highlight(code: String): AnnotatedString = buildAnnotatedString {
        append(code)
        addStyle(SpanStyle(color = CodeColors.Plain), 0, code.length)
        for (t in tokenize(code)) {
            val style = when (t.kind) {
                TokenKind.Comment -> SpanStyle(color = color(t.kind), fontStyle = FontStyle.Italic)
                TokenKind.Keyword -> SpanStyle(color = color(t.kind), fontWeight = FontWeight.SemiBold)
                else -> SpanStyle(color = color(t.kind))
            }
            addStyle(style, t.start, t.end)
        }
    }
}
