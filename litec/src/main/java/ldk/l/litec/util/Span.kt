package ldk.l.litec.util

data class Span(
    val start: Int,
    val end: Int
) {
    constructor(position: Position) : this(
        position.offset,
        position.offset
    )
    constructor(start: Position,end: Position) : this(
        start.offset,
        end.offset
    )

    fun extendTo(other: Span): Span {
        return Span(
            this.start,other.end
        )
    }
}