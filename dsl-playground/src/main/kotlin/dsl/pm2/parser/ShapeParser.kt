package dsl.pm2.parser

import dsl.pm2.interpreter.value.Pm2ColorValue
import dsl.pm2.interpreter.value.Pm2PositionValue
import dsl.pm2.interpreter.value.Pm2VertexValue
import graviks2d.util.Color
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream

fun writeShape(vertexSet: Set<Pm2VertexValue>, triangleList: List<Pm2VertexValue>, output: OutputStream) {
    val vertexList = vertexSet.toList()
    val vertexMap = mutableMapOf<Pm2VertexValue, Int>()
    for ((index, vertex) in vertexList.withIndex()) {
        vertexMap[vertex] = index
    }

    val verticesToWrite = vertexList.map { vertexValue ->
        val vertex = vertexValue.toVertex()
        val map = mutableMapOf<String, Any>()
        map["x"] = vertex.x
        map["y"] = vertex.y
        map["color"] = vertex.color.rawValue
        map
    }

    val trianglesToWrite = triangleList.map { vertexValue ->
        val index = vertexMap[vertexValue] ?: throw IllegalArgumentException("Vertex $vertexValue is not in $vertexSet")
        index
    }

    val objectOutput = ObjectOutputStream(output)
    objectOutput.writeObject(verticesToWrite)
    objectOutput.writeObject(trianglesToWrite)
    objectOutput.flush()
}

@Suppress("UNCHECKED_CAST")
fun parseShape(input: InputStream): Pair<Set<Pm2VertexValue>, List<Pm2VertexValue>> {
    val objectInput = ObjectInputStream(input)
    val verticesToWrite = objectInput.readObject() as List<Map<String, Any>>
    val trianglesToWrite = objectInput.readObject() as List<Int>

    val vertexList = verticesToWrite.map { map ->
        val x = map["x"] as Float
        val y = map["y"] as Float
        val rawColor = map["color"]

        val vertex = Pm2VertexValue()
        vertex.setProperty("position", Pm2PositionValue(x, y))
        if (rawColor != null) vertex.setProperty("color", Pm2ColorValue(Color.raw(rawColor as Int)))

        vertex
    }
    val vertexSet = vertexList.toSet()

    val triangleList = trianglesToWrite.map { index -> vertexList[index] }
    return Pair(vertexSet, triangleList)
}
