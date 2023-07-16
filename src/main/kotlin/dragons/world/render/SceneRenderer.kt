package dragons.world.render

import dragons.geometry.Position
import dragons.world.realm.Realm
import org.joml.Matrix4f
import troll.sync.WaitSemaphore

interface SceneRenderer {
    fun render(realm: Realm, averageEyePosition: Position, leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f)

    fun submit(realm: Realm, waitSemaphores: Array<WaitSemaphore>, signalSemaphores: LongArray)
}
