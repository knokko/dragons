package gruviks.event

abstract class KeyEvent : Event()

class KeyTypeEvent(val codePoint: Int) : KeyEvent()

class KeyPressEvent(val key: Key, val isRepeat: Boolean) : KeyEvent()

class KeyReleaseEvent(val key: Key) : KeyEvent()

abstract class KeyboardFocusEvent : Event()

class KeyboardFocusAcquiredEvent : KeyboardFocusEvent()

class KeyboardFocusLostEvent : KeyboardFocusEvent()

class KeyboardFocusRejectedEvent : KeyboardFocusEvent()

class Key(
    val code: Int,
    val type: KeyType
)

enum class KeyType {
    Left,
    Right,
    Up,
    Down,
    Escape,
    Backspace,
    Enter,
    Tab,
    Other
}
