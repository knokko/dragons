package dragons.geometry.shape

import dragons.geometry.BoundingBox
import dragons.geometry.Distance
import dragons.geometry.Position
import org.joml.Vector3f

sealed class Shape {

    abstract fun createBoundingBox(ownPosition: Position): BoundingBox

    /**
     * Perhaps a raytrace from `rayStart` in the specified `unitDirection` (which must be normalized) with length
     * `rayLength` against this shape when the center of this shape is placed at `ownPosition`. If the ray hits this
     * shape, the distance between `rayStart` and the intersection point will be returned. If not, this method
     * returns `null`.
     */
    abstract fun findRayIntersection(
        ownPosition: Position, rayStart: Position, unitDirection: Vector3f, rayLength: Distance
    ): Distance?
}
