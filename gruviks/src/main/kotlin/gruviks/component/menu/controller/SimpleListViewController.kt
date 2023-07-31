package gruviks.component.menu.controller

import gruviks.component.Component
import gruviks.event.Event
import gruviks.event.UpdateEvent
import gruviks.space.Point
import gruviks.space.RectRegion
import java.util.*

class SimpleListViewController<T>(
        private val elements: MutableList<T>,
        val generateComponents: (
                /**
                 * The element for which components should be generated
                 */
                element: T,
                /**
                 * The index of `element` into `elements`
                 */
                index: Int,
                /**
                 * The result of `generateComponents(previousComponent)`, or `null` if this is the first component.
                 * Use this position to determine where the components should be placed.
                 */
                position: Point?,
                /**
                 * You should insert all components that you want to add in this list, along with their position.
                 */
                components: MutableList<Pair<Component, RectRegion>>,
                /**
                 * Notifies this controller that `elements` has been changed
                 */
                refreshController: () -> Unit
        ) -> Point
) : SimpleFlatController() {

    private var lastGeneratedElements = emptyList<GeneratedElementComponents<T>>()
    private var shouldRefresh = true

    override fun processEvent(event: Event) {
        if (shouldRefresh && event is UpdateEvent) {
            shouldRefresh = false
            updateComponents()
        }
    }

    private fun updateComponents() {
        val generatingElements = elements.toList()
        if (lastGeneratedElements.size > generatingElements.size) {
            for (index in generatingElements.size until lastGeneratedElements.size) {
                for (component in lastGeneratedElements[index].components) this.menu.removeComponent(component)
            }
        }

        var nextPosition: Point? = null
        val componentsToAdd = mutableListOf<Pair<Component, RectRegion>>()

        val generatedElements = mutableListOf<GeneratedElementComponents<T>>()
        for ((index, element) in generatingElements.withIndex()) {
            var shouldAddNewComponents = false
            if (index < lastGeneratedElements.size) {
                val lastEntry = lastGeneratedElements[index]
                if (element == lastEntry.element && nextPosition == lastEntry.position) {
                    generatedElements.add(lastEntry)
                    nextPosition = lastEntry.nextPosition
                } else {
                    for (component in lastEntry.components) menu.removeComponent(component)
                    shouldAddNewComponents = true
                }
            } else shouldAddNewComponents = true

            if (shouldAddNewComponents) {
                val position = nextPosition
                nextPosition = generateComponents(element, index, position, componentsToAdd, this::refresh)
                for ((component, region) in componentsToAdd) {
                    menu.addComponent(component, region)
                }
                generatedElements.add(GeneratedElementComponents(element, position, nextPosition, componentsToAdd.map { it.first.id }))
                componentsToAdd.clear()
            }
        }

        lastGeneratedElements = generatedElements
    }

    fun refresh() {
        shouldRefresh = true
    }

    private class GeneratedElementComponents<T>(
        val element: T,
        val position: Point?,
        val nextPosition: Point,
        val components: Collection<UUID>
    )
}

