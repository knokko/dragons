package dragons.vr.openxr

import dragons.vr.controls.DragonControls
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
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
    val dragonWalkSideThresholdAction: XrAction
    val dragonWalkForwardAction: XrAction
    val dragonWalkForwardThresholdAction: XrAction
    val dragonTurnCameraAction: XrAction
    val dragonSpitAction: XrAction
    val dragonUsePowerAction: XrAction
    val dragonToggleMenuAction: XrAction
    val dragonToggleLeftWingAction: XrAction
    val dragonToggleRightWingAction: XrAction
    val dragonHandPoseAction: XrAction
    val dragonLeftHandSpace: XrSpace
    val dragonRightHandSpace: XrSpace
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
            this.dragonWalkSideThresholdAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_BOOLEAN_INPUT, "dragon_walk_side_threshold", "Threshold: walk left/right (dragon)"
            )
            this.dragonWalkForwardAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_FLOAT_INPUT, "dragon_walk_forward", "Walk forward (dragon)"
            )
            this.dragonWalkForwardThresholdAction = createAction(
                this.dragonActionSet, XR_ACTION_TYPE_BOOLEAN_INPUT, "dragon_walk_forward_threshold", "Threshold: walk forward (dragon)"
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
            val leftHandPath = getPath(stack, "/user/hand/left")
            val rightHandPath = getPath(stack, "/user/hand/right")
            this.dragonHandPoseAction = run {
                ciAction.actionType(XR_ACTION_TYPE_POSE_INPUT)
                ciAction.actionName(stack.UTF8("dragon_hand_location"))
                ciAction.localizedActionName(stack.UTF8("Position of your hands"))
                ciAction.subactionPaths(stack.longs(leftHandPath, rightHandPath))

                assertXrSuccess(
                    xrCreateAction(this.dragonActionSet, ciAction, pAction),
                    "CreateAction", "dragon hand location"
                )
                XrAction(pAction[0], dragonActionSet)
            }

            fun createHandSpace(handPath: Long, handName: String): XrSpace {
                val ciSpace = XrActionSpaceCreateInfo.calloc(stack)
                ciSpace.`type$Default`()
                ciSpace.action(this.dragonHandPoseAction)
                ciSpace.subactionPath(handPath)
                ciSpace.poseInActionSpace().`position$`().set(0f, 0f, 0f)
                ciSpace.poseInActionSpace().orientation().set(0f, 0f, 0f, 1f)

                val pSpace = stack.callocPointer(1)
                assertXrSuccess(
                    xrCreateActionSpace(xrSession, ciSpace, pSpace),
                    "CreateActionSpace", "$handName hand"
                )
                return XrSpace(pSpace[0], xrSession)
            }

            this.dragonLeftHandSpace = createHandSpace(leftHandPath, "left")
            this.dragonRightHandSpace = createHandSpace(rightHandPath, "right")

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

    private fun suggestOculusTouchBindings(stack: MemoryStack) {
        val suggestedBindings = XrActionSuggestedBinding.calloc(14, stack)

        suggestedBindings[0].action(this.dragonWalkSideAction)
        suggestedBindings[0].binding(getPath(stack, "/user/hand/left/input/thumbstick/x"))

        suggestedBindings[1].action(this.dragonWalkSideThresholdAction)
        suggestedBindings[1].binding(getPath(stack, "/user/hand/left/input/thumbstick/x"))

        suggestedBindings[2].action(this.dragonWalkForwardAction)
        suggestedBindings[2].binding(getPath(stack, "/user/hand/left/input/thumbstick/y"))

        suggestedBindings[3].action(this.dragonWalkForwardThresholdAction)
        suggestedBindings[3].binding(getPath(stack, "/user/hand/left/input/thumbstick/y"))

        suggestedBindings[4].action(this.dragonTurnCameraAction)
        suggestedBindings[4].binding(getPath(stack, "/user/hand/right/input/thumbstick/x"))

        suggestedBindings[5].action(this.dragonSpitAction)
        suggestedBindings[5].binding(getPath(stack, "/user/hand/left/input/x/click"))

        suggestedBindings[6].action(this.dragonUsePowerAction)
        suggestedBindings[6].binding(getPath(stack, "/user/hand/right/input/a/click"))

        suggestedBindings[7].action(this.dragonToggleMenuAction)
        suggestedBindings[7].binding(getPath(stack, "/user/hand/left/input/menu/click"))

        suggestedBindings[8].action(this.dragonToggleLeftWingAction)
        suggestedBindings[8].binding(getPath(stack, "/user/hand/left/input/trigger/value"))

        suggestedBindings[9].action(this.dragonToggleRightWingAction)
        suggestedBindings[9].binding(getPath(stack, "/user/hand/right/input/trigger/value"))

        suggestedBindings[10].action(this.dragonHandPoseAction)
        suggestedBindings[10].binding(getPath(stack, "/user/hand/left/input/grip/pose"))

        suggestedBindings[11].action(this.dragonHandPoseAction)
        suggestedBindings[11].binding(getPath(stack, "/user/hand/right/input/grip/pose"))

        suggestedBindings[12].action(this.dragonGrabLeftAction)
        suggestedBindings[12].binding(getPath(stack, "/user/hand/left/input/squeeze/value"))

        suggestedBindings[13].action(this.dragonGrabRightAction)
        suggestedBindings[13].binding(getPath(stack, "/user/hand/right/input/squeeze/value"))

        val suggestedOculusTouchBindings = XrInteractionProfileSuggestedBinding.calloc(stack)
        suggestedOculusTouchBindings.`type$Default`()
        suggestedOculusTouchBindings.interactionProfile(getPath(stack, "/interaction_profiles/oculus/touch_controller"))
        suggestedOculusTouchBindings.suggestedBindings(suggestedBindings)

        assertXrSuccess(
            xrSuggestInteractionProfileBindings(xrInstance, suggestedOculusTouchBindings),
            "SuggestInteractionProfileBindings", "Oculus Touch"
        )
    }

    fun getDragonControls(renderSpace: XrSpace, handsDisplayTime: Long?): DragonControls {
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

            val isWalking = getPressValue(this.dragonWalkSideThresholdAction) || getPressValue(this.dragonWalkForwardThresholdAction)

            var leftHandPosition: Vector3f? = null
            var rightHandPosition: Vector3f? = null

            var leftHandOrientation: Quaternionf? = null
            var rightHandOrientation: Quaternionf? = null

            if (handsDisplayTime != null) {
                val handPose = XrSpaceLocation.calloc(stack)
                handPose.`type$Default`()
                assertXrSuccess(
                    xrLocateSpace(this.dragonLeftHandSpace, renderSpace, handsDisplayTime, handPose),
                    "LocateSpace", "left hand"
                )

                var handPosition = handPose.pose().`position$`()
                var handOrientation = handPose.pose().orientation()

                if ((handPose.locationFlags() and XR_SPACE_LOCATION_POSITION_VALID_BIT.toLong()) != 0L) {
                    leftHandPosition = Vector3f(handPosition.x(), handPosition.y(), handPosition.z())
                }
                if ((handPose.locationFlags() and XR_SPACE_LOCATION_ORIENTATION_VALID_BIT.toLong()) != 0L) {
                    leftHandOrientation =
                        Quaternionf(handOrientation.x(), handOrientation.y(), handOrientation.z(), handOrientation.w())
                }

                assertXrSuccess(
                    xrLocateSpace(this.dragonRightHandSpace, renderSpace, handsDisplayTime, handPose),
                    "LocateSpace", "right hand"
                )

                handPosition = handPose.pose().`position$`()
                handOrientation = handPose.pose().orientation()

                if ((handPose.locationFlags() and XR_SPACE_LOCATION_POSITION_VALID_BIT.toLong()) != 0L) {
                    rightHandPosition = Vector3f(handPosition.x(), handPosition.y(), handPosition.z())
                }
                if ((handPose.locationFlags() and XR_SPACE_LOCATION_ORIENTATION_VALID_BIT.toLong()) != 0L) {
                    rightHandOrientation =
                        Quaternionf(handOrientation.x(), handOrientation.y(), handOrientation.z(), handOrientation.w())
                }
            }

            DragonControls(
                walkDirection = Vector2f(
                    if (isWalking) getFloatValue(this.dragonWalkSideAction) else 0f,
                    if (isWalking) getFloatValue(this.dragonWalkForwardAction) else 0f
                ),
                cameraTurnDirection = getFloatValue(this.dragonTurnCameraAction),
                isSpitting = getPressValue(this.dragonSpitAction),
                isUsingPower = getPressValue(this.dragonUsePowerAction),
                shouldToggleMenu = getToggleValue(this.dragonToggleMenuAction),
                shouldToggleLeftWing = getToggleValue(this.dragonToggleLeftWingAction),
                shouldToggleRightWing = getToggleValue(this.dragonToggleRightWingAction),
                isGrabbingLeft = getPressValue(this.dragonGrabLeftAction),
                isGrabbingRight = getPressValue(this.dragonGrabRightAction),
                leftHandPosition = leftHandPosition,
                rightHandPosition = rightHandPosition,
                leftHandOrientation = leftHandOrientation,
                rightHandOrientation = rightHandOrientation
            )
        }
    }

    fun destroy() {
        xrDestroyActionSet(this.dragonActionSet)
        xrDestroySpace(this.dragonLeftHandSpace)
        xrDestroySpace(this.dragonRightHandSpace)
    }
}