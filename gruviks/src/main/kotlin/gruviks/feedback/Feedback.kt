package gruviks.feedback

abstract class Feedback

class RenderFeedback : Feedback()

class ExitFeedback : Feedback()

class SelectionFeedback(val selectedText: String) : Feedback()
