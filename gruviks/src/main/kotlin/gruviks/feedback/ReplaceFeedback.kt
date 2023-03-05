package gruviks.feedback

import gruviks.component.Component

class ReplaceMeFeedback(val createReplacement: () -> Component) : Feedback()

class ReplaceYouFeedback(val createReplacement: () -> Component) : Feedback()
