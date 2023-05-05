package dsl.pm2.interpreter.instruction

enum class Pm2InstructionType {
    PushValue,
    PushVariable,
    PushProperty,

    Divide,
    Multiply,
    Add,
    Subtract,

    SmallerThan,
    SmallerOrEqual,

    Duplicate,
    Delete,

    DeclareVariable,
    ReassignVariable,
    SetProperty,

    Jump,
    InvokeBuiltinFunction,

    PushScope,
    PopScope
}
