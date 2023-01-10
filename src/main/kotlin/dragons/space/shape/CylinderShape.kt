package dragons.space.shape

import dragons.space.BoundingBox
import dragons.space.Distance
import dragons.space.Position
import org.joml.Vector2f
import org.joml.Vector3f

// TODO Unit test this class
class CylinderShape(val halfHeight: Distance, val radius: Distance): Shape() {

    override fun createBoundingBox(ownPosition: Position) = BoundingBox(
        Position(ownPosition.x - radius, ownPosition.y - halfHeight, ownPosition.z - radius),
        Position(ownPosition.x + radius, ownPosition.y + halfHeight, ownPosition.z + radius)
    )

    override fun findRayIntersection(
        ownPosition: Position, rayStart: Position, unitDirection: Vector3f, rayLength: Distance
    ): Distance? {
        // Note that firstIntersectionDistance <= secondIntersectionDistance
        var (firstIntersectionDistance, secondIntersectionDistance) = determineLineCircleIntersections(
            ownPosition.x, ownPosition.z, this.radius, rayStart.x, rayStart.z, Vector2f(unitDirection.x, unitDirection.z)
        ) ?: return null

        // If the ray goes in the opposite direction or the intersection is too far away, we should return null
        if (secondIntersectionDistance.meters < 0f || firstIntersectionDistance > rayLength) return null

        // If the first intersection distance is negative and the second is positive, the ray starts inside this
        // cylinder (if we ignore the Y coordinate)
        if (firstIntersectionDistance.meters < 0f) firstIntersectionDistance = Distance.meters(0)

        val firstY = rayStart.y + firstIntersectionDistance * unitDirection.y
        val secondY = rayStart.y + secondIntersectionDistance * unitDirection.y

        val ownMinY = ownPosition.y - halfHeight
        val ownMaxY = ownPosition.y + halfHeight

        // If the entire circle intersection happens below this cylinder, there is no real intersection
        if (firstY < ownMinY && secondY < ownMinY) return null

        // If the entire circle intersection happens above this cylinder, there is no real intersection either
        if (firstY > ownMaxY && secondY > ownMaxY) return null

        if (firstY < ownMinY) {
            // In this case, the ray starts below this cylinder and intersects this cylinder at its bottom
            return (ownMinY - rayStart.y) / unitDirection.y
        }

        if (firstY > ownMaxY) {
            // In this case, the ray starts above this cylinder and intersects this cylinder at its top
            return (ownMaxY - rayStart.y) / unitDirection.y
        }

        // If we haven't returned yet, the ray starts inside this cylinder, so we should return 0
        return Distance.meters(0)
    }
}
