package dsl.pm2.ui

import org.joml.Vector2f

private fun sign(p1: Vector2f, p2: Vector2f, p3: Vector2f): Float {
    return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
}

internal fun isPointInsideTriangle(pt: Vector2f, v1: Vector2f, v2: Vector2f, v3: Vector2f): Boolean {
    val d1 = sign(pt, v1, v2)
    val d2 = sign(pt, v2, v3)
    val d3 = sign(pt, v3, v1)
    val hasNeg = d1 < 0 || d2 < 0 || d3 < 0
    val hasPos = d1 > 0 || d2 > 0 || d3 > 0
    return !(hasNeg && hasPos)
}
