package graviks2d

import graviks2d.resource.text.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestTextPlacer {

    @Test
    fun testIsPrimarilyLeftToRight() {
        assertEquals(TextDirection.LeftToRight, getPrimaryDirection("Hello".codePoints().toArray()))
        assertEquals(TextDirection.LeftToRight, getPrimaryDirection("Hm...".codePoints().toArray()))
        assertEquals(TextDirection.LeftToRight, getPrimaryDirection(".".codePoints().toArray()))
        assertEquals(TextDirection.LeftToRight, getPrimaryDirection("test12345".codePoints().toArray()))
        assertEquals(TextDirection.RightToLeft, getPrimaryDirection("שייקס".codePoints().toArray()))
        assertEquals(TextDirection.RightToLeft, getPrimaryDirection("ש12345קס".codePoints().toArray()))
    }

    @Test
    fun testOrderChars() {
        testOrderChars(charArrayOf(
            't', 'e', 's', 't'
        ), intArrayOf(0, 1, 2, 3), intArrayOf(), "test", listOf(
            DirectionGroup(0, 4, TextDirection.LeftToRight)
        ), BooleanArray(4) { true}, true)

        testOrderChars(charArrayOf(
            '1', '2', '3', '4'
        ), intArrayOf(0, 1, 2, 3), intArrayOf(), "1234", listOf(
            DirectionGroup(0, 4, TextDirection.Number)
        ), BooleanArray(4) { true }, true)

        testOrderChars(charArrayOf(
            'ט', 'ל', 'א'
        ), intArrayOf(2, 1, 0), intArrayOf(), "אלט", listOf(
            DirectionGroup(0, 3, TextDirection.RightToLeft)
        ), BooleanArray(3) { false }, false)

        testOrderChars(charArrayOf(
            '.', ',', ';'
        ), intArrayOf(0, 1, 2), intArrayOf(), ".,;", listOf(
            DirectionGroup(0, 3, TextDirection.LeftToRight)
        ), BooleanArray(3) { true }, true)

        testOrderChars(charArrayOf(
            't', 'e', 's', 't', '1', '2', '3', '4'
        ), intArrayOf(0, 1, 2, 3, 4, 5, 6, 7), intArrayOf(), "test1234", listOf(
            DirectionGroup(0, 4, TextDirection.LeftToRight),
            DirectionGroup(4, 8, TextDirection.Number)
        ), BooleanArray(8) { true }, true)

        testOrderChars(charArrayOf(
            '(', 'O', 'n', 'l', 'y', ')', ' ', '1', ' ', 'w', 'o', 'r', 'd', ' ',
            '(', '(', 'ט', 'ל', 'א', ')', ' ', 'i', 's', ' ', 'H', 'e', 'b', 'r', 'e', 'w'
        ), (0 until 16).toList().toIntArray() + intArrayOf(18, 17, 16) + (19 until 30).toList().toIntArray(),
            intArrayOf(), "(Only) 1 word ((אלט) is Hebrew", listOf(
            DirectionGroup(0, 7, TextDirection.LeftToRight),
            DirectionGroup(7, 8, TextDirection.Number),
            DirectionGroup(8, 16, TextDirection.LeftToRight),
            DirectionGroup(16, 19, TextDirection.RightToLeft),
            DirectionGroup(19, 30, TextDirection.LeftToRight)
        ), BooleanArray(30) { index -> index < 16 || index > 18}, true)

        // These strings are ripped from the Hebrew William Shakespeare Wikipedia page
        // (but some are slightly modified to make things more interesting)

        // Note: all brackets are inverted because they are *mirrored*
        testOrderChars(charArrayOf(
            ')', '5', '2', ' ', 'ט', 'ל', 'א', '(', ' ', ')', 'ש', 'י', 'נ', 'א', 'י',
            'ל', 'ו', 'י', '(', ' ', '1', '6', '1', '6', ' ', 'ל', 'י', 'ר', 'פ', 'א',
            ' ', '2', '3'
        ), intArrayOf(32, 30, 31) + (13 until 30).reversed().toList().toIntArray()
                + intArrayOf(9, 10, 11, 12) + (2 until 9).reversed().toList().toIntArray() + intArrayOf(0, 1),
        intArrayOf(
            0, 7, 9, 18
        ), "23 אפריל 1616 (יוליאניש) (אלט 52)", listOf(
            DirectionGroup(0, 2, TextDirection.Number),
            DirectionGroup(2, 9, TextDirection.RightToLeft),
            DirectionGroup(9, 13, TextDirection.Number),
            DirectionGroup(13, 30, TextDirection.RightToLeft),
            DirectionGroup(30, 32, TextDirection.Number),
            DirectionGroup(32, 33, TextDirection.RightToLeft)
        ), BooleanArray(33) { index -> index == 1 || index == 2 || index in 20..23 || index >= 31 }, false)

        testOrderChars(charArrayOf(
            ')', ')', ')', 'ן', 'ע', 'מ', ' ', 'ס', 'נ', 'י', 'י', 'ל', 'ר', ')', 'ע', 'ב',
            'מ', 'ע', 'ש', '(', 'ט', ' ', 'ד', 'ר', 'א', 'ל', '(', '(', ' ', 'L', 'o',
            'r', 'd', ' ', 'C', 'h', 'a', 'm', 'b', 'e', 'r', 'l', 'a', 'i', 'n', '\'',
            's', ' ', 'M', 'e', 'n', ' ', 'י', 'ד', ' ', 'ן', 'ס', 'י', 'י', 'ה', 'ע', 'ג'
        ), (33 until 62).reversed().toList().toIntArray() + (11 until 33).toList().toIntArray() +
                (0 until 11).reversed().toList().toIntArray(), intArrayOf(
            0, 1, 2, 13, 19, 26, 27
        ), "געהייסן די Lord Chamberlain's Men ((לארד ט(שעמבע)רליינס מען)))", listOf(
            DirectionGroup(0, 11, TextDirection.RightToLeft),
            DirectionGroup(11, 33, TextDirection.LeftToRight),
            DirectionGroup(33, 62, TextDirection.RightToLeft)
        ), BooleanArray(62) { index -> index in 29..50 }, false)

        testOrderChars(charArrayOf(
            'ד', 'ר', 'א', 'ל', '(', '(', ' ', 'L', 'o', 'r', 'd', ' ', 'C', 'h', 'a', 'm',
            'b', 'e', 'r', 'l', 'a', 'i', 'n', '\'', 's', ' ', 'M', 'e', 'n', ' ', 'ר'
        ), (24 until 31).reversed().toList().toIntArray() + (2 until 24).toList().toIntArray() + intArrayOf(1, 0),
            intArrayOf(
            4, 5
        ), "ר Lord Chamberlain's Men ((לארד", listOf(
            DirectionGroup(0, 2, TextDirection.RightToLeft),
            DirectionGroup(2, 24, TextDirection.LeftToRight),
            DirectionGroup(24, 31, TextDirection.RightToLeft)
        ), BooleanArray(31) { index -> index in 7..28 }, false)

        testOrderChars(charArrayOf(
            ')', 'a', '(', ')', 'b', ' ', '5', '2', ' ', 'ל', ')', '(', 'ט', 'א', '('
        ), intArrayOf(
            14, 10, 11, 12, 13, 9, 7, 8, 6, 5, 4, 3, 2, 1, 0
        ), intArrayOf(
            0, 10, 11, 14
        ), "(אט()ל 52 a()b)", listOf(
            DirectionGroup(0, 7, TextDirection.RightToLeft),
            DirectionGroup(7, 9, TextDirection.Number),
            DirectionGroup(9, 10, TextDirection.RightToLeft),
            DirectionGroup(10, 14, TextDirection.LeftToRight),
            DirectionGroup(14, 15, TextDirection.RightToLeft)
        ), BooleanArray(15) { index -> index in 1..4 || index == 6 || index == 7 }, false)
    }

    private fun testOrderChars(
        expected: CharArray, expectedOriginalIndices: IntArray, expectedMirrorIndices: IntArray, inputString: String,
        expectedGroups: List<DirectionGroup>, expectedLeftToRight: BooleanArray, expectPrimarilyLeftToRight: Boolean
    ) {
        val codepoints = inputString.codePoints().toArray()
        assertEquals(expectedGroups, groupText(codepoints, getPrimaryDirection(codepoints)))

        val orderedChars = orderChars(codepoints)
        assertArrayEquals(expected.map { it.code }.toIntArray(), orderedChars.chars)
        assertArrayEquals(expectedOriginalIndices, orderedChars.originalIndices)

        val expectedMirror = BooleanArray(expected.size)
        for (expectedMirrorIndex in expectedMirrorIndices) {
            expectedMirror[expectedMirrorIndex] = true
        }
        assertArrayEquals(expectedMirror, orderedChars.shouldMirror)
        assertArrayEquals(expectedLeftToRight, orderedChars.isLeftToRight)

        assertEquals(expectPrimarilyLeftToRight, orderedChars.isPrimarilyLeftToRight)
    }
}