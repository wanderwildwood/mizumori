package com.wanderwildwood.mizumori.core

/** The "tap again" question: the first tap arms, the second confirms. A row disarms itself after six seconds. */
object TapAgain {
    data class Result(val armed: Boolean, val confirmed: Boolean)

    fun press(armed: Boolean): Result = if (armed) Result(armed = false, confirmed = true) else Result(armed = true, confirmed = false)
}
