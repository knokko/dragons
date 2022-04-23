package dragons.vr.openxr

import dragons.vr.controls.DragonControls
import org.joml.Vector2f
import org.lwjgl.openxr.*
import org.lwjgl.openxr.XR10.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush

class XrInput(
    private val xrInstance: XrInstance,
    private val xrSession: XrSession
) {

    val dragonActionSet: XrActionSet
    val dragonWalkSideAction: XrAction
    val dragonWalkForwardAction: XrAction
    val dragonTurnCameraAction: XrAction
    val dragonSpitAction: XrAction
    val dragonUsePowerAction: XrAction
    val dragonToggleMenuAction: XrAction
    val dragonToggleLeftWingAction: XrAction
    val dragonToggleRightWingAction: XrAction
    val dragonGrabLeftAction: XrAction
    val dragonGrabRightAction: XrAction

    init {
        stackPush().use { stack ->
            val ciDragonActionSet = XrActionSetCreateInfo.calloc(stack)
            ciDragonActionSet.`type$Default`()
            ciDragonActionSet.actionSetName(stack.UTF8("dragon_controls"))
            ciDragonActionSet.localizedActionSetName(stack.UTF8("Dragon Controls"))
            ciDragonActionSet.priority(1)

            val pDragonActionSet = stack.callocPointer(1)

            assertXrSuccess(
                xrCreateActionSet(xrInstance, ciDragonActionSet, pDragonActionSet),
                "CreateActionSet", "dragon controls"
            )
            this.dragonActionSet = XrActionSet(pDragonActionSet[0], xrInstance)

            val ciAction = XrActionCreateInfo.calloc(stack)
            ciAction.`type$Default`()

            val pAction = stack.callocPointer(1)

            fun createAction(set: XrActionSet, type: Int, name: String, localizedName: String): XrAction {

                ciAction.actionType(type)
                ciAction.actionName(stack.UTF8(name))
                ciAction.localizedActionName(stack.UTF8(localizedName))

                assertXrSuccess(
                    xrCreateAction(set, ciAction, pAction),
                    "CreateAction", name
                )
                return XrAction(pAction[0], set)
            }

            this.dragonWalkSideAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_FLOAT_INPUT, "dragon_walk_side", "Walk left/right (dragon)"
            )
            this.dragonWalkForwardAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_FLOAT_INPUT, "dragon_walk_forward", "Walk forward (dragon)"
            )
            this.dragonTurnCameraAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_FLOAT_INPUT, "dragon_turn_camera", "Turn camera left/right (dragon)"
            )
            this.dragonSpitAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_BOOLEAN_INPUT, "dragon_spit", "Spit (dragon)"
            )
            this.dragonUsePowerAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_BOOLEAN_INPUT, "dragon_use_power", "Use power (dragon)"
            )
            this.dragonToggleMenuAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_BOOLEAN_INPUT, "dragon_toggle_menu", "Toggle menu (dragon)"
            )
            this.dragonToggleLeftWingAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_BOOLEAN_INPUT, "dragon_toggle_left_wing", "Toggle left wing (dragon)"
            )
            this.dragonToggleRightWingAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_BOOLEAN_INPUT, "dragon_toggle_right_wing", "Toggle right wing (dragon)"
            )
            this.dragonGrabLeftAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_BOOLEAN_INPUT, "dragon_grab_left", "Grab with left claw (dragon)"
            )
            this.dragonGrabRightAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_BOOLEAN_INPUT, "dragon_grab_right", "Grab with right claw (dragon)"
            )

            // Avoid code duplication by using the Khronos controller binding whenever possible
            this.suggestKhronosSimpleControllerProfileBinding(stack)
            // TODO Also add suggested bindings for all other controllers
            this.suggestOculusTouchBindings(stack)

            val aiSessionActions = XrSessionActionSetsAttachInfo.calloc(stack)
            aiSessionActions.`type$Default`()
            aiSessionActions.actionSets(stack.pointers(this.dragonActionSet.address()))

            assertXrSuccess(
                xrAttachSessionActionSets(this.xrSession, aiSessionActions),
                "xrAttachSessionActionSets"
            )
        }
    }

    private fun getPath(stack: MemoryStack, stringPath: String): Long {
        val pPath = stack.callocLong(1)
        assertXrSuccess(
            xrStringToPath(xrInstance, stack.UTF8(stringPath), pPath),
            "StringToPath", stringPath
        )
        return pPath[0]
    }

    private fun suggestKhronosSimpleControllerProfileBinding(stack: MemoryStack) {
        // TODO Add hand pose actions
        val suggestedBindings = XrActionSuggestedBinding.calloc(1, stack)

        suggestedBindings[0].action(this.dragonToggleMenuAction)
        suggestedBindings[0].binding(getPath(stack, "/user/hand/left/input/menu/click"))

        val suggestedSimpleControllerBindings = XrInteractionProfileSuggestedBinding.calloc(stack)
        suggestedSimpleControllerBindings.`type$Default`()
        suggestedSimpleControllerBindings.interactionProfile(getPath(stack, "/interaction_profiles/khr/simple_controller"))
        suggestedSimpleControllerBindings.suggestedBindings(suggestedBindings)

        assertXrSuccess(
            xrSuggestInteractionProfileBindings(xrInstance, suggestedSimpleControllerBindings),
            "SuggestInteractionProfileBindings", "Khronos simple controller profile"
        )
    }

    private fun suggestOculusTouchBindings(stack: MemoryStack) {
        val suggestedBindings = XrActionSuggestedBinding.calloc(9, stack)

        suggestedBindings[0].action(this.dragonWalkSideAction)
        suggestedBindings[0].binding(getPath(stack, "/user/hand/left/input/thumbstick/x"))

        suggestedBindings[1].action(this.dragonWalkForwardAction)
        suggestedBindings[1].binding(getPath(stack, "/user/hand/left/input/thumbstick/y"))

        suggestedBindings[2].action(this.dragonTurnCameraAction)
        suggestedBindings[2].binding(getPath(stack, "/user/hand/right/input/thumbstick/x"))

        suggestedBindings[3].action(this.dragonSpitAction)
        suggestedBindings[3].binding(getPath(stack, "/user/hand/left/input/x/click"))

        suggestedBindings[4].action(this.dragonUsePowerAction)
        suggestedBindings[4].binding(getPath(stack, "/user/hand/right/input/a/click"))

        suggestedBindings[5].action(this.dragonToggleLeftWingAction)
        suggestedBindings[5].binding(getPath(stack, "/user/hand/left/input/squeeze/value"))

        suggestedBindings[6].action(this.dragonToggleRightWingAction)
        suggestedBindings[6].binding(getPath(stack, "/user/hand/right/input/squeeze/value"))

        suggestedBindings[7].action(this.dragonGrabLeftAction)
        suggestedBindings[7].binding(getPath(stack, "/user/hand/left/input/trigger/value"))

        suggestedBindings[8].action(this.dragonGrabRightAction)
        suggestedBindings[8].binding(getPath(stack, "/user/hand/right/input/trigger/value"))

        val suggestedOculusTouchBindings = XrInteractionProfileSuggestedBinding.calloc(stack)
        suggestedOculusTouchBindings.`type$Default`()
        suggestedOculusTouchBindings.interactionProfile(getPath(stack, "/interaction_profiles/oculus/touch_controller"))
        suggestedOculusTouchBindings.suggestedBindings(suggestedBindings)

        assertXrSuccess(
            xrSuggestInteractionProfileBindings(xrInstance, suggestedOculusTouchBindings),
            "SuggestInteractionProfileBindings", "Oculus Touch"
        )
    }

    fun getDragonControls(): DragonControls {
        return stackPush().use { stack ->
            val activeActionSets = XrActiveActionSet.calloc(1, stack)
            activeActionSets[0].actionSet(this.dragonActionSet)
            activeActionSets[0].subactionPath(XR_NULL_PATH)

            val actionsSyncInfo = XrActionsSyncInfo.calloc(stack)
            actionsSyncInfo.`type$Default`()
            actionsSyncInfo.countActiveActionSets(1)
            actionsSyncInfo.activeActionSets(activeActionSets)

            assertXrSuccess(
                xrSyncActions(this.xrSession, actionsSyncInfo),
                "SyncActions", "Dragon Controls"
            )

            val giActionState = XrActionStateGetInfo.calloc(stack)
            giActionState.`type$Default`()
            giActionState.subactionPath(XR_NULL_PATH)

            val booleanState = XrActionStateBoolean.calloc(stack)
            booleanState.`type$Default`()

            val floatState = XrActionStateFloat.calloc(stack)
            floatState.`type$Default`()

            fun getBooleanValue(action: XrAction): Pair<Boolean, Boolean> {
                giActionState.action(action)
                assertXrSuccess(
                    xrGetActionStateBoolean(this.xrSession, giActionState, booleanState),
                    "GetActionStateBoolean"
                )

                return if (booleanState.isActive) Pair(booleanState.changedSinceLastSync(), booleanState.currentState())
                else Pair(false, false)
            }

            fun getToggleValue(action: XrAction): Boolean {
                val (changed, isPressed) = getBooleanValue(action)
                return changed && isPressed
            }

            fun getPressValue(action: XrAction) = getBooleanValue(action).second

            fun getFloatValue(action: XrAction): Float {
                giActionState.action(action)
                assertXrSuccess(
                    xrGetActionStateFloat(this.xrSession, giActionState, floatState),
                    "GetActionStateFloat"
                )

                return if (floatState.isActive) floatState.currentState()
                else 0f
            }

            DragonControls(
                // TODO Take current head rotation into account
                walkDirection = Vector2f(
                    getFloatValue(this.dragonWalkSideAction),
                    getFloatValue(this.dragonWalkForwardAction)
                ),
                cameraTurnDirection = getFloatValue(this.dragonTurnCameraAction),
                isSpitting = getPressValue(this.dragonSpitAction),
                isUsingPower = getPressValue(this.dragonUsePowerAction),
                shouldToggleMenu = getToggleValue(this.dragonToggleMenuAction),
                shouldToggleLeftWing = getToggleValue(this.dragonToggleLeftWingAction),
                shouldToggleRightWing = getToggleValue(this.dragonToggleRightWingAction),
                isGrabbingLeft = getPressValue(this.dragonGrabLeftAction),
                isGrabbingRight = getPressValue(this.dragonGrabRightAction)
            )
        }
    }

    fun destroy() {
        xrDestroyActionSet(this.dragonActionSet)
    }
}