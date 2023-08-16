package dsl.pm2.interpreter.importer

import java.io.File
import java.io.InputStream
import java.nio.file.Files

interface Pm2ImportFunctions {

    fun getSourceCode(path: String): String?

    fun getBinaryInput(path: String): InputStream?
}

class Pm2DummyImportFunctions: Pm2ImportFunctions {

    override fun getSourceCode(path: String): String? = null

    override fun getBinaryInput(path: String): InputStream? = null
}

class Pm2FileImportFunctions(private val root: File): Pm2ImportFunctions {

    override fun getSourceCode(path: String): String? {
        val file = File("$root/$path")
        return try {
            Files.readString(file.toPath())
        } catch (failed: Throwable) { null }
    }

    override fun getBinaryInput(path: String): InputStream? {
        val file = File("$root/$path")
        return try {
            Files.newInputStream(file.toPath())
        } catch (failed: Throwable) { null }
    }
}
