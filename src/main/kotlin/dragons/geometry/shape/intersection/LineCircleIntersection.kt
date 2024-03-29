package dragons.geometry.shape.intersection

import dragons.geometry.Area
import dragons.geometry.Coordinate
import dragons.geometry.Distance
import org.joml.Vector2f

/**
 * Determines whether and where a given line intersects a given circle. The center of the circle is at
 * `(centerX, centerY)` and its radius is `radius`. The line goes through `(lineX, lineY)` and has direction
 * `direction`.
 *
 * If the line intersects the circle, this method returns a pair (a, b) such that a <= b and the line intersects
 * the circle at `(lineX, lineY) + a * direction` and at `(lineX, lineY) + b * direction`.
 *
 * If the line does **not** intersect the circle, this method returns `null`.
 *
 * ### Special cases
 * If the line touches the circle, floating point rounding errors will determine whether it returns `null` or a pair
 * `(a, b)` with a ~= b.
 *
 * If `direction.length() ~= 0` and `(lineX, lineY)` is outside the circle, this method returns `null`.
 *
 * If `direction.length() ~= 0` and `(lineX, lineY)` is inside the circle, this method returns `(-100 kilometers, 100k)`.
 * (This behavior makes some sense when you consider 100k to be 'infinity': if direction is (almost) 0, the 'line'
 * would basically intersect the circle at `(lineX, lineY) +- infinity * direction`. I think this behavior is the
 * most practical, and is very helpful for the ray-cylinder intersection test. If you don't like this behavior, then
 * don't use the zero vector as direction.)
 */
fun determineLineCircleIntersections(
    centerX: Coordinate, centerY: Coordinate, radius: Distance, lineX: Coordinate, lineY: Coordinate, direction: Vector2f
): Pair<Distance, Distance>? {
    val perpendicularDirection = Vector2f(direction).perpendicular()
    val lineDirectionScale = determineLineLineIntersection(lineX, lineY, direction, centerX, centerY, perpendicularDirection)

    // lineDirectionScale will be null if and only if direction.length() ~= 0
    // to handle this case, simple check whether the line starts inside the circle
    if (lineDirectionScale == null) {
        val dx = (centerX - lineX).meters
        val dy = (centerY - lineY).meters
        return if (dx * dx + dy * dy <= radius.meters * radius.meters) {
            // Returning (-100k, 100k) is kinda dirty, but useful in most cases
            Pair(Distance.kiloMeters(-100), Distance.kiloMeters(100))
        } else null
    }

    val distanceFromLineStartToClosestPoint = lineDirectionScale * direction.length()
    val closestPointX = lineX + lineDirectionScale * direction.x
    val closestPointY = lineY + lineDirectionScale * direction.y

    val distanceFromCenterToClosestPointSq = run {
        val dx = centerX - closestPointX
        val dy = centerY - closestPointY
        dx * dx + dy * dy
    }

    /*
     * Now that the closest point on the line to the center has been found, we can compute the actual intersection
     * points.
     *
     * Mathematics:
     * a = distanceFromCenterToClosestPoint, b = distanceFromLineStartToClosestPoint, r = radius,
     * c = distanceFromCircleOutlineToClosestPoint
     *
     * In the triangle (lineStart, center, closestPoint), the angle at closestPoint is 90 degrees since
     * pd is orthogonal to ld (by definition). Since any intersection point must be located on the line through
     * lineStart and closestPoint, the angle at closestPoint in any triangle (closestPoint, center, intersectionPoint)
     * must also be 90 degrees. This makes it possible to use Pythagoras to compute the distance from the closest
     * point to the intersection points: a^2 + c^2 = r^2 -> c^2 = r^2 - a^2
     *
     * When r^2 > a^2, the closest point is inside the circle, and the distance between the line start and the
     * intersection points are b - c and b + c. When r^2 < a^2, the closest point is outside the circle, so there is
     * no intersection. In this case we should return null. When r^2 ~= a^2, the line touches the circle. In this
     * case, floating point rounding errors will determine whether this is considered an intersection (possibly with
     * b - c = b + c) or a miss.
     */

    val radiusSq = radius * radius
    val distanceFromCircleOutlineToClosestPointSq = radiusSq - distanceFromCenterToClosestPointSq
    if (distanceFromCircleOutlineToClosestPointSq < Area.squareMeters(0)) return null

    val distanceFromCircleOutlineToClosestPoint = distanceFromCircleOutlineToClosestPointSq.squareRoot()
    return Pair(
        (distanceFromLineStartToClosestPoint - distanceFromCircleOutlineToClosestPoint) / direction.length(),
        (distanceFromLineStartToClosestPoint + distanceFromCircleOutlineToClosestPoint) / direction.length()
    )
}
