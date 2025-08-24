package ldk.l.litec.util.error

import ldk.l.litec.util.Span
import ldk.l.litec.util.CharStream
import ldk.l.litec.util.Position

// src/main/java/ldk/l/litec/util/error/CompilerError.kt

sealed class CompilerError(
    open val span: Span,          // 主错误位置（锚点）
    open val errorType: ErrorType,
    open val filePath: String? = null,
    open val templateArgs: Array<out Any> = emptyArray(),
    open val note: String? = null,        // 辅助说明
    open val help: String? = null         // 修复建议
) {
    // 为方便，提供一个构造 range 的快捷方式
    constructor(
        span: Span,
        source: CharStream,
        errorType: ErrorType,
        filePath: String? = null,
        templateArgs: Array<out Any> = emptyArray(),
        note: String? = null,
        help: String? = null
    ) : this(
        span = span,
        errorType = errorType,
        filePath = filePath,
        templateArgs = templateArgs,
        note = note,
        help = help
    )

    data class LexerError(
        override val span: Span,
        override val errorType: ErrorType,
        override val filePath: String? = null,
        override val templateArgs: Array<out Any> = emptyArray(),
        override val note: String? = null,
        override val help: String? = null
    ) : CompilerError(
        span = span,
        errorType = errorType,
        filePath = filePath,
        templateArgs = templateArgs,
        note = note,
        help = help
    ) {
        // 保持 equals/hashCode（Kotlin data class 自动生成）
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LexerError

            if (span != other.span) return false
            if (errorType != other.errorType) return false
            if (filePath != other.filePath) return false
            if (!templateArgs.contentEquals(other.templateArgs)) return false
            if (note != other.note) return false
            if (help != other.help) return false

            return true
        }

        override fun hashCode(): Int {
            var result = span.hashCode()
            result = 31 * result + errorType.hashCode()
            result = 31 * result + (filePath?.hashCode() ?: 0)
            result = 31 * result + templateArgs.contentHashCode()
            result = 31 * result + (note?.hashCode() ?: 0)
            result = 31 * result + (help?.hashCode() ?: 0)
            return result
        }
    }

    data class ParserError(
        override val span: Span,
        override val errorType: ErrorType,
        val expected: String?,
        val found: String?,
        override val filePath: String? = null,
        override val templateArgs: Array<out Any> = emptyArray(),
        override val note: String? = null,
        override val help: String? = null
    ) : CompilerError(
        span = span,
        errorType = errorType,
        filePath = filePath,
        templateArgs = templateArgs,
        note = note,
        help = help
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ParserError

            if (span != other.span) return false
            if (errorType != other.errorType) return false
            if (expected != other.expected) return false
            if (found != other.found) return false
            if (filePath != other.filePath) return false
            if (!templateArgs.contentEquals(other.templateArgs)) return false
            if (note != other.note) return false
            if (help != other.help) return false

            return true
        }

        override fun hashCode(): Int {
            var result = span.hashCode()
            result = 31 * result + errorType.hashCode()
            result = 31 * result + (expected?.hashCode() ?: 0)
            result = 31 * result + (found?.hashCode() ?: 0)
            result = 31 * result + (filePath?.hashCode() ?: 0)
            result = 31 * result + templateArgs.contentHashCode()
            result = 31 * result + (note?.hashCode() ?: 0)
            result = 31 * result + (help?.hashCode() ?: 0)
            return result
        }
    }
}

