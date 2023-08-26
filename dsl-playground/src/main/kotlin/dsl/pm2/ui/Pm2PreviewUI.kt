package dsl.pm2.ui

import dsl.pm2.interpreter.BuiltinTypes
import dsl.pm2.interpreter.Pm2Type
import dsl.pm2.interpreter.value.Pm2FloatValue
import dsl.pm2.interpreter.value.Pm2IntValue
import dsl.pm2.interpreter.value.Pm2StringValue
import dsl.pm2.interpreter.value.Pm2Value
import graviks2d.resource.text.TextStyle
import graviks2d.util.Color
import gruviks.component.Component
import gruviks.component.menu.SimpleFlatMenu
import gruviks.component.menu.controller.SimpleFlatController
import gruviks.component.menu.controller.SimpleListViewController
import gruviks.component.text.TextField
import gruviks.component.text.transparentTextFieldStyle
import gruviks.event.Event
import gruviks.event.UpdateEvent
import gruviks.space.Coordinate
import gruviks.space.Point
import gruviks.space.RectRegion
import gruviks.space.SpaceLayout

fun createParameterConfigurationMenu(previewComponent: Pm2PreviewComponent): Component {
    val parameterValues = mutableMapOf<String, Pm2Value>()

    fun updateParameterValues(): String {
        previewComponent.dynamicParameterValues.clear()
        for ((key, value) in parameterValues.entries) {
            previewComponent.dynamicParameterValues[key] = value
        }
        return ""
    }

    val menu = SimpleFlatMenu(SpaceLayout.Simple, backgroundColor)
    menu.addComponent(previewComponent, RectRegion.percentage(0, 0, 70, 100))

    val textFieldStyle = transparentTextFieldStyle(
        defaultStyle = TextStyle(fillColor = Color.rgbInt(200, 200, 200), font = null),
        focusStyle = TextStyle(fillColor = Color.WHITE, font = null)
    )

    val listElements = mutableListOf<Pair<String, Pm2Type>>()
    var lastDynamicParameterTypes = previewComponent.getDynamicParameterTypes()
    for ((name, type) in lastDynamicParameterTypes) {
        listElements.add(Pair(name, type))
        if (type.createDefaultValue != null) {
            parameterValues[name] = type.createDefaultValue.invoke()
        }
    }
    updateParameterValues()

    val controller = SimpleListViewController(listElements) { element, _, position, components, _ ->
        val componentPosition = position ?: Point.percentage(70, 100)
        val (name, type) = element
        val component = when (type) {
            BuiltinTypes.FLOAT -> TextField.floatField(
                    name, parameterValues[name]!!.floatValue(), textFieldStyle,
                    onChange = { newValue -> parameterValues[name] = Pm2FloatValue(newValue); updateParameterValues() }
            )
            BuiltinTypes.INT -> TextField.intField(
                    name, parameterValues[name]!!.intValue(), textFieldStyle,
                    onChange = { newValue -> parameterValues[name] = Pm2IntValue(newValue); updateParameterValues()}
            )
            BuiltinTypes.STRING -> TextField(
                    name, parameterValues[name]!!.castTo<Pm2StringValue>().value, textFieldStyle,
                    onChange = { newValue -> parameterValues[name] = Pm2StringValue(newValue); updateParameterValues() }
            )
            else -> return@SimpleListViewController componentPosition
        }

        val region = RectRegion(
                componentPosition.x, componentPosition.y - Coordinate.percentage(10),
                Coordinate.percentage(100), componentPosition.y
        )

        components.add(Pair(component, region))

        Point(componentPosition.x, componentPosition.y - Coordinate.percentage(11))
    }
    menu.addController(controller)

    class RefreshController : SimpleFlatController() {
        override fun processEvent(event: Event) {
            if (event is UpdateEvent) {
                val newDynamicParameterTypes = previewComponent.getDynamicParameterTypes()
                if (newDynamicParameterTypes != lastDynamicParameterTypes) {
                    lastDynamicParameterTypes = newDynamicParameterTypes
                    listElements.clear()
                    for ((name, type) in newDynamicParameterTypes) {
                        listElements.add(Pair(name, type))
                        if (!parameterValues.containsKey(name) && type.createDefaultValue != null) {
                            parameterValues[name] = type.createDefaultValue.invoke()
                        }
                    }
                    parameterValues.keys.removeIf { !newDynamicParameterTypes.containsKey(it) }
                    updateParameterValues()
                    controller.refresh()
                }
            }
        }
    }
    menu.addController(RefreshController())

    return menu
}
