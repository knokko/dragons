package dsl.pm2.interpreter.importer

import java.io.File
import java.nio.file.Files

interface Pm2ImportFunctions {

    fun getSourceCode(path: String): String?
}

class Pm2DummyImportFunctions: Pm2ImportFunctions {

    override fun getSourceCode(path: String): String? = null
}

class Pm2FileImportFunctions(private val root: File): Pm2ImportFunctions {

    override fun getSourceCode(path: String): String? {
        val file = File("$root/$path")
        return try {
            Files.readString(file.toPath())
        } catch (failed: Throwable) {
            null
        }
    }
}
