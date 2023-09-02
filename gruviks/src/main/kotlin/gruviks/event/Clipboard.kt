package gruviks.event

abstract class ClipboardEvent: Event()

class ClipboardPasteEvent(val content: String) : ClipboardEvent()

class ClipboardCopyEvent(val cut: Boolean, val setText: (String) -> Unit) : ClipboardEvent()
