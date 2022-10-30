package dragons.world.entity

import dragons.space.Position

abstract class EntityState(
    var position: Position
) {
    override fun toString() = position.toString()
}
