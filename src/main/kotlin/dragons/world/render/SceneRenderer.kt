package dragons.world.render

import com.github.knokko.boiler.sync.WaitSemaphore
import dragons.geometry.Position
import dragons.world.realm.Realm
import org.joml.Matrix4f

interface SceneRenderer {
    fun render(realm: Realm, averageEyePosition: Position, leftCameraMatrix: Matrix4f, rightCameraMatrix: Matrix4f)

    fun submit(realm: Realm, waitSemaphores: Array<WaitSemaphore>, signalSemaphores: LongArray)
}
