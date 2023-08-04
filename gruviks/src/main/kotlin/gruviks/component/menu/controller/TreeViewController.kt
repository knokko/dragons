package gruviks.component.menu.controller

import gruviks.component.Component
import gruviks.event.Event
import gruviks.event.UpdateEvent
import gruviks.space.Point
import gruviks.space.RectRegion
import java.util.*

class TreeViewController<T>(
    private val rootNode: Node<T>,
    private val goDown: (Point) -> Point,
    private val goUp: (Point) -> Point,
    val generateComponents: (
        /**
         * The node containing the element for which components should be generated
         */
        node: Node<T>,
        /**
         * The indices of `node` into `rootNode`: `rootNode.getChild(indices) === node`
         */
        indices: List<Int>,
        /**
         * The result of `generateComponents(previousComponent)`, after potentially being mapped by `goDown`
         * or `goUp`. This will be `null` if `node === rootNode`.
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

    fun refresh() {
        shouldRefresh = true
    }

    private fun updateComponents() {
        val currentGeneratedElements = mutableListOf<GeneratedElementComponents<T>>()
        val componentsToAdd = mutableListOf<Pair<Component, RectRegion>>()
        val indices = mutableListOf(-1)
        val parents = mutableListOf<Node<T>>()
        var nextPosition: Point? = null
        var currentNode = rootNode
        var generatedNodeIndex = 0

        while (true) {
            // childIndex == -1 -> generate components for currentNode
            // childIndex >= 0 -> generate components for currentNode.children[childIndex]
            val childIndex = indices.removeLast()
            if (childIndex == -1) {

                var shouldGenerate = true
                if (generatedNodeIndex < lastGeneratedElements.size) {
                    val lastElement = lastGeneratedElements[generatedNodeIndex]
                    if (
                        lastElement.element == currentNode.element && lastElement.position == nextPosition
                        && lastElement.numChildren == currentNode.getNumChildren()
                    ) {
                        shouldGenerate = false
                        nextPosition = lastElement.nextPosition
                        currentGeneratedElements.add(lastElement)
                    } else {
                        for (component in lastElement.components) menu.removeComponent(component)
                    }
                }

                if (shouldGenerate) {
                    val oldPosition = nextPosition
                    nextPosition = generateComponents(
                        currentNode, indices.toList(), nextPosition, componentsToAdd, this::refresh
                    )
                    for ((component, position) in componentsToAdd) menu.addComponent(component, position)
                    currentGeneratedElements.add(GeneratedElementComponents(
                        currentNode.element, currentNode.getNumChildren(), oldPosition, nextPosition,
                        componentsToAdd.map { it.first.id }
                    ))
                    componentsToAdd.clear()
                }
                generatedNodeIndex += 1

                indices.add(0)
                nextPosition = goDown(nextPosition!!)
            } else {
                val children = currentNode.children
                if (children != null && children.size > childIndex) {
                    indices.add(childIndex)
                    indices.add(-1)
                    parents.add(currentNode)
                    currentNode = children[childIndex]
                } else {
                    if (indices.isEmpty()) break
                    indices.add(indices.removeLast() + 1)
                    currentNode = parents.removeLast()
                    nextPosition = goUp(nextPosition!!)
                }
            }
        }

        if (lastGeneratedElements.size > generatedNodeIndex) {
            for (index in generatedNodeIndex until lastGeneratedElements.size) {
                for (component in lastGeneratedElements[index].components) this.menu.removeComponent(component)
            }
        }

        lastGeneratedElements = currentGeneratedElements
    }

    class Node<T>(var element: T, var children: MutableList<Node<T>>?) {
        // TODO Test this
        fun getChild(indices: List<Int>): Node<T> {
            var node = this
            for (index in indices) {
                node = node.children!![index]
            }
            return node
        }

        internal fun getNumChildren() = this.children?.size
    }

    private class GeneratedElementComponents<T>(
        val element: T,
        val numChildren: Int?,
        val position: Point?,
        val nextPosition: Point,
        val components: Collection<UUID>
    )
}
