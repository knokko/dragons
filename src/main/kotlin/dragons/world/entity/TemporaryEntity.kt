package dragons.world.entity

import java.util.*

internal class TemporaryEntity(
    val id: UUID,
    val properties: EntityProperties,
    var state: EntityState
) {
    override fun toString() = "TempEntity(props=$properties, state=$state)"
}
