package com.benisalla.glitch.menu

/**
 * When the menu draws a new order. Pure, so it is unit tested on the JVM.
 *
 * - As a normal app (not the default home): one new order each time the menu is newly opened.
 * - As the default home screen: one new order per unlock. It is drawn when the screen turns off,
 *   so it is ready before the user sees it; returning Home after using an app keeps the order.
 * - Always: a first order when there is none yet (cold process start); never on recomposition
 *   or configuration change.
 */
object ReshufflePolicy {

    /**
     * Activity.onStart. [newSession] is true on the first start and on a start after a stop that
     * was not a configuration change.
     */
    fun shuffleOnStart(hasOrder: Boolean, newSession: Boolean, isDefaultHome: Boolean): Boolean =
        !hasOrder || (newSession && !isDefaultHome)

    /**
     * The screen turned off. As a normal app, the stop that comes with it already starts a new
     * session, so only the home screen shuffles here.
     */
    fun shuffleOnScreenOff(isDefaultHome: Boolean): Boolean = isDefaultHome
}
