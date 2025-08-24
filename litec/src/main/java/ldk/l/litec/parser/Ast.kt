package ldk.l.litec.parser

import ldk.l.litec.util.Span

sealed class Expr {
    abstract val span: Span

    data class LiteralExpr(
        override val span: Span,
        val value: Any?
    ) : Expr()

    data class BinaryExpr(
        override val span: Span,
        val left: Expr,
        val right: Expr,
        val op: Token,
    ) : Expr()

    data class VariableExpr(
        override val span: Span,
        val name: String
    ) : Expr()

    data class Assignment(
        override val span: Span,
        val name: Expr,
        val expr: Expr
    ) : Expr()

    data class UnaryExpr(
        override val span: Span,
        val op: Token,
        val operand: Expr
    ) : Expr()

    data class IndexExpr(
        override val span: Span,
        val left: Expr,
        val index: Expr
    ) : Expr()

    data class CallExpr(
        override val span: Span,
        val callee: Expr,
        val arguments: List<Expr>
    ) : Expr()

    data class MemberAccessExpr(
        override val span: Span,
        val accessed: Expr,
        val name: Token
    ) : Expr()

    data class PostfixExpr(
        override val span: Span,
        val operand: Expr,
        val op: Token,
    ) : Expr()
}

sealed class Stmt {
    abstract val span: Span

    data class Program(
        override val span: Span,
        val statements: List<Stmt>
    ) : Stmt()

    data class ExprStmt(
        override val span: Span,
        val expr: Expr
    ) : Stmt()

    data class PrintStmt(
        override val span: Span,
        val value: Expr
    ) : Stmt()

    data class VarDeclaration(
        override val span: Span,
        val keywordSpan: Span,
        val name: Expr,
        val init: Expr
    ) : Stmt()
}