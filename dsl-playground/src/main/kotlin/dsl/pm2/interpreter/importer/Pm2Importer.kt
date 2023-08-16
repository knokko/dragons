package dsl.pm2.interpreter.importer

import dsl.pm2.interpreter.Pm2CompileError
import dsl.pm2.interpreter.Pm2RuntimeError
import dsl.pm2.interpreter.program.Pm2Program
import dsl.pm2.interpreter.value.Pm2Value
import dsl.pm2.interpreter.value.Pm2VertexValue
import dsl.pm2.parser.parseShape

class Pm2Importer(internal val cache: Pm2ImportCache, private val prefix: String) {

    @Throws(Pm2CompileError::class, Pm2RuntimeError::class)
    fun importValue(path: String, isRelative: Boolean): Pm2Value {
        val fullPath = (if (isRelative) prefix + path else path) + ".pv2"
        val cachedValue = cache.values[fullPath]

        if (cachedValue != null) return cachedValue.copy()

        val sourceCode = cache.importFunctions.getSourceCode(fullPath) ?: throw Pm2CompileError("Can't import $fullPath")

        val lastSlash = fullPath.lastIndexOf('/')
        val valuePrefix = if (lastSlash == -1) "" else fullPath.substring(0 until lastSlash)
        val valueImporter = Pm2Importer(cache, valuePrefix)

        val valueProgram = Pm2Program.compile(sourceCode, valueImporter)
        val computedValue = valueProgram.getResultValue()
        cache.values[fullPath] = computedValue
        return computedValue
    }

    @Throws(Pm2CompileError::class)
    fun importModel(path: String, isRelative: Boolean): String {
        val fullPath = (if (isRelative) prefix + path else path) + ".pm2"
        val cachedId = cache.models[fullPath]

        if (cachedId != null) return cachedId.id

        val sourceCode = cache.importFunctions.getSourceCode(fullPath) ?: throw Pm2CompileError("Can't import $fullPath")
        val lastSlash = fullPath.lastIndexOf('/')
        val valuePrefix = if (lastSlash == -1) "" else fullPath.substring(0 until lastSlash)
        val childImporter = Pm2Importer(cache, valuePrefix)

        val childProgram = ChildProgram(fullPath) // The path is a good ID, at least for now

        // IMPORTANT: To avoid endless recursion when two child programs import each other,
        // the compilations must happen AFTER the child program is stored in the cache
        cache.models[fullPath] = childProgram
        childProgram.program = Pm2Program.compile(sourceCode, childImporter, true)

        return childProgram.id
    }

    @Throws(Pm2CompileError::class)
    fun importTriangles(path: String, isRelative: Boolean): Pair<Set<Pm2VertexValue>, List<Pm2VertexValue>> {
        val fullPath = (if (isRelative) prefix + path else path) + ".tri2"
        val cachedTriangles = cache.triangles[fullPath]
        if (cachedTriangles != null) return cachedTriangles

        val input = cache.importFunctions.getBinaryInput(fullPath) ?: throw Pm2CompileError("Can't import $fullPath")
        val importedTriangles = parseShape(input)
        input.close()

        cache.triangles[fullPath] = importedTriangles
        return importedTriangles
    }
}
