package dragons.geometry.shape

import dragons.geometry.*
import dragons.geometry.shape.intersection.determineLineLineIntersection
import org.joml.Vector2f
import org.joml.Vector3f

class VerticalPlaneShape(
    val halfWidth: Distance,
    val halfHeight: Distance,
    /**
     * The angle between this plane and the x-axis
     */
    val angle: Angle
): Shape() {
    override fun createBoundingBox(ownPosition: Position): BoundingBox {
        val sizeX = halfWidth * angle.cos
        val sizeZ = halfWidth * angle.sin
        val sizeVector = Vector(sizeX, halfHeight, sizeZ)
        return BoundingBox(ownPosition + sizeVector, ownPosition - sizeVector)
    }

    override fun findRayIntersection(
        ownPosition: Position,
        rayStart: Position,
        unitDirection: Vector3f,
        rayLength: Distance
    ): Distance? {
        val flatIntersectionDistance = determineLineLineIntersection(
            rayStart.x, rayStart.z, Vector2f(unitDirection.x, unitDirection.z),
            ownPosition.x, ownPosition.z, Vector2f(angle.cos, angle.sin)
        )
            // Special case: ray is vertical or parallel to this plane
            // I think returning null is a very reasonable way to handle this case
            ?: return null

        // If the intersection is behind the ray or too far away, return null
        if (flatIntersectionDistance < Distance.ZERO || flatIntersectionDistance > rayLength) return null

        val intersectionPoint = rayStart + flatIntersectionDistance * Vector.meters(unitDirection)

        // If the intersection happens above or below this plane, return null
        if (intersectionPoint.y < ownPosition.y - halfHeight || intersectionPoint.y > ownPosition.y + halfHeight) return null

        val horizontalDistance = ownPosition.distanceTo(Position(intersectionPoint.x, ownPosition.y, intersectionPoint.z))

        // Return null if and only if the intersection happens outside the bounds of this plane
        return if (horizontalDistance <= halfWidth) flatIntersectionDistance else null
    }
}
