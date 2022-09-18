package dragons.plugins.standard.menu

import dragons.plugins.standard.world.tile.DebugPanelTile
import dragons.plugins.standard.world.tile.SkylandTestTile
import dragons.space.Angle
import dragons.space.Position
import dragons.world.chunk.ChunkLocation
import dragons.world.realm.InMemoryRealm
import dragons.world.realm.Realm
import java.util.*

fun createStandardMainMenuRealm(): Realm {
    val realm = InMemoryRealm(UUID.randomUUID(), "Test realm 1", true)

    for (terrainIndex in 0 until 300) {
        realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
            SkylandTestTile(
                Position.meters(
                    -100 + 20 * ((terrainIndex % 100) / 10),
                    -40 + 40 * (terrainIndex / 100),
                    -100 + 20 * (terrainIndex % 10)
                )
            ), SkylandTestTile.State()
        )
    }

    realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
        DebugPanelTile(Position.meters(0, 5, -30), Angle.degrees(0f)),
        DebugPanelTile.State()
    )
    realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
        DebugPanelTile(Position.meters(0, 5, 30), Angle.degrees(180f)),
        DebugPanelTile.State()
    )
    realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
        DebugPanelTile(Position.meters(30, 5, 0), Angle.degrees(270f)),
        DebugPanelTile.State()
    )
    realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
        DebugPanelTile(Position.meters(-30, 5, 0), Angle.degrees(90f)),
        DebugPanelTile.State()
    )

    return realm
}
