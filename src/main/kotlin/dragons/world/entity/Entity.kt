package dragons.world.entity

import java.util.*

/**
 * A game object that is able to move, typically a creature or projectile.
 */
class Entity(
    val id: UUID,
    val properties: EntityProperties,
    val copyState: () -> EntityState,
    val setState: (EntityState) -> Unit
)
