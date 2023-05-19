package dsl.pm2.interpreter

import graviks2d.util.Color

class Pm2Vertex(
    val x: Float,
    val y: Float,
    val color: Color
) {
    override fun toString() = "Vertex($x, $y, $color)"
}
