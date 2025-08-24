package ldk.l.litec.parser

import ldk.l.litec.util.Span
import ldk.l.litec.util.Stdout
import ldk.l.litec.util.error.CompilerError
import ldk.l.litec.util.error.ErrorType

class LiteParser(
    private val lexer: LiteLexer,
    private val filePath: String
) {
    private val errors: MutableList<CompilerError.ParserError> = mutableListOf()

    private var currentToken: Token? = lexer.next()

    fun hasError() = errors.isNotEmpty()
    fun printErrors() {
        errors.forEach {
            Stdout.println(it)
        }
    }

    private fun consume(): Token? {
        val prev = currentToken
        currentToken = lexer.next()
        return prev
    }

    private fun current() = currentToken

    /**
     * ✅ 判断当前 token 是否是某个类型
     */
    private fun match(type: TokenType): Boolean {
        if (current()?.type == type) {
            consume()
            return true
        }
        return false
    }

    /**
     * ✅ 期望某个 token，如果不是就报错
     */
    private fun expect(type: TokenType, error: CompilerError.ParserError): Token? {
        if (current()?.type == type) {
            return consume()
        } else {
            errors.add(error)
            return null
        }
    }

    private fun addError(
        span: Span,
        errorType: ErrorType,
        expected: String?,
        found: String?,
        filePath: String? = null,
        templateArgs: Array<out Any> = emptyArray(),
        note: String? = null,
        help: String? = null
    ) {
        errors.add(
            CompilerError.ParserError(
                span,
                errorType,
                expected,
                found,
                filePath,
                templateArgs,
                note,
                help
            )
        )
    }

    /**
     * 同步到下一个语句边界
     */
    fun synchronize() {
        while (!match(TokenType.EOF) && !isAtStatementBoundary()) {
            consume()
        }
        if (match(TokenType.EOF)) return
        consume() // 跳过边界 token（如 ; }
    }

    private fun isAtStatementBoundary(): Boolean = when (current()?.type) {
        TokenType.SEMICOLON,
        TokenType.RBRACE,
        TokenType.LET,
        TokenType.FUN,
        TokenType.IF,
        TokenType.WHILE,
        TokenType.RETURN -> true
        else -> false
    }

    fun lookahead(n: Int): Token? {
        val snapshot = lexer.checkpoint()
        return try {
            repeat(n-1) { lexer.next() }
            lexer.next()
        } finally {
            lexer.restore(snapshot)
        }
    }

    /**
     * 🔁 安全回溯：尝试执行一个解析函数
     * 成功 → 返回结果
     * 失败 → 自动回退，返回 null
     */
    // 工具函数
    private inline fun <T> backtrack(crossinline block: () -> T?): T? {
        val snapshot = lexer.checkpoint()
        val result = block()
        if (result == null) {
            lexer.restore(snapshot)
        }
        return result
    }

    fun parseExpr(minPrecedence: Int = 0): Expr? {
        var left = parsePrefix() ?: return null

        while (true) {
            val op = current() ?: break

            val infixPrecedence = op.type.leftBindingPower()

            if (infixPrecedence < minPrecedence) {
                break
            }

            consume()

            left = parseInfix(left,op)
                ?: return null
        }

        return left
    }

    private fun parseInfix(left: Expr, token: Token): Expr? {
        return when (token.type) {
            // 处理二元操作符 (+, -, *, /, ==, <, &&, || 等)
            TokenType.PLUS,
            TokenType.MINUS,
            TokenType.STAR,
            TokenType.SLASH,
            TokenType.PERCENT,
            TokenType.EQUAL_EQUAL,
            TokenType.BANG_EQUAL,
            TokenType.LESS,
            TokenType.LESS_EQUAL,
            TokenType.GREATER,
            TokenType.GREATER_EQUAL,
            TokenType.AND,
            TokenType.OR,
            TokenType.EQUAL,
            TokenType.PLUS_EQUAL,
            TokenType.MINUS_EQUAL,
            TokenType.STAR_EQUAL,
            TokenType.SLASH_EQUAL,
            TokenType.PERCENT_EQUAL -> {
                // 1. 计算右操作数的最小优先级
                val nextMinPrecedence = if (token.type.isRightAssociative()) {
                    token.type.leftBindingPower() // 允许相同优先级
                } else {
                    token.type.leftBindingPower() + 1 // 强制左结合
                }

                // 2. 解析右操作数
                val right = parseExpr(nextMinPrecedence) ?: return null

                // 3. 构造二元表达式节点
                Expr.BinaryExpr(left.span.extendTo(right.span), left, right, token)
            }

            // 处理函数调用 a()
            TokenType.LPAREN -> {
                // 消费 '(' 已经在 parseExpr 的循环中完成
                val arguments = mutableListOf<Expr>()
                // 解析参数列表，直到遇到 ')'
                if (current()?.type != TokenType.RPAREN) { // 如果不是空参
                    do {
                        val arg = parseExpr() ?: break
                        arguments.add(arg)
                    } while (match(TokenType.COMMA))
                }
                // 期望 ')'
                val closeParen = expect(
                    TokenType.RPAREN,
                    error = CompilerError.ParserError(
                        span = token.span,
                        errorType = ErrorType.MISSING_PARENTHESIS,
                        expected = ")",
                        found = current()?.lexeme,
                        filePath = filePath,
                        help = "添加上 ')'"
                    )
                ) ?: return null // 如果缺少 ')', 返回 null

                Expr.CallExpr(left.span.extendTo(closeParen.span), left, arguments)
            }

            // 处理数组索引 a[0]
            TokenType.LBRACKET -> {
                // 消费 '[' 已在循环中完成
                val index = parseExpr() ?: return null
                val closeBracket = expect(
                    TokenType.RBRACKET,
                    error = CompilerError.ParserError(
                        span = token.span,
                        errorType = ErrorType.MISSING_PARENTHESIS,
                        expected = "]",
                        found = current()?.lexeme,
                        filePath = filePath,
                        help = "添加上 ']'"
                    )
                ) ?: return null
                Expr.IndexExpr(left.span.extendTo(closeBracket.span), left, index)
            }

            // 处理成员访问 a.b
            TokenType.DOT -> {
                val identifier = expect(
                    TokenType.IDENTIFIER,
                    error = CompilerError.ParserError(
                        span = current()!!.span,
                        errorType = ErrorType.EMPTY_EXPRESSION,
                        expected = "identifier",
                        found = current()?.lexeme,
                        filePath = filePath,
                        help = "`.` 后应跟一个标识符"
                    )
                ) ?: return null
                Expr.MemberAccessExpr(left.span.extendTo(identifier.span), left, identifier)
            }

            // 处理后缀操作符 i++, i--
            TokenType.PLUS_PLUS, TokenType.MINUS_MINUS -> {
                // 这里通常构造后缀操作表达式节点
                Expr.PostfixExpr(left.span.extendTo(token.span), left,token)
            }

            else -> {
                // 理论上不应该到达这里，因为 parseExpr 的循环只对 leftBindingPower > 0 的 token 调用 parseInfix
                addError(
                    span = token.span,
                    errorType = ErrorType.UNEXPECTED_TOKEN,
                    expected = "infix operator",
                    found = token.lexeme,
                    filePath = filePath
                )
                null
            }
        }
    }

    private fun parsePrefix(): Expr? {
        return when (val type = current()?.type) {
            // 字面量
            TokenType.TRUE -> Expr.LiteralExpr(current()!!.span, true).also { consume() }
            TokenType.FALSE -> Expr.LiteralExpr(current()!!.span, false).also { consume() }
            TokenType.NULL -> Expr.LiteralExpr(current()!!.span, null).also { consume() }
            TokenType.INT -> Expr.LiteralExpr(current()!!.span, current()!!.lexeme.toInt()).also { consume() }
            TokenType.FLOAT -> Expr.LiteralExpr(current()!!.span, current()!!.lexeme.toDouble()).also { consume() }
            TokenType.STRING -> Expr.LiteralExpr(current()!!.span, current()!!.lexeme).also { consume() }

            // 标识符
            TokenType.IDENTIFIER -> Expr.VariableExpr(current()!!.span, current()!!.lexeme).also { consume() }

            // 前缀操作符
            TokenType.BANG, TokenType.MINUS, TokenType.PLUS -> {
                val op = consume()
                val expr = parseExpr(TokenType.MINUS.leftBindingPower()) ?: return null
                Expr.UnaryExpr(op!!.span.extendTo(expr.span), op, expr)
            }

            // 括号表达式
            TokenType.LPAREN -> {
                val start = consume() // (
                val expr = parseExpr()

                val closeParen = expect(
                    TokenType.RPAREN,
                    error = CompilerError.ParserError(
                        span = start!!.span.extendTo(current()!!.span),
                        errorType = ErrorType.MISSING_PARENTHESIS,
                        expected = ")",
                        found = current()?.lexeme,
                        filePath = filePath,
                        help = "添加上 ')'"
                    )
                )
                if (closeParen == null) {
                    return null // 右括号缺失，整个括号表达式失败
                }
                expr
            }

            else -> null
        }
    }
}