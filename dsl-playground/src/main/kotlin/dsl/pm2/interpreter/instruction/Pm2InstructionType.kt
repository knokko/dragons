package dsl.pm2.interpreter.instruction

enum class Pm2InstructionType {
    PushValue,
    PushVariable,
    PushProperty,
    ReadArrayOrMap,

    Divide,
    Multiply,
    Add,
    Subtract,

    SmallerThan,
    SmallerOrEqual,

    Duplicate,
    Swap,
    Delete,

    DeclareVariable,
    ReassignVariable,
    SetProperty,
    UpdateArrayOrMap,

    Jump,
    InvokeBuiltinFunction,
    TransferVariable,
    CreateDynamicMatrix,
    CreateChildModel,
    ExitProgram,

    PushScope,
    PopScope
}
