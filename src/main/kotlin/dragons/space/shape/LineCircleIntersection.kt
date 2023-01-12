package dragons.space.shape

import dragons.space.Distance
import org.joml.Math.sqrt
import org.joml.Vector2f

fun determineLineCircleIntersections(
    centerX: Distance, centerY: Distance, radius: Distance, lineX: Distance, lineY: Distance, direction: Vector2f
): Pair<Distance, Distance>? {

    // If direction ~= 0, we simply check whether the line start is inside the circle
    if (direction.length() < 0.0001f) {
        val dx = (centerX - lineX).meters
        val dy = (centerY - lineY).meters
        return if (dx * dx + dy * dy <= radius.meters * radius.meters) {
            // Returning (0, 100k) is kinda dirty, but useful in most cases
            Pair(Distance.meters(0), Distance.kiloMeters(100))
        } else null
    }

    val perpendicularDirection = Vector2f(direction).perpendicular()

    /*
     * First, we need to find the closest point on the line to the center of the circle.
     *
     * Mathematics:
     * ld = (line)direction, pd = perpendicularDirection, c = center, l = lineStart, r = radius,
     * a * |pd| = distanceFromCenterToClosestPoint, b * |ld| = distanceFromLineStartToClosestPoint
     *
     * Closest point = c + a * pd = l + b * ld | with a and b unknown
     * (1) c.x + a * pd.x = l.x + b * ld.x
     *     a * pd.x = l.x - c.x + b * ld.x
     *     a = (l.x - c.x + b * ld.x) / pd.x = (l.y - c.y + b * ld.y) / pd.y
     *
     * (2) (l.x - c.x + b * ld.x) / pd.x = (l.y - c.y + b * ld.y) / pd.y
     *     (l.x - c.x + b * ld.x) * pd.y = (l.y - c.y + b * ld.y) * pd.x
     *     b * ld.x * pd.y + (l.x - c.x) * pd.y = b * ld.y * pd.x + (l.y - c.y) * pd.x
     *     b * (ld.x * pd.y - ld.y * pd.x) = (l.y - c.y) * pd.x - (l.x - c.x) * pd.y
     *     b = ((l.y - c.y) * pd.x - (l.x - c.x) * pd.y)) / (ld.x * pd.y - ld.y * pd.x)
     */
    val b = ((lineY - centerY) * perpendicularDirection.x - (lineX - centerX) * perpendicularDirection.y) /
            (direction.x * perpendicularDirection.y - direction.y * perpendicularDirection.x)
    val distanceFromLineStartToClosestPoint = b * direction.length()
    val closestPointX = lineX + b * direction.x
    val closestPointY = lineY + b * direction.y

    val distanceFromCenterToClosestPointSq = run {
        val dx = (centerX - closestPointX).meters
        val dy = (centerY - closestPointY).meters
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

    val radiusSq = radius.meters * radius.meters
    val distanceFromCircleOutlineToClosestPointSq = radiusSq - distanceFromCenterToClosestPointSq
    if (distanceFromCircleOutlineToClosestPointSq < 0f) return null

    val distanceFromCircleOutlineToClosestPoint = Distance.meters(sqrt(distanceFromCircleOutlineToClosestPointSq))
    return Pair(
        (distanceFromLineStartToClosestPoint - distanceFromCircleOutlineToClosestPoint) / direction.length(),
        (distanceFromLineStartToClosestPoint + distanceFromCircleOutlineToClosestPoint) / direction.length()
    )
}
