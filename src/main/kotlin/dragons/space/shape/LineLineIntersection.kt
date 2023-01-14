package dragons.space.shape

import dragons.space.Distance
import org.joml.Vector2f
import kotlin.math.absoluteValue

/**
 * Determines whether and where the line through `(x1, y1)` with direction `direction1` intersects the line through
 * `(x2, y2)` with direction `direction2`.
 *
 * If the lines intersect, this method returns the distance `a` such that the intersection point is located at
 * `(x1, y1) + a * direction1`.
 *
 * If the lines don't intersect, this method returns `null`. This happens when the lines are parallel to each other
 * or at least one of the directions is (almost) 0. Note: this method also returns `null` when the lines overlap
 * completely.
 */
fun determineLineLineIntersection(
    x1: Distance, y1: Distance, direction1: Vector2f,
    x2: Distance, y2: Distance, direction2: Vector2f
): Distance? {
    /*
     * Mathematics:
     * Assume that intersection point is at (x1, y1) + b * direction1 = (x2, y2) + a * direction2
     * where a and b are unknown real numbers
     *
     * Abbreviations: d1 = direction1 and d2 = direction2
     *
     * (1) x2 + a * d2.x = x1 + b * d1.x
     *     a * d2.x = x1 - x2 + b * d1.x
     *     a = (x1 - x2 + b * d1.x) / d2.x = (y1 - y2 + b * d1.y) / d2.y
     *
     * (2) (x1 - x2 + b * d1.x) / d2.x = (y1 - y2 + b * d1.y) / d2.y
     *     (x1 - x2 + b * d1.x) * d2.y = (y1 - y2 + b * d1.y) * d2.x
     *     b * d1.x * d2.y + (x1 - x2) * d2.y = b * d1.y * d2.x + (y1 - y2) * d2.x
     *     b * (d1.x * d2.y - d1.y * d2.x) = (y1 - y2) * d2.x - (x1 - x2) * d2.y
     *     b = ((y1 - y2) * d2.x - (x1 - x2) * d2.y)) / (d1.x * d2.y - d1.y * d2.x)
     */
    val denominator = direction1.x * direction2.y - direction1.y * direction2.x

    // The denominator can be zero if either direction is (almost) (0, 0), or if the lines are parallel
    if (denominator.absoluteValue < 0.0001f) return null

    return ((y1 - y2) * direction2.x - (x1 - x2) * direction2.y) / denominator
}
