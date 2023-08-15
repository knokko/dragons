package dsl.pm2.interpreter.instruction

enum class Pm2InstructionType {
    PushValue,
    PushVariable,
    PushProperty,
    ReadListOrMap,

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
    UpdateListOrMap,

    Jump,
    InvokeBuiltinFunction,
    TransferVariable,
    CreateDynamicMatrix,
    CreateChildModel,
    ExitProgram,

    PushScope,
    PopScope
}
