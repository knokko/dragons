package gruviks.space

import kotlin.math.absoluteValue
import kotlin.math.sign

fun wouldMultiplicationOverflow(a: Long, b: Long): Boolean {
    // Multiplication with 0 clearly never overflows. This case needs special treatment because the check below would
    // divide by 0 and raise an Exception
    if (a == 0L || b == 0L) return false

    val product = a * b
    return product / a != b || product / b != a
}

fun divideRounded(numerator: Long, denominator: Long): Long {
    val rawQuotient = numerator / denominator
    val recoveredNumerator = rawQuotient * denominator

    return if (numerator.sign * denominator.sign >= 0) {
        if ((numerator - recoveredNumerator).absoluteValue <= (numerator - ((rawQuotient + 1) * denominator)).absoluteValue) {
            rawQuotient
        } else {
            rawQuotient + 1
        }
    } else {
        if ((numerator - recoveredNumerator).absoluteValue <= (numerator - ((rawQuotient - 1) * denominator)).absoluteValue) {
            rawQuotient
        } else {
            rawQuotient - 1
        }
    }
}
