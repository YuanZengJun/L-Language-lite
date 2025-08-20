package ldk.l.litec.util.error

import ldk.l.litec.util.CharStream
import ldk.l.litec.util.Position
import com.vdurmont.emoji.EmojiManager
import kotlin.math.max
import kotlin.math.min

object ErrorFormatter {
    // ANSI 颜色代码
    private const val RED = "\u001B[31m"
    private const val YELLOW = "\u001B[33m"
    private const val BLUE = "\u001B[34m"
    private const val CYAN = "\u001B[36m"
    private const val RESET = "\u001B[0m"

    // 上下文行数配置
    private const val CONTEXT_LINES = 2

    fun format(error: CompilerError): String {
        return buildString {
            // 1. 错误头部
            appendErrorHeader(error)

            // 2. 源代码上下文
            appendSourceContext(error)

            // 3. 错误标记线
            appendUnderline(error)

            // 4. 错误详情
            appendErrorDetails(error)
        }
    }

    private fun StringBuilder.appendErrorHeader(error: CompilerError) {
        val (line, col) = calculateVisualPosition(error.position, error.source)
        appendLine("${RED}error[${error.errorType.code}]: ${error.errorType.message}$RESET")
        appendLine("${BLUE}in the ${error.filePath ?: "<REPL>"}:$line:$col$RESET")
    }

    private fun StringBuilder.appendSourceContext(error: CompilerError) {
        val errorLineIdx = error.position.line - 1
        val startLine = max(0, errorLineIdx - CONTEXT_LINES)
        val endLine = min(error.source.lineCount() - 1, errorLineIdx + CONTEXT_LINES)
        val lineNumWidth = (endLine + 1).toString().length

        for (i in startLine..endLine) {
            val prefix = if (i == errorLineIdx) " --> " else "     "
            val lineNum = (i + 1).toString().padStart(lineNumWidth)
            appendLine("$prefix$lineNum | ${error.source.getLine(i)}")
        }
    }

    private fun StringBuilder.appendUnderline(error: CompilerError) {
        val errorLine = error.source.getLine(error.position.line - 1)
        val underline = buildUnderline(error, errorLine)
        val leadingSpaces = calculateLeadingSpaces(error.position, errorLine)
        appendLine("${" ".repeat(leadingSpaces)}$underline")
    }

    private fun calculateLeadingSpaces(pos: Position, line: CharStream): Int {
        return 5 + // " --> " 前缀
                pos.line.toString().length + 3 + // 行号和 " | "
                (pos.column - 1)
    }

    private fun buildUnderline(error: CompilerError, line: CharStream): String {
        val col = error.position.column - 1
        if (col >= line.length()) return ""

        val char = line[col]
        return when {
            isChinesePunctuation(char) -> "${RED}^^$RESET"
            isEmojiChar(char) -> buildEmojiUnderline(line, col)
            char == "\t" -> "${RED}^^^^$RESET"
            else -> "${RED}^$RESET"
        }
    }

    // ========== CharStream 增强扩展 ==========
    private fun CharStream.getLine(lineIndex: Int): CharStream {
        val lines = this.toString().split('\n')
        return CharStream(lines.getOrNull(lineIndex) ?: "")
    }

    private fun CharStream.lineCount(): Int {
        return this.toString().count { it == '\n' } + 1
    }
    // ========== END ==========

    // ========== Emoji 处理 ==========
    private fun isEmojiChar(char: String): Boolean {
        if (char.isEmpty()) return false
        return when {
            EmojiManager.isEmoji(char) -> true
            char == "\u200D" -> true  // 零宽连接符
            char[0].code in 0x1F600..0x1F64F -> true  // 补充范围
            else -> false
        }
    }

    private fun buildEmojiUnderline(line: CharStream, start: Int): String {
        // while (end < line.length() && isEmojiComponent(line[end])) {
        //     end++
        // }
        return "$RED^^$RESET"
    }

    private fun isEmojiComponent(char: String): Boolean {
        return isEmojiChar(char) || char == "\uFE0F"
    }
    // ========== END ==========

    private fun isChinesePunctuation(char: String): Boolean {
        if (char.isEmpty()) return false
        return when (char[0]) {
            '＂', '，', '。', '；', '：', '？', '！', '“', '”', '‘', '’', '【', '】', '『', '』', '、', '·', '「', '」' -> true
            else -> false
        }
    }

    private fun StringBuilder.appendErrorDetails(error: CompilerError) {
        appendLine("   = ${error.errorType.template.format(*error.templateArgs)}")
        error.note?.let { appendLine("   = ${CYAN}note: $it$RESET") }
        error.help?.let { appendLine("   = ${YELLOW}help: $it$RESET") }
    }

    private fun calculateVisualPosition(pos: Position, source: CharStream): Pair<Int, Int> {
        // CharStream 已处理代理对，直接使用其逻辑位置
        return pos.line to pos.column
    }

    fun formatWithoutColor(error: CompilerError): String {
        return format(error)
            .replace(RED, "")
            .replace(YELLOW, "")
            .replace(BLUE, "")
            .replace(CYAN, "")
            .replace(RESET, "")
    }
}