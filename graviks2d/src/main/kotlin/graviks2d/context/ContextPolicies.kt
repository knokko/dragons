package graviks2d.context

enum class DepthPolicy {
    Automatic,
    Manual
}

enum class TranslucentPolicy {
    Manual,
    Forbid,
    Round,
    AlwaysFlush,
    SeparateBufferQuick,
    SeparateBufferPrecise
}
