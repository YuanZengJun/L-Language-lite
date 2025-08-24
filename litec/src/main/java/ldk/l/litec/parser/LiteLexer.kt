package ldk.l.litec.parser

import ldk.l.litec.util.*
import ldk.l.litec.util.error.CompilerError
import ldk.l.litec.util.error.ErrorType

class LiteLexer(
    private var source: CharStream,
    private var filePath: String
) {
    val tokens: MutableList<Token> = mutableListOf()
    val errors: MutableList<CompilerError> = mutableListOf()

    fun hasError() = errors.isNotEmpty()
    fun printTokens() {
        tokens.forEach {
            Stdout.println(it)
        }
    }
    fun printErrors() {
        errors.forEach {
            Stdout.println(it)
        }
    }

    /**
     * 创建当前状态的快照
     */
    fun checkpoint(): Snapshot = source.checkpoint()

    /**
     * 恢复到指定快照
     */
    fun restore(snapshot: Snapshot) = source.restore(snapshot)

    companion object {
        private val Keywords = mapOf(
            "let"       to    TokenType.LET,
            "if"        to    TokenType.IF,
            "else"      to    TokenType.ELSE,
            "while"     to    TokenType.WHILE,
            "for"       to    TokenType.FOR,
            "fun"       to    TokenType.FUN,
            "return"    to    TokenType.RETURN,
            "null"      to    TokenType.NULL,
            "true"      to    TokenType.TRUE,
            "false"     to    TokenType.FALSE
        )

        fun getIdentifierType(text: String): TokenType {
            return Keywords[text] ?: TokenType.IDENTIFIER
        }
    }

    fun tokenize(source: CharStream, filePath: String): List<Token> {
        this.source = source
        this.filePath = filePath

        while (!source.isAtEnd()) {
            val token = scanToken()
            token?.let { addToken(it) }
        }

        val eof = source.position
        tokens.add(Token(TokenType.EOF, "", Span(eof)))

        return tokens.toList()
    }

    fun next(): Token? {
        val token = scanToken()
        return token
    }

    private fun scanToken(): Token? {
        val start = source.position
        val ch = source.peek()

        if (source.isAtEnd()) {
            return Token(
                type = TokenType.EOF,
                lexeme = "",
                span = Span(start)
            )
        }

        when {
            // ✅ 空白字符
            ch in setOf(" ", "\t", "\n", "\r") -> {
                source.next()
                return scanToken()
            }

            // ✅ 单字符分隔符
            ch == "(" -> return Token(TokenType.LPAREN, "(", Span(start)).also { source.next() }
            ch == ")" -> return Token(TokenType.RPAREN, ")", Span(start)).also { source.next() }
            ch == "{" -> return Token(TokenType.LBRACE, "{", Span(start)).also { source.next() }
            ch == "}" -> return Token(TokenType.RBRACE, "}", Span(start)).also { source.next() }
            ch == "[" -> return Token(TokenType.LBRACKET, "[", Span(start)).also { source.next() }
            ch == "]" -> return Token(TokenType.RBRACKET, "]", Span(start)).also { source.next() }
            ch == ";" -> return Token(TokenType.SEMICOLON, ";", Span(start)).also { source.next() }
            ch == "," -> return Token(TokenType.COMMA, ",", Span(start)).also { source.next() }
            ch == ":" -> return Token(TokenType.COLON, ":", Span(start)).also { source.next() }

            // ✅ 字符串字面量
            ch == "\"" || ch == "'" -> {
                scanString()
            }

            // ✅ 数字（以 . 开头的浮点数）
            ch == "." -> {
                return if (source.peek(1).isDigit()) {
                    scanNumber()
                } else {
                    Token(TokenType.DOT, ".", Span(start)).also {
                        source.next()
                    }
                }
            }

            // ✅ 注释
            ch == "/" -> {
                if (source.peek(1) == "/") {
                    while (!source.isAtEnd() && source.peek() != "\n") {
                        source.next()
                    }
                    return scanToken()
                } else if (source.peek(1) == "*") {
                    source.next()
                    source.next()
                    var foundEnd = false
                    while (!source.isAtEnd()) {
                        if (source.peek() == "*" && source.peek(1) == "/") {
                            source.next()
                            source.next()
                            foundEnd = true
                            break
                        }
                        source.next()
                    }
                    if (!foundEnd) {
                        addError(
                            errorType = ErrorType.UNTERMINATED_COMMENT,
                            start = start,
                            end = source.position,
                            note = "多行注释并没有结束",
                            help = "添加一个 */ 在注释结尾"
                        )
                    }
                    return scanToken()
                } else if (matchNext("=")) {
                    return Token(TokenType.SLASH_EQUAL, "/=", Span(start,source.position)).also { source.next() }
                } else {
                    return Token(TokenType.SLASH, "/", Span(start,source.position)).also { source.next() }
                }
            }

            // ✅ 加法与自增: +, ++
            ch == "+" -> {
                when {
                    matchNext("+") -> return Token(TokenType.PLUS_PLUS, "++", Span(start,source.position)).also {source.next()}
                    matchNext("=") -> return Token(TokenType.PLUS_EQUAL, "+=", Span(start,source.position)).also {source.next()}
                    else -> return Token(TokenType.PLUS, "+", Span(start)).also { source.next() }
                }
            }

            // ✅ 减法、自减、箭头: -, --, -=
            ch == "-" -> {
                when {
                    matchNext("-") -> return Token(TokenType.MINUS_MINUS, "--", Span(start,source.position)).also { source.next() }
                    matchNext("=") -> return Token(TokenType.MINUS_EQUAL, "-=", Span(start,source.position)).also { source.next() }
                    else -> return Token(TokenType.MINUS, "-", Span(start)).also { source.next() }
                }
            }

            // ✅ 乘法与复合赋值: *, *=
            ch == "*" -> {
                if (matchNext("=")) {
                    return Token(TokenType.STAR_EQUAL, "*=", Span(start,source.position)).also { source.next() }
                } else {
                    return Token(TokenType.STAR, "*", Span(start)).also { source.next() }
                }
            }

            // ✅ 取模与复合赋值: %, %=
            ch == "%" -> {
                if (matchNext("=")) {
                    return Token(TokenType.PERCENT_EQUAL, "%=", Span(start,source.position)).also { source.next() }
                } else {
                    return Token(TokenType.PERCENT, "%", Span(start)).also { source.next() }
                }
            }

            // ✅ 赋值与比较: =, ==
            ch == "=" -> {
                if (matchNext("=")) {
                    return Token(TokenType.EQUAL_EQUAL, "==", Span(start,source.position)).also { source.next() }
                } else {
                    return Token(TokenType.EQUAL, "=", Span(start)).also { source.next() }
                }
            }

            // ✅ 非/不等于: !, !=
            ch == "!" -> {
                if (matchNext("=")) {
                    return Token(TokenType.BANG_EQUAL, "!=", Span(start,source.position)).also { source.next() }
                } else {
                    return Token(TokenType.BANG, "!", Span(start)).also { source.next() }
                }
            }

            // ✅ 小于/小于等于: <, <=
            ch == "<" -> {
                if (matchNext("=")) {
                    return Token(TokenType.LESS_EQUAL, "<=", Span(start,source.position)).also { source.next() }
                } else {
                    return Token(TokenType.LESS, "<", Span(start)).also { source.next() }
                }
            }

            // ✅ 大于/大于等于: >, >=
            ch == ">" -> {
                if (matchNext("=")) {
                    return Token(TokenType.GREATER_EQUAL, ">=", Span(start,source.position)).also { source.next() }
                } else {
                    return Token(TokenType.GREATER, ">", Span(start)).also { source.next() }
                }
            }

            // ✅ 逻辑与: &&
            ch == "&" -> {
                if (matchNext("&")) {
                    return Token(TokenType.AND, "&&", Span(start,source.position)).also { source.next() }
                } else {
                    addError(
                        ErrorType.ILLEGAL_CHARACTER,
                        start,
                        end = source.position,
                        note = "单独的 '&' 不被支持（你是否想写 &&?）",
                        help = "使用 '&&' 表示逻辑与"
                    )
                    source.next()
                    return scanToken()
                }
            }

            // ✅ 逻辑或: ||
            ch == "|" -> {
                if (matchNext("|")) {
                    return Token(TokenType.OR, "||", Span(start,source.position)).also { source.next() }
                } else {
                    addError(
                        ErrorType.ILLEGAL_CHARACTER,
                        start,
                        end = source.position,
                        note = "单独的 '|' 不被支持（你是否想写 ||？）",
                        help = "使用 '||' 表示逻辑或"
                    )
                    source.next()
                    return scanToken()
                }
            }

            // ✅ 标识符（关键字/变量名）
            isIdentifierStart(ch) -> {
                return scanIdentifier()
            }

            // ✅ 数字（以数字开头）
            ch.isDigit() -> {
                return scanNumber()
            }

            else -> {
                addError(
                    ErrorType.ILLEGAL_CHARACTER,
                    start,
                    end = source.position,
                    note = "未知字符 '${source.peek()}'",
                    help = "修改成其他字符",
                    templateArgs = arrayOf(source.peek()),
                )
                source.next()
                return null
            }
        }
        return null
    }

    private fun scanString() {
        val start = source.position
        val quoteChar = source.peek() // 是 '"' 还是 '\''
        source.next() // 跳过起始引号

        val buffer = StringBuilder()

        while (!source.isAtEnd()) {
            val ch = source.peek()

            if (ch == quoteChar) {
                // 找到结束引号，结束字符串
                source.next() // 消费结束引号
                val text = buffer.toString()
                addToken(Token(TokenType.STRING, text, Span(start,source.getPositionFromOffset(source.position.offset - 1))))
                return
            }

            if (ch == "\\") {
                // 处理转义字符
                source.next() // 消费 '\'
                if (source.isAtEnd()) {
                    // 转义符在末尾，报错
                    addError(
                        errorType = ErrorType.UNTERMINATED_STRING,
                        start = start,
                        end = source.position,
                        note = "字符串中转义字符 '\\' 后面缺少字符",
                        help = "请检查转义序列，例如使用 \\\", \\n, \\\\ 等"
                    )
                    return
                }

                when (val escape = source.peek()) {
                    "n" -> {
                        buffer.append('\n')
                        source.next()
                    }
                    "r" -> {
                        buffer.append('\r')
                        source.next()
                    }
                    "t" -> {
                        buffer.append('\t')
                        source.next()
                    }
                    "\\" -> {
                        buffer.append('\\')
                        source.next()
                    }
                    "\"" -> {
                        buffer.append('"')
                        source.next()
                    }
                    "'" -> {
                        buffer.append('\'')
                        source.next()
                    }
                    else -> {
                        // 未知转义，可选择保留原样或报错
                        // 这里选择保留：\x -> x
                        buffer.append(escape)
                        source.next()
                    }
                }
            } else {
                // 普通字符
                buffer.append(source.next())
            }
        }

        // 循环结束但未遇到结束引号 → 未闭合字符串
        addError(
            errorType = ErrorType.UNTERMINATED_STRING,
            start = start,
            end = source.position,
            note = "未闭合的字符串字面量，缺少结束的 '$quoteChar'",
            help = "请在字符串末尾添加 '$quoteChar'"
        )
    }

    private fun scanNumber(): Token {
        val start = source.position
        val buffer = StringBuilder()
        var type = TokenType.INT

        // 读取整数部分
        while (source.peek().isDigit()) {
            buffer.append(source.peek())
            source.next()
        }

        // 处理小数部分
        if (source.peek() == "." && source.peek(1).isDigit()) {
            buffer.append(source.peek()) // '.'
            source.next()
            while (source.peek().isDigit()) {
                buffer.append(source.peek())
                source.next()
            }
            type = TokenType.FLOAT
        }

        val text = buffer.toString()
        return Token(type, text, Span(start,source.getPositionFromOffset(source.position.offset - 1)))
    }

    private fun scanIdentifier(): Token {
        val start = source.position

        // 读取第一个字符（已知是合法首字符）
        val first = source.next()
        val builder = StringBuilder().append(first)

        // 继续读取后续字符
        while (!source.isAtEnd()) {
            val ch = source.peek()
            if (isIdentifierPart(ch)) {
                builder.append(source.next())
            } else {
                break
            }
        }

        val text = builder.toString()

        // 检查是否是关键字
        val tokenType = getIdentifierType(text)

        return Token(type = tokenType, lexeme = text, span = Span(start,source.getPositionFromOffset(source.position.offset - 1)))
    }

    private fun isIdentifierStart(ch: String): Boolean {
        if (ch.length != 1) return false
        val c = ch[0]
        return c.isLetter() || c == '_' || Character.isUnicodeIdentifierStart(c.code)
    }

    private fun isIdentifierPart(ch: String): Boolean {
        if (ch.length != 1) return false
        val c = ch[0]
        return c.isLetterOrDigit() || c == '_' || Character.isUnicodeIdentifierPart(c.code)
    }

    private fun matchNext(except: String): Boolean {
        if (source.peek(1) == except) {
            source.next()
            return true
        }
        return false
    }

    // 主要的错误添加函数
    private fun addError(
        errorType: ErrorType,
        start: Position,
        end: Position? = null,
        note: String? = null,
        help: String? = null,
        vararg templateArgs: Any,
    ) {
        val span = Span(start,end?:start)

        errors.add(
            CompilerError.LexerError(
                span = span,
                errorType = errorType,
                filePath = filePath,
                templateArgs = templateArgs,
                note = note,
                help = help
            )
        )
    }

    private fun addToken(token: Token) {
        tokens.add(token)
    }
}