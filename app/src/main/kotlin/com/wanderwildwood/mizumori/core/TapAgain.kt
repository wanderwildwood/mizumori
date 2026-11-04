package com.wanderwildwood.mizumori.core

/** The "tap again" question: the first tap arms, the second confirms. A row stays armed until the next other touch, Back or leaving the screen; there is no timer. */
object TapAgain {
    data class Result(val armed: Boolean, val confirmed: Boolean)

    fun press(armed: Boolean): Result = if (armed) Result(armed = false, confirmed = true) else Result(armed = true, confirmed = false)
}
