package ldk.l.litec.util

import java.nio.CharBuffer
import java.nio.charset.StandardCharsets

class CharStream(text: String) {
    // 原始文本内容
    val originalText: String = text

    // 处理后的字符序列（每个元素是一个独立的字符或Emoji单元）
    private val chars: List<String>

    // 初始化处理文本
    init {
        chars = processText(text)
    }

    /**
     * 处理文本并构建精确索引
     */
    private fun processText(text: String): List<String> {
        val charsList = mutableListOf<String>()
        val buffer = CharBuffer.wrap(text)

        while (buffer.hasRemaining()) {
            val char = buffer.get()
            val codePoint = if (Character.isHighSurrogate(char) && buffer.hasRemaining()) {
                // 处理代理对（surrogate pair）
                val nextChar = buffer.get(buffer.position())
                if (Character.isLowSurrogate(nextChar)) {
                    val cp = Character.toCodePoint(char, nextChar)
                    buffer.get() // 消耗低代理字符
                    cp
                } else {
                    char.code
                }
            } else {
                char.code
            }

            if (isEmoji(codePoint)) {
                // 处理组合 Emoji
                val emoji = buildString {
                    appendCodePoint(codePoint)
                    while (buffer.hasRemaining() && isEmojiComponent(buffer.get(buffer.position()))) {
                        append(buffer.get())
                    }
                }
                charsList.add(emoji)
            } else {
                // 普通字符（包括中文）
                charsList.add(String(Character.toChars(codePoint)))
            }
        }

        return charsList
    }

    /**
     * 索引操作符重载：精确获取指定位置的字符或Emoji单元
     */
    operator fun get(index: Int): String {
        if (index < 0 || index >= chars.size) return ""
        return chars[index]
    }

    /**
     * 获取字符数量（每个Emoji单元计为一个字符）
     */
    fun length(): Int = chars.size

    /**
     * 转换为字符串
     */
    override fun toString(): String = originalText

    /**
     * 子字符串提取
     */
    fun substring(startIndex: Int, endIndex: Int = length()): String {
        if (startIndex < 0 || endIndex > length() || startIndex > endIndex) return ""
        return chars.subList(startIndex, endIndex).joinToString("")
    }

    /**
     * 增强的 trim 函数
     */
    fun trim(): CharStream {
        return CharStream(trimInternal(originalText))
    }

    private fun trimInternal(text: String): String {
        if (text.isEmpty()) return text

        val whitespace = setOf(
            ' ', '\t', '\n', '\r',
            '\u200B', '\uFEFF', '\u3000'
        )

        var start = 0
        var end = text.length - 1

        while (start <= end && text[start] in whitespace) start++
        while (end >= start && text[end] in whitespace) {
            if (end > 0 && isEmoji(text[end - 1].code)) break
            end--
        }

        return if (start > end) "" else text.substring(start, end + 1)
    }

    /**
     * 判断是否是Emoji
     */
    private fun isEmoji(codePoint: Int): Boolean {
        return when (codePoint) {
            in 0x1F600..0x1F64F,  // 表情符号
            in 0x1F300..0x1F5FF,  // 符号与Pictographs
            in 0x1F680..0x1F6FF,  // 交通与地图
            in 0x2600..0x26FF,    // 杂项符号
            in 0x2700..0x27BF,    // 装饰符号
            in 0x1F900..0x1F9FF,  // 补充符号和图形
            in 0x1FA70..0x1FAFF   // 符号和图形扩展-A
                -> true
            else -> false
        }
    }

    /**
     * 判断是否是Emoji组件（包括零宽连接符）
     */
    private fun isEmojiComponent(char: Char): Boolean {
        return when (char.code) {
            0x200D, // 零宽连接符 (ZWJ)
            0xFE0F, // 变体选择器-16 (VS-16)
            in 0x1F3FB..0x1F3FF, // 肤色修饰符
            in 0x1F1E6..0x1F1FF  // 区域指示符号（国旗组件）
                -> true
            else -> false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CharStream) {
            throw RuntimeException("输入不是CharStream")
        }
        return other.originalText == originalText
    }

    override fun hashCode(): Int {
        var result = originalText.hashCode()
        result = 31 * result + chars.hashCode()
        return result
    }
}