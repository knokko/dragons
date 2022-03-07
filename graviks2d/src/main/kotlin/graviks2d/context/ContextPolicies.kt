package graviks2d.context

enum class DepthPolicy {
    Manual,
    AlwaysIncrement,
    QuickCheck,
    PreciseCheck
}

enum class TranslucentPolicy {
    Manual,
    Forbid,
    Round,
    AlwaysFlush,
    SeparateBufferQuick,
    SeparateBufferPrecise
}
