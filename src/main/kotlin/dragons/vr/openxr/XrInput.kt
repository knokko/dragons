package dragons.vr.openxr

import org.lwjgl.openxr.*
import org.lwjgl.openxr.XR10.*
import org.lwjgl.system.MemoryStack.stackPush

class XrInput(
    xrInstance: XrInstance,
    xrVkSession: XrSession
) {

    val dragonActionSet: XrActionSet
    val dragonMovementAction: XrAction

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

            val ciDragonMovementAction = XrActionCreateInfo.calloc(stack)
            ciDragonMovementAction.`type$Default`()
            ciDragonMovementAction.actionType(XR_ACTION_TYPE_VECTOR2F_INPUT)
            ciDragonMovementAction.actionName(stack.UTF8("dragon_movement"))
            ciDragonMovementAction.localizedActionName(stack.UTF8("Dragon Movement"))

            val pDragonMovementAction = stack.callocPointer(1)
            assertXrSuccess(
                xrCreateAction(this.dragonActionSet, ciDragonMovementAction, pDragonMovementAction),
                "CreateAction", "ehm"
            )
            this.dragonMovementAction = XrAction(pDragonMovementAction[0], this.dragonActionSet)

            val aiDragonSet = XrSessionActionSetsAttachInfo.calloc(stack)
            aiDragonSet.`type$Default`()
            aiDragonSet.actionSets(stack.pointers(this.dragonActionSet.address()))

            assertXrSuccess(
                xrAttachSessionActionSets(xrVkSession, aiDragonSet),
                "xrAttachSessionActionSets"
            )
        }
    }

    fun destroy() {
        xrDestroyActionSet(this.dragonActionSet)
    }
}