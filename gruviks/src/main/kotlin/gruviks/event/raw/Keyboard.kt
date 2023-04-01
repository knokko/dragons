package gruviks.event.raw

import gruviks.event.Key

class RawKeyTypeEvent(val codePoint: Int) : RawEvent()

class RawKeyPressEvent(val key: Key, val isRepeat: Boolean) : RawEvent()

class RawKeyReleaseEvent(val key: Key) : RawEvent()
