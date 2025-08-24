package ldk.l.litec.parser

import ldk.l.litec.util.Span

// 词法单元
data class Token(
    val type: TokenType,
    val lexeme: String,
    val span: Span
)