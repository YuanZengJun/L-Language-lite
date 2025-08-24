package ldk.l.litec.util

data class Position(
    val line: Int,
    val column: Int,
    val offset: Int,
) {
    override fun toString() = "$line:$column"
}