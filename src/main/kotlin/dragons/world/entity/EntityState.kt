package dragons.world.entity

import dragons.geometry.Position

abstract class EntityState(
    var position: Position
) {
    override fun toString() = position.toString()
}
