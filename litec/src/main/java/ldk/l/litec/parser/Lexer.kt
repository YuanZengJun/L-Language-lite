package ldk.l.litec.parser

import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.EmptyParser
import com.github.h0tk3y.betterParse.parser.Parser
import com.vdurmont.emoji.EmojiManager
import ldk.l.litec.util.CharStream
import ldk.l.litec.util.Logger
import ldk.l.litec.util.Position
import ldk.l.litec.util.Stdout
import ldk.l.litec.util.error.CompilerError
import ldk.l.litec.util.error.ErrorFormatter
import ldk.l.litec.util.error.ErrorType

object Lexer : Grammar<Unit>() {
    // 1. 实现 rootParser
    override val rootParser: Parser<Unit> = EmptyParser

    // 2. 定义原生 Token 规则
    val letToken by literalToken("let")
    val ifToken by literalToken("if")
    val elseToken by literalToken("else")
    val funToken by literalToken("fun")
    val plusToken by literalToken("+")
    val minusToken by literalToken("-")
    val starToken by literalToken("*")
    val slashToken by literalToken("/")
    val eqToken by literalToken("==")
    val assignToken by literalToken("=")
    val lParenToken by literalToken("(")
    val rParenToken by literalToken(")")
    val lBraceToken by literalToken("{")
    val rBraceToken by literalToken("}")
    val semicolonToken by literalToken(";")
    val commaToken by literalToken(",")
    val numberToken by regexToken("\\d+")
    val stringToken by regexToken("(?s)\".*?\"")
    val identifierToken by regexToken("[a-zA-Z_][a-zA-Z0-9_]*")
    val whitespace by regexToken("\\s+", ignore = true)

    // 错误存储
    val errors = mutableListOf<CompilerError.LexerError>()

    fun resetErrors() {
        errors.clear()
    }

    fun hadError(): Boolean = errors.isNotEmpty()

    // 3. 词法分析核心方法
    fun tokenize(source: CharStream, filePath: String): List<TokenMatch> {
        resetErrors()
        checkForIllegalCharacters(source, filePath)
        return tokenizer.tokenize(source.toString()).toList()
    }

    private fun checkForIllegalCharacters(source: CharStream, filePath: String?) {
        var line = 1
        var column = 1
        var charIndex = 0 // 使用字符单元索引

        while (charIndex < source.length()) {
            val unit = source[charIndex] // 获取整个字符单元
            val unitLength = unit.length

            // 1. 检查Emoji单元
            if (isEmojiUnit(unit)) {
                // 添加错误时使用字符单元索引作为绝对位置
                errors.add(CompilerError.LexerError(
                    position = Position(
                        line = line,
                        column = column,
                        offset = charIndex // 使用字符单元索引作为绝对位置
                    ),
                    source = source,
                    errorType = ErrorType.INVALID_EMOJI,
                    filePath = filePath,
                    templateArgs = arrayOf(unit),
                    note = "检测到Emoji符号: $unit",
                    help = "禁止使用Emoji，请使用字母/数字/运算符"
                ))

                // 更新位置
                column += 1
                charIndex += 1
                continue
            }

            // 2. 处理换行符（特殊位置更新）
            if (unit == "\n") {
                line++
                column = 1
                charIndex++
                continue
            }

            // 3. 检查其他非法字符（仅当单元长度为1时）
            if (unitLength == 1) {
                val char = unit[0]
                var isLegal = true
                var errorType: ErrorType = ErrorType.ILLEGAL_CHARACTER
                var errorNote = ""
                var errorHelp = "请检查输入是否有误"

                // 判断合法性
                isLegal = when {
                    char.isLetterOrDigit() -> true
                    char in setOf('+', '-', '*', '/', '=', ';', '(', ')', '{', '}', ',', '"', '\'', '_') -> true
                    char.isWhitespace() -> true
                    else -> false
                }

                // 分类错误类型
                if (!isLegal) {
                    errorType = when {
                        // 非法运算符
                        char in setOf('@', '#', '$', '%', '^', '&', '|', '~', '`') -> {
                            errorNote = "检测到非法运算符: $char"
                            errorHelp = "支持的运算符：+、-、*、/、=、==、!=、>、<、>=、<="
                            ErrorType.INVALID_OPERATOR
                        }

                        // 控制字符
                        char.isISOControl() -> {
                            errorNote = "检测到控制字符: U+${char.code.toString(16).uppercase()}"
                            errorHelp = "请移除退格、制表符等不可见控制字符"
                            ErrorType.CONTROL_CHARACTER_NOT_ALLOWED
                        }

                        // 中文引号
                        char in setOf('“', '”', '‘', '’', '「', '」') -> {
                            errorNote = "检测到中文引号: $char"
                            errorHelp = "请使用英文半角引号（\" 或 '）"
                            ErrorType.CHINESE_QUOTE_NOT_ALLOWED
                        }

                        // 中文标点
                        char in setOf('，', '。', '；', '：', '？', '！', '＂', '，', '＇') -> {
                            errorNote = "检测到中文标点: $char"
                            errorHelp = "请使用英文半角标点（, . ; : ? !）"
                            ErrorType.CHINESE_PUNCTUATION_NOT_ALLOWED
                        }

                        // 未结束字符串
                        (char == '"' && !source.substring(charIndex).endsWith('"')) -> {
                            errorNote = "第 $line 行字符串未结束"
                            errorHelp = "请在字符串末尾添加闭合英文引号（\"）"
                            ErrorType.UNTERMINATED_STRING
                        }

                        // 默认非法字符
                        else -> {
                            errorNote = "检测到非法字符: $char (U+${char.code.toString(16).uppercase()})"
                            errorHelp = "仅支持字母、数字、下划线及指定运算符/标点"
                            ErrorType.ILLEGAL_CHARACTER
                        }
                    }

                    // 添加错误 - 使用字符单元索引作为绝对位置
                    errors.add(CompilerError.LexerError(
                        position = Position(
                            line = line,
                            column = column,
                            offset = charIndex // 使用字符单元索引作为绝对位置
                        ),
                        source = source,
                        errorType = errorType,
                        filePath = filePath,
                        templateArgs = arrayOf(char),
                        note = errorNote,
                        help = errorHelp
                    ))

                    // 记录日志
                    Logger.error(
                        module = "Lexer",
                        content = "非法字符: '$char' | 位置: $line:$column | 类型: ${errorType.code}"
                    )
                }
            }

            // 4. 更新位置
            column += when {
                unit == "\t" -> 4
                else -> 1
            }
            charIndex++ // 移动到下一个字符单元
        }
    }

    private fun isEmojiUnit(unit: String): Boolean {
        if (unit.isEmpty()) return false

        // 检查第一个字符是否是Emoji起始字符
        if (EmojiManager.isEmoji(unit)) return true

        // 特殊处理组合Emoji
        if (unit.contains("\u200D")) { // 包含零宽连接符
            return unit.all { isEmojiComponent(it) }
        }

        // 检查整个单元是否只包含Emoji组件
        return unit.all { isEmojiComponent(it) }
    }

    /**
     * 判断是否是Emoji组件
     */
    private fun isEmojiComponent(char: Char): Boolean {
        return when (char.code) {
            0x200D, // 零宽连接符 (ZWJ)
            0xFE0F, // 变体选择器-16 (VS-16)
            in 0x1F3FB..0x1F3FF, // 肤色修饰符
            in 0x1F1E6..0x1F1FF  // 区域指示符号（国旗组件）
                -> true
            else -> EmojiManager.isEmoji(char.toString()) // 使用重命名的函数
        }
    }

    // 计算位置（行号和列号）
    private fun calculatePosition(offset: Int, source: CharSequence): Pair<Int, Int> {
        if (offset <= 0) return 1 to 1
        val substring = source.take(offset)
        val line = substring.count { it == '\n' } + 1
        val lastNewlineIndex = substring.lastIndexOf('\n').takeIf { it != -1 } ?: -1
        val column = if (lastNewlineIndex == -1) offset else (offset - lastNewlineIndex)
        return line to column
    }

    fun printErrors() {
        if (errors.isEmpty()) {
            Stdout.println("未发现词法错误")
            return
        }

        Stdout.println("发现 ${errors.size} 个词法错误:")
        errors.forEach {
            Stdout.println(ErrorFormatter.format(it))
        }
    }

    fun printTokens(tokens: List<TokenMatch>) {
        tokens.forEach { tokenMatch ->
            val startOffset = tokenMatch.offset
            val (startLine, startColumn) = calculatePosition(startOffset, tokenMatch.input)

            Stdout.println(
                "Token: ${tokenMatch.tokenIndex} " +
                        "| Text: '${tokenMatch.text}' " +
                        "| 位置: $startLine:$startColumn " +
                        "| 偏移量: $startOffset"
            )
        }
    }
}