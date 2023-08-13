package dsl.pm2.interpreter.importer

import dsl.pm2.interpreter.program.Pm2Program

class ChildProgram(
    val id: String
) {
    lateinit var program: Pm2Program
}
