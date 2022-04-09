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
        ), "test", listOf(
            DirectionGroup(0, 4, TextDirection.LeftToRight)
        ))

        testOrderChars(charArrayOf(
            '1', '2', '3', '4'
        ), "1234", listOf(
            DirectionGroup(0, 4, TextDirection.Number)
        ))

        testOrderChars(charArrayOf(
            'ט', 'ל', 'א'
        ), "אלט", listOf(
            DirectionGroup(0, 3, TextDirection.RightToLeft)
        ))

        testOrderChars(charArrayOf(
            '.', ',', ';'
        ), ".,;", listOf(
            DirectionGroup(0, 3, TextDirection.LeftToRight)
        ))

        testOrderChars(charArrayOf(
            't', 'e', 's', 't', '1', '2', '3', '4'
        ), "test1234", listOf(
            DirectionGroup(0, 4, TextDirection.LeftToRight),
            DirectionGroup(4, 8, TextDirection.Number)
        ))

        testOrderChars(charArrayOf(
            '(', 'O', 'n', 'l', 'y', ')', ' ', '1', ' ', 'w', 'o', 'r', 'd', ' ',
            '(', '(', 'ט', 'ל', 'א', ')', ' ', 'i', 's', ' ', 'H', 'e', 'b', 'r', 'e', 'w'
        ), "(Only) 1 word ((אלט) is Hebrew", listOf(
            DirectionGroup(0, 7, TextDirection.LeftToRight),
            DirectionGroup(7, 8, TextDirection.Number),
            DirectionGroup(8, 16, TextDirection.LeftToRight),
            DirectionGroup(16, 19, TextDirection.RightToLeft),
            DirectionGroup(19, 30, TextDirection.LeftToRight)
        ))

        // These strings are ripped from the Hebrew William Shakespeare Wikipedia page
        // (but some are slightly modified to make things more interesting)

        // Note: all brackets are inverted because they are *mirrored*
        testOrderChars(charArrayOf(
            ')', '5', '2', ' ', 'ט', 'ל', 'א', '(', ' ', ')', 'ש', 'י', 'נ', 'א', 'י',
            'ל', 'ו', 'י', '(', ' ', '1', '6', '1', '6', ' ', 'ל', 'י', 'ר', 'פ', 'א',
            ' ', '2', '3'
        ), "23 אפריל 1616 (יוליאניש) (אלט 52)", listOf(
            DirectionGroup(0, 2, TextDirection.Number),
            DirectionGroup(2, 9, TextDirection.RightToLeft),
            DirectionGroup(9, 13, TextDirection.Number),
            DirectionGroup(13, 30, TextDirection.RightToLeft),
            DirectionGroup(30, 32, TextDirection.Number),
            DirectionGroup(32, 33, TextDirection.RightToLeft)
        ))

        testOrderChars(charArrayOf(
            ')', ')', ')', 'ן', 'ע', 'מ', ' ', 'ס', 'נ', 'י', 'י', 'ל', 'ר', ')', 'ע', 'ב',
            'מ', 'ע', 'ש', '(', 'ט', ' ', 'ד', 'ר', 'א', 'ל', '(', '(', ' ', 'L', 'o',
            'r', 'd', ' ', 'C', 'h', 'a', 'm', 'b', 'e', 'r', 'l', 'a', 'i', 'n', '\'',
            's', ' ', 'M', 'e', 'n', ' ', 'י', 'ד', ' ', 'ן', 'ס', 'י', 'י', 'ה', 'ע', 'ג'
        ), "געהייסן די Lord Chamberlain's Men ((לארד ט(שעמבע)רליינס מען)))", listOf(
            DirectionGroup(0, 11, TextDirection.RightToLeft),
            DirectionGroup(11, 33, TextDirection.LeftToRight),
            DirectionGroup(33, 62, TextDirection.RightToLeft)
        ))

        testOrderChars(charArrayOf(
            'ד', 'ר', 'א', 'ל', '(', '(', ' ', 'L', 'o', 'r', 'd', ' ', 'C', 'h', 'a', 'm',
            'b', 'e', 'r', 'l', 'a', 'i', 'n', '\'', 's', ' ', 'M', 'e', 'n', ' ', 'ר'
        ), "ר Lord Chamberlain's Men ((לארד", listOf(
            DirectionGroup(0, 2, TextDirection.RightToLeft),
            DirectionGroup(2, 24, TextDirection.LeftToRight),
            DirectionGroup(24, 31, TextDirection.RightToLeft)
        ))

        testOrderChars(charArrayOf(
            ')', 'a', '(', ')', 'b', ' ', '5', '2', ' ', 'ל', ')', '(', 'ט', 'א', '('
        ), "(אט()ל 52 a()b)", listOf(
            DirectionGroup(0, 7, TextDirection.RightToLeft),
            DirectionGroup(7, 9, TextDirection.Number),
            DirectionGroup(9, 10, TextDirection.RightToLeft),
            DirectionGroup(10, 14, TextDirection.LeftToRight),
            DirectionGroup(14, 15, TextDirection.RightToLeft)
        ))
    }

    private fun testOrderChars(
        expected: CharArray, inputString: String, expectedGroups: List<DirectionGroup>
    ) {
        val codepoints = inputString.codePoints().toArray()
        assertEquals(expectedGroups, groupText(codepoints, getPrimaryDirection(codepoints)))
        assertArrayEquals(expected.map { it.code }.toIntArray(), orderChars(codepoints))
    }
}