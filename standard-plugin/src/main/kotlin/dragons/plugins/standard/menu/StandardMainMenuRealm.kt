package dragons.plugins.standard.menu

import dragons.plugins.standard.world.tile.DebugPanelTile
import dragons.plugins.standard.world.tile.SkylandTestTile
import dragons.util.Angle
import dragons.world.chunk.ChunkLocation
import dragons.world.realm.InMemoryRealm
import dragons.world.realm.Realm
import org.joml.Vector3f
import java.util.*

fun createStandardMainMenuRealm(): Realm {
    val realm = InMemoryRealm(UUID.randomUUID(), "Test realm 1", true)

    for (terrainIndex in 0 until 300) {
        realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
            SkylandTestTile(
                Vector3f(
                -10f + 2f * ((terrainIndex % 100) / 10),
                -4f + 4f * (terrainIndex / 100),
                -10f + 2f * (terrainIndex % 10)
            )
            ), SkylandTestTile.State()
        )
    }

    realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
        DebugPanelTile(Vector3f(0f, 5f, -30f), Angle.degrees(0f)),
        DebugPanelTile.State()
    )
    realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
        DebugPanelTile(Vector3f(0f, 5f, 30f), Angle.degrees(180f)),
        DebugPanelTile.State()
    )
    realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
        DebugPanelTile(Vector3f(30f, 5f, 0f), Angle.degrees(270f)),
        DebugPanelTile.State()
    )
    realm.getChunk(ChunkLocation(0, 0, 0)).addTile(
        DebugPanelTile(Vector3f(-30f, 5f, 0f), Angle.degrees(90f)),
        DebugPanelTile.State()
    )

    return realm
}
