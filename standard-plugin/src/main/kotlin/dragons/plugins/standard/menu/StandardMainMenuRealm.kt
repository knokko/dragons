package dragons.plugins.standard.menu

import dragons.plugins.standard.world.entity.SkylandTestEntity
import dragons.plugins.standard.world.tile.DebugPanelTile
import dragons.plugins.standard.world.tile.SkylandTestTile
import dragons.space.Angle
import dragons.space.Position
import dragons.world.realm.InMemoryRealm
import dragons.world.realm.Realm
import java.util.*

fun createStandardMainMenuRealm(): Realm {
    val realm = InMemoryRealm(UUID.randomUUID(), "Test realm 1", true)

    val rng = Random()
    for (counter in 0 until 100) {
        realm.addEntity(SkylandTestEntity(), SkylandTestEntity.State(Position.meters(
            rng.nextInt(2000) - 1000, rng.nextInt(2000) - 1000, rng.nextInt(2000) - 1000)
        ))
    }

    for (terrainIndex in 0 until 300) {
        realm.addTile(
            SkylandTestTile(
                Position.meters(
                    -100 + 20 * ((terrainIndex % 100) / 10),
                    -40 + 40 * (terrainIndex / 100),
                    -100 + 20 * (terrainIndex % 10)
                )
            ), SkylandTestTile.State()
        )
    }

//    realm.addTile(
//        DebugPanelTile(Position.meters(0, -30, -30), Angle.degrees(0f)),
//        DebugPanelTile.State()
//    )
//    realm.addTile(
//        DebugPanelTile(Position.meters(0, -30, 30), Angle.degrees(180f)),
//        DebugPanelTile.State()
//    )
//    realm.addTile(
//        DebugPanelTile(Position.meters(30, -30, 0), Angle.degrees(270f)),
//        DebugPanelTile.State()
//    )
//    realm.addTile(
//        DebugPanelTile(Position.meters(-30, -30, 0), Angle.degrees(90f)),
//        DebugPanelTile.State()
//    )

    return realm
}
