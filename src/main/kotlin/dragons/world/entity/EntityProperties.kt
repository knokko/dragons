package dragons.world.entity

import dragons.geometry.shape.Shape

/**
 * Represents immutable properties of an `Entity`
 */
abstract class EntityProperties {
    /**
     * For persistent entities, this should return a string that uniquely defines this class and is *not* subject
     * to change. (Using the full name of the class is discouraged because it changes whenever you reorganize
     * packages.)
     *
     * For non-persistent entities, this should return null.
     */
    abstract fun getPersistentClassID(): String?

    abstract fun getShape(state: EntityState): Shape

    override fun toString() = "EntityProperties"
}
