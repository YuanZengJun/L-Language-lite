package ldk.l.litec.parser

enum class TokenType {
    // 字面量
    IDENTIFIER, STRING, FLOAT, INT,

    // 关键字
    LET, IF, ELSE, WHILE, FOR, FUN, RETURN, TRUE, FALSE, NULL,

    // 操作符
    PLUS, MINUS, STAR, SLASH,
    EQUAL, EQUAL_EQUAL, BANG, BANG_EQUAL,
    LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
    PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL,
    ARROW,OR,AND,PLUS_PLUS,MINUS_MINUS,PERCENT,PERCENT_EQUAL,

    // 分隔符
    COMMA, DOT, SEMICOLON, COLON,
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,

    // 其他
    EOF;

    fun leftBindingPower(): Int = when (this) {
        // 赋值（最低，右结合）
        EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL, PERCENT_EQUAL -> 1

        // 逻辑 or
        OR -> 2

        // 逻辑 and
        AND -> 3

        // 相等性
        EQUAL_EQUAL, BANG_EQUAL -> 4

        // 比较
        LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> 5

        // 算术
        PLUS, MINUS -> 6
        STAR, SLASH, PERCENT -> 7

        // 一元后缀（如 ++ --）
        PLUS_PLUS, MINUS_MINUS -> 8

        // 成员访问、调用、索引（最高）
        DOT -> 9
        LBRACKET -> 9
        LPAREN -> 9

        else -> -1
    }

    fun isLeftAssociative(): Boolean = when (this) {
        // 右结合：赋值
        EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL, PERCENT_EQUAL -> false

        // 其他大多数是左结合
        else -> true
    }

    fun isRightAssociative(): Boolean = !isLeftAssociative()
}