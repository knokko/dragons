package gruviks.event.raw

class RawClipboardPasteEvent(val content: String) : RawEvent()

class RawClipboardCopyEvent(val cut: Boolean, val setText: (String) -> Unit) : RawEvent()
