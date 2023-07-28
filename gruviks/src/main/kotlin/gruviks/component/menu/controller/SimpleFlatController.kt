package gruviks.component.menu.controller

import gruviks.component.menu.SimpleFlatMenu
import gruviks.event.Event

abstract class SimpleFlatController {
    protected lateinit var menu: SimpleFlatMenu

    fun init(menu: SimpleFlatMenu) {
        if (this::menu.isInitialized) throw IllegalStateException("Menu is already initialized")
        this.menu = menu
    }

    abstract fun processEvent(event: Event)
}
