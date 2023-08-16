package dsl.pm2.ui

import dsl.pm2.interpreter.value.Pm2PositionValue
import dsl.pm2.interpreter.value.Pm2VertexValue
import graviks2d.target.GraviksTarget
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.RectangularDrawnRegion
import gruviks.component.RenderResult
import gruviks.event.*
import gruviks.feedback.RenderFeedback
import gruviks.feedback.RequestKeyboardFocusFeedback
import org.joml.Matrix3x2f
import org.joml.Vector2f
import org.lwjgl.glfw.GLFW.*
import kotlin.math.absoluteValue

class Pm2ShapeEditor(
    val points: MutableSet<Pm2VertexValue>,
    val triangles: MutableList<Pm2VertexValue>
): Component() {

    private val cameraMatrix = Matrix3x2f().scale(0.5f).translate(1f, 1f)

    override fun subscribeToEvents() {
        agent.subscribe(CursorClickEvent::class)
        agent.subscribe(KeyPressEvent::class)
        agent.subscribe(CursorScrollEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is CursorClickEvent) {
            agent.giveFeedback(RequestKeyboardFocusFeedback())
            val inverse = cameraMatrix.invert(Matrix3x2f())
            val clickedPosition = inverse.transformPosition(Vector2f(event.position.x, event.position.y))

            if (event.button == 0) {
                val newVertex = Pm2VertexValue()
                newVertex.setProperty("position", Pm2PositionValue(clickedPosition.x, clickedPosition.y))
                points.add(newVertex)
                agent.giveFeedback(RenderFeedback())
            } else {
                val maxDistance = 0.02f
                var minDistance = maxDistance
                var closestVertex: Pm2VertexValue? = null
                for (vertex in points) {
                    val fullVertex = vertex.toVertex()
                    val distance = clickedPosition.distance(Vector2f(fullVertex.x, fullVertex.y))
                    if (distance < minDistance) {
                        minDistance = distance
                        closestVertex = vertex
                    }
                }

                if (event.button == 1) {
                    if (closestVertex != null && !triangles.contains(closestVertex) ){
                        points.remove(closestVertex)
                    } else {
                        for (index in 0 until triangles.size step 3) {
                            val vertexPositions = triangles.subList(index, index + 3).map { vertexValue ->
                                val vertex = vertexValue.toVertex()
                                Vector2f(vertex.x, vertex.y)
                            }
                            if (isPointInsideTriangle(clickedPosition, vertexPositions[0], vertexPositions[1], vertexPositions[2])) {
                                for (counter in 0 until 3) triangles.removeAt(index)
                                break
                            }
                        }
                    }
                    agent.giveFeedback(RenderFeedback())
                }

                if (event.button == 2 && closestVertex != null) {
                    val selectedVertices = getSelectedVertices()
                    if (!selectedVertices.contains(closestVertex)) {
                        triangles.add(closestVertex)
                        agent.giveFeedback(RenderFeedback())
                    }
                }
            }
        }
        if (event is KeyPressEvent) {
            if (event.key.type == KeyType.Left) cameraMatrix.translate(0.1f, 0f)
            if (event.key.type == KeyType.Right) cameraMatrix.translate(-0.1f, 0f)
            if (event.key.type == KeyType.Down) cameraMatrix.translate(0f, 0.1f)
            if (event.key.type == KeyType.Up) cameraMatrix.translate(0f, -0.1f)
            if (event.key.type == KeyType.Escape) while (triangles.size % 3 != 0) triangles.removeLast()

            fun translate(dx: Float, dy: Float) {
                val scale = 0.1f
                for (vertex in points) {
                    val oldPosition = vertex.getProperty("position").castTo<Pm2PositionValue>()
                    val newPosition = Pm2PositionValue(
                        oldPosition.getProperty("x").floatValue() + dx * scale,
                        oldPosition.getProperty("y").floatValue() + dy * scale
                    )
                    vertex.setProperty("position", newPosition)
                }
            }

            if (event.key.code == GLFW_KEY_A) translate(-1f, 0f)
            if (event.key.code == GLFW_KEY_D) translate(1f, 0f)
            if (event.key.code == GLFW_KEY_W) translate(0f, 1f)
            if (event.key.code == GLFW_KEY_S) translate(0f, -1f)

            agent.giveFeedback(RenderFeedback())
        }
        if (event is CursorScrollEvent) {
            if (event.direction == ScrollDirection.Horizontal) cameraMatrix.translateLocal(-event.amount, 0f)
            if (event.direction == ScrollDirection.Vertical) cameraMatrix.translateLocal(0f, event.amount)
            if (event.direction == ScrollDirection.Zoom) {
                val scale = if (event.amount >= 0f) 1f / (1f + event.amount) else 1f - event.amount
                val cursorState = agent.cursorTracker.getCursorState(event.cursor)

                if (cursorState != null) {
                    cameraMatrix.scaleAroundLocal(
                        scale,
                        2f * cursorState.localPosition.x - 1f,
                        -2f * cursorState.localPosition.y + 1f
                    )
                }
            }
            agent.giveFeedback(RenderFeedback())
        }
    }

    private fun getSelectedVertices(): Set<Pm2VertexValue> {
        return if (triangles.size % 3 == 0) emptySet()
        else triangles.subList(3 * (triangles.size / 3), triangles.size).toSet()
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        target.fillRect(0f, 0f, 1f, 1f, Color.BLUE)

        val aspectRatio = target.getAspectRatio()
        val cameraRatio = cameraMatrix.m11 / cameraMatrix.m00

        if ((aspectRatio - cameraRatio).absoluteValue > 0.001f) {
            cameraMatrix.scale(cameraRatio / aspectRatio, 1f)
        }

        fun getPositionVector(vertex: Pm2VertexValue): Vector2f {
            val position = vertex.getProperty("position").castTo<Pm2PositionValue>()
            return Vector2f(position.getProperty("x").floatValue(), position.getProperty("y").floatValue())
        }

        val selectedVertices = getSelectedVertices()
        for (vertex in points) {
            val positionVector = getPositionVector(vertex)
            val size = 0.01f
            val position1 = cameraMatrix.transformPosition(positionVector.add(-size, -size, Vector2f()))
            val position2 = cameraMatrix.transformPosition(positionVector.add(size, size, Vector2f()))

            val color = if (selectedVertices.contains(vertex)) Color.GREEN else Color.BLACK
            target.fillRect(position1.x, position1.y, position2.x, position2.y, color)
        }

        for (triangle in triangles.chunked(3)) {
            if (triangle.size == 3) {
                val positions = triangle.map { vertex -> cameraMatrix.transformPosition(getPositionVector(vertex)) }
                target.fillTriangle(
                    positions[0].x, positions[0].y,
                    positions[1].x, positions[1].y,
                    positions[2].x, positions[2].y,
                    Color.RED
                )
            }
        }

        val lineWidth = 0.01f
        val originMinus = cameraMatrix.transformPosition(Vector2f(-lineWidth))
        val originPlus = cameraMatrix.transformPosition(Vector2f(lineWidth))
        target.fillRect(originMinus.x, 0f, originPlus.x, 1f, Color.BLACK)
        target.fillRect(0f, originMinus.y, 1f, originPlus.y, Color.BLACK)

        for (d in -10 .. 10) {
            run {
                val minus = cameraMatrix.transformPosition(Vector2f(d.toFloat() - lineWidth * 0.5f))
                val plus = cameraMatrix.transformPosition(Vector2f(d.toFloat() + lineWidth * 0.5f))
                target.fillRect(minus.x, 0f, plus.x, 1f, Color.BLACK)
                target.fillRect(0f, minus.y, 1f, plus.y, Color.BLACK)
            }

            for (i in 1 until 10) {
                val sum = d.toFloat() + i.toFloat() * 0.1f
                val minus = cameraMatrix.transformPosition(Vector2f(sum - lineWidth * 0.2f))
                val plus = cameraMatrix.transformPosition(Vector2f(sum + lineWidth * 0.2f))
                target.fillRect(minus.x, 0f, plus.x, 1f, Color.BLACK)
                target.fillRect(0f, minus.y, 1f, plus.y, Color.BLACK)
            }
        }

        return RenderResult(drawnRegion = RectangularDrawnRegion(0f, 0f, 1f, 1f), propagateMissedCursorEvents = false)
    }
}
