// src/main/java/ldk/l/litec/util/LexerCharStream.kt

package ldk.l.litec.util

import com.vdurmont.emoji.EmojiManager

fun String.isDigit(): Boolean = this.length == 1 && this[0].isDigit()
fun String.isLetter(): Boolean = this.length == 1 && this[0].isLetter()
fun String.isLetterOrDigit(): Boolean = this.length == 1 && this[0].isLetterOrDigit()

/**
 * 表示一个字符单元及其在原始文本中的起始偏移量
 */
data class CharUnit(
    val text: String,
    val startOffset: Int // 在原始字符串中的起始索引
)

/**
 * 专为词法分析器设计的字符流
 * 支持 Emoji、代理对、精确行/列追踪
 */
class CharStream(private val text: String) {
    // 处理后的字符单元列表（包含文本和原始偏移量）
    private val chars: List<CharUnit> = processTextWithOffset(text)

    // 当前读取索引（在 chars 列表中的位置）
    private var currentIndex = 0

    val position: Position
        get() = getCurrentPosition()

    /**
     * 处理文本为 CharUnit 列表，记录每个单元的起始偏移量
     */
    private fun processTextWithOffset(text: String): List<CharUnit> {
        val units = mutableListOf<CharUnit>()
        var offset = 0

        while (offset < text.length) {
            val c = text[offset]

            when {
                // 处理代理对
                Character.isHighSurrogate(c) && offset + 1 < text.length -> {
                    val next = text[offset + 1]
                    if (Character.isLowSurrogate(next)) {
                        val codePoint = Character.toCodePoint(c, next)
                        if (isEmoji(codePoint)) {
                            // Emoji 序列（含修饰符）
                            val emojiBuilder = StringBuilder()
                            emojiBuilder.append(c).append(next)
                            val start = offset
                            offset += 2
                            while (offset < text.length && isEmojiComponent(text[offset])) {
                                emojiBuilder.append(text[offset])
                                offset++
                            }
                            units.add(CharUnit(emojiBuilder.toString(), start))
                            continue
                        } else {
                            units.add(CharUnit("$c$next", offset))
                            offset += 2
                            continue
                        }
                    }
                }

                // 处理单字符 Emoji
                isEmoji(c.code) -> {
                    val emojiBuilder = StringBuilder()
                    emojiBuilder.append(c)
                    val start = offset
                    offset++
                    while (offset < text.length && isEmojiComponent(text[offset])) {
                        emojiBuilder.append(text[offset])
                        offset++
                    }
                    units.add(CharUnit(emojiBuilder.toString(), start))
                    continue
                }
            }

            // 普通字符
            units.add(CharUnit(c.toString(), offset))
            offset++
        }

        return units
    }

    /**
     * 获取当前字符单元（不消费）
     */
    fun peek(): String = if (isAtEnd()) "" else chars[currentIndex].text

    fun peek(n: Int): String {
        if (currentIndex + n >= chars.size) return ""
        return chars[currentIndex + n].text
    }

    fun isAtEnd(): Boolean = currentIndex >= chars.size

    private fun calculatePositionAtEnd(): Position {
        if (text.isEmpty()) return Position(1, 1, 0)

        var line = 1
        var column = 1

        for (i in text.indices) {
            when (text[i]) {
                '\n' -> { line++; column = 1 }
                '\r' -> {
                    line++
                    column = 1
                    if (i + 1 < text.length && text[i + 1] == '\n') continue
                }
                else -> column++
            }
        }

        return Position(line, column, text.length)
    }

    fun getPositionFromOffset(offset: Int): Position {
        if (offset <= 0) return Position(1, 1, 0)
        if (offset >= text.length) {
            return calculatePositionAtEnd()
        }

        var line = 1
        var column = 1
        var i = 0

        while (i < offset) {
            when (val c = text[i]) {
                '\n' -> {
                    line++
                    column = 1
                }
                '\r' -> {
                    line++
                    column = 1
                    if (i + 1 < text.length && text[i + 1] == '\n') {
                        i++ // 跳过 \n
                    }
                }
                else -> column++
            }
            i++ // 只加一次
        }

        return Position(line, column, offset)
    }

    fun getLastPosition(): Position {
        return getPositionFromOffset(text.length)
    }

    // --- Emoji 判断工具 ---
    private fun isEmoji(codePoint: Int): Boolean = EmojiManager.isEmoji(codePoint.toString())

    private fun isEmojiComponent(char: Char): Boolean = when (char.code) {
        0x200D, 0xFE0F -> true
        in 0x1F3FB..0x1F3FF, in 0x1F1E6..0x1F1FF -> true
        else -> false
    }

    val length: Int get() = chars.size

    override fun toString(): String {
        return "LexerCharStream(text=$text,pos=$currentIndex, total=${chars.size})"
    }

    // ✅ 新增：缓存当前行/列信息，避免重复计算
    private var currentLine = 1
    private var currentColumn = 1
    private var currentOffset = 0  // 原始文本中的偏移量

    init {
        // 初始化时同步位置
        syncPosition()
    }

    /**
     * 同步内部行/列/偏移量到 currentIndex
     */
    private fun syncPosition() {
        if (currentIndex >= chars.size) {
            val last = getLastPosition()
            currentLine = last.line
            currentColumn = last.column
            currentOffset = text.length
        } else {
            val unit = chars[currentIndex]
            val pos = getPositionFromOffset(unit.startOffset)
            currentLine = pos.line
            currentColumn = pos.column
            currentOffset = unit.startOffset
        }
    }

    /**
     * 🔹 创建当前状态的快照
     */
    fun checkpoint(): Snapshot {
        return Snapshot(
            currentIndex = currentIndex,
            currentLine = currentLine,
            currentColumn = currentColumn,
            currentOffset = currentOffset
        )
    }

    /**
     * 🔁 恢复到指定快照
     */
    fun restore(snapshot: Snapshot) {
        this.currentIndex = snapshot.currentIndex
        this.currentLine = snapshot.currentLine
        this.currentColumn = snapshot.currentColumn
        this.currentOffset = snapshot.currentOffset
    }

    fun next(): String {
        if (isAtEnd()) return ""

        val currentUnit = chars[currentIndex]
        val text = currentUnit.text
        val startOffset = currentUnit.startOffset

        // ✅ 先记录当前位置（用于当前 token 的开始）
        // 但 position 属性仍用于“当前读取位置”
        currentIndex++

        // ✅ 更新内部状态到下一个字符
        if (currentIndex < chars.size) {
            val nextUnit = chars[currentIndex]
            val pos = getPositionFromOffset(nextUnit.startOffset)
            currentLine = pos.line
            currentColumn = pos.column
            currentOffset = nextUnit.startOffset
        } else {
            // 到达末尾，设置为文本末尾
            val lastPos = getLastPosition()
            currentLine = lastPos.line
            currentColumn = lastPos.column
            currentOffset = text.length
        }

        return text
    }

    // ✅ 重写 back()，也更新位置
    fun back() {
        if (currentIndex > 0) {
            currentIndex--
            val unit = chars[currentIndex]
            val pos = getPositionFromOffset(unit.startOffset)
            currentLine = pos.line
            currentColumn = pos.column
            currentOffset = unit.startOffset
        }
    }

    // ✅ 重写 getCurrentPosition()，使用缓存值
    fun getCurrentPosition(): Position {
        if (isAtEnd()) {
            return getLastPosition()
        }
        return Position(currentLine, currentColumn, currentOffset)
    }

    // ✅ 重写 getCurrentEndOffset()
    fun getCurrentEndOffset(): Int {
        if (isAtEnd() || chars.isEmpty()) return text.length
        val unit = chars[currentIndex]
        return unit.startOffset + unit.text.length
    }

    override operator fun equals(other: Any?): Boolean = when (other) {
        is CharStream -> other.text == this.text
        else -> false
    }

    override fun hashCode(): Int {
        var result = currentIndex
        result = 31 * result + text.hashCode()
        result = 31 * result + chars.hashCode()
        result = 31 * result + length
        result = 31 * result + position.hashCode()
        return result
    }
}