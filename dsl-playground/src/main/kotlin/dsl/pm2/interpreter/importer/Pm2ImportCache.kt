package dsl.pm2.interpreter.importer

import dsl.pm2.interpreter.value.Pm2Value

class Pm2ImportCache(val importFunctions: Pm2ImportFunctions) {

    internal val values = mutableMapOf<String, Pm2Value>()

    internal val models = mutableMapOf<String, ChildProgram>()
}
