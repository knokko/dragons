package gruviks.feedback

import java.util.*

class AddressedFeedback(
    /**
     * The ID of the parent component to whom this feedback is addressed, or null if it is addressed to the window
     */
    val targetID: UUID?,

    /**
     * The feedback to be delivered to the target component or window
     */
    val targetFeedback: Feedback
): Feedback() {
    override fun toString() = "AddressedFeedback(targetID=$targetID, feedback=$targetFeedback)"
}
