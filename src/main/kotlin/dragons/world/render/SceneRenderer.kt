package dragons.world.render

import dragons.space.Position
import dragons.world.realm.Realm
import org.joml.Matrix4f

interface SceneRenderer {
    fun render(realm: Realm, averageEyePosition: Position, leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f)

    fun submit(realm: Realm, waitSemaphores: LongArray, waitStageMasks: IntArray, signalSemaphores: LongArray)
}
