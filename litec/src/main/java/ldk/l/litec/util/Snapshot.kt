package ldk.l.litec.util

/**
 * CharStream 的快照，用于回溯
 */
data class Snapshot(
    val currentIndex: Int,
    val currentLine: Int,
    val currentColumn: Int,
    val currentOffset: Int
) {
    companion object {
        // 用于创建“空快照”或默认值
        val EMPTY = Snapshot(0, 1, 1, 0)
    }
}