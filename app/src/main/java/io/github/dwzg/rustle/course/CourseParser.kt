package io.github.dwzg.rustle.course

/**
 * Parses the plain-text course format described in docs/COURSE_FORMAT.md.
 * tools/verify_course.py validates the same files more strictly at build time,
 * so this parser only needs to be tolerant and correct for valid input.
 */
object CourseParser {

    class ParseException(message: String) : IllegalArgumentException(message)

    private class Block(val kind: String, val arg: String, val line: Int) {
        val lines = mutableListOf<String>()
    }

    fun parseUnit(text: String, fileName: String = "unit"): CourseUnit {
        val blocks = splitBlocks(text, fileName)
        val head = blocks.firstOrNull()
        if (head == null || head.kind != "unit") throw ParseException("$fileName: must start with @unit")
        val unitMeta = meta(head)

        val lessons = mutableListOf<Lesson>()
        var lessonId: String? = null
        var lessonTitle = ""
        val steps = mutableListOf<Step>()

        fun flush() {
            val id = lessonId ?: return
            lessons += Lesson(id, lessonTitle, steps.toList())
            steps.clear()
        }

        for (b in blocks.drop(1)) {
            if (b.kind == "lesson") {
                flush()
                lessonId = b.arg
                lessonTitle = meta(b)["title"] ?: b.arg
                continue
            }
            val id = lessonId ?: throw ParseException("$fileName:${b.line}: step outside a lesson")
            steps += parseStep(b, "$id#${steps.size}", fileName)
        }
        flush()

        return CourseUnit(
            id = head.arg,
            title = unitMeta["title"] ?: head.arg,
            tagline = unitMeta["tagline"] ?: "",
            icon = unitMeta["icon"] ?: "🦀",
            lessons = lessons,
        )
    }

    private fun splitBlocks(text: String, fileName: String): List<Block> {
        val blocks = mutableListOf<Block>()
        var inFence = false
        text.lines().forEachIndexed { i, line ->
            if (line.startsWith("```")) inFence = !inFence
            if (!inFence && line.startsWith("@")) {
                val kind = line.drop(1).substringBefore(' ').trim()
                val arg = line.drop(1).substringAfter(' ', "").trim()
                blocks += Block(kind, arg, i + 1)
            } else {
                val current = blocks.lastOrNull()
                if (current != null) current.lines += line
                else if (line.isNotBlank()) throw ParseException("$fileName:${i + 1}: text before first block")
            }
        }
        return blocks
    }

    private fun meta(b: Block): Map<String, String> =
        b.lines.filter { ':' in it }.associate { it.substringBefore(':').trim() to it.substringAfter(':').trim() }

    private class Parts {
        val codes = mutableListOf<String>()
        val plus = mutableListOf<String>()
        val minus = mutableListOf<String>()
        val eq = mutableListOf<String>()
        val tilde = mutableListOf<String>()
        val why = mutableListOf<String>()
    }

    private fun parts(b: Block, fileName: String): Parts {
        val p = Parts()
        var inFence = false
        val code = mutableListOf<String>()
        for (line in b.lines) {
            if (line.startsWith("```")) {
                if (inFence) {
                    p.codes += code.joinToString("\n")
                    code.clear()
                }
                inFence = !inFence
                continue
            }
            if (inFence) {
                code += line
                continue
            }
            val s = line.trim()
            when {
                s.isEmpty() || s.startsWith("!") -> Unit
                s.startsWith("+ ") -> p.plus += s.drop(2).trim()
                s.startsWith("- ") -> p.minus += s.drop(2).trim()
                s.startsWith("= ") -> p.eq += s.drop(2).trim()
                s.startsWith("~ ") -> p.tilde += s.drop(2).trim()
                s.startsWith("? ") -> p.why += s.drop(2).trim()
                else -> throw ParseException("$fileName:${b.line}: unexpected line '$line'")
            }
        }
        return p
    }

    private fun parseStep(b: Block, key: String, fileName: String): Step {
        if (b.kind == "explain") {
            return ExplainStep(key, b.arg, b.lines.joinToString("\n").trim())
        }
        val p = parts(b, fileName)
        val why = p.why.joinToString(" ")
        fun code(): String = p.codes.singleOrNull()
            ?: throw ParseException("$fileName:${b.line}: @${b.kind} needs exactly one code block")

        return when (b.kind) {
            "choice" -> ChoiceStep(
                key = key,
                prompt = b.arg,
                code = p.codes.firstOrNull(),
                answer = p.plus.singleOrNull() ?: throw ParseException("$fileName:${b.line}: need one '+' option"),
                wrong = p.minus.toList(),
                explanation = why,
            )
            "fill" -> FillStep(key, b.arg, code(), p.eq.toList(), p.tilde.toList(), why)
            "order" -> OrderStep(key, b.arg, code().lines().filter { it.isNotBlank() }, why)
            "compiles" -> CompilesStep(
                key = key,
                prompt = b.arg.ifEmpty { "Will this compile?" },
                code = code(),
                compiles = when (p.eq.singleOrNull()) {
                    "yes" -> true
                    "no" -> false
                    else -> throw ParseException("$fileName:${b.line}: needs '= yes' or '= no'")
                },
                explanation = why,
            )
            "bug" -> BugStep(
                key = key,
                prompt = b.arg,
                code = code(),
                bugLine = p.eq.singleOrNull()?.toIntOrNull()
                    ?: throw ParseException("$fileName:${b.line}: needs '= N'"),
                explanation = why,
            )
            else -> throw ParseException("$fileName:${b.line}: unknown block @${b.kind}")
        }
    }
}
