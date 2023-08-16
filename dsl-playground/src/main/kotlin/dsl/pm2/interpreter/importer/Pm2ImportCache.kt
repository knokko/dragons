package dsl.pm2.interpreter.importer

import dsl.pm2.interpreter.value.Pm2Value
import dsl.pm2.interpreter.value.Pm2VertexValue

class Pm2ImportCache(val importFunctions: Pm2ImportFunctions) {

    internal val values = mutableMapOf<String, Pm2Value>()

    internal val models = mutableMapOf<String, ChildProgram>()

    internal val triangles = mutableMapOf<String, Pair<Set<Pm2VertexValue>, List<Pm2VertexValue>>>()
}
