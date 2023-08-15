package dsl.pm2.ui

import dsl.pm2.interpreter.value.Pm2PositionValue
import dsl.pm2.interpreter.value.Pm2VertexValue
import graviks2d.target.GraviksTarget
import gruviks.component.Component
import gruviks.component.RenderResult
import gruviks.event.CursorClickEvent
import gruviks.event.Event

class Pm2ShapeEditor(
    val points: MutableList<Pm2PositionValue>,
    val triangles: MutableList<Triple<Int, Int, Int>>
): Component() {
    override fun subscribeToEvents() {
        agent.subscribe(CursorClickEvent::class)
    }

    override fun processEvent(event: Event) {
        if (event is CursorClickEvent) {
        }
    }

    override fun render(target: GraviksTarget, force: Boolean): RenderResult {
        TODO("Not yet implemented")
    }
}