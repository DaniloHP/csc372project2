package tests;

import static org.junit.jupiter.api.Assertions.*;

import grammars.MathGrammar;
import org.junit.jupiter.api.Test;

public class GrammarTests {

    @Test
    void testMathGrammar() {
        var grammar = new MathGrammar();
        assertTrue(grammar.isValid("x"));
        assertTrue(grammar.isValid("(1)"));
        assertTrue(grammar.isValid("(x)"));
        assertTrue(grammar.isValid("100"));
        assertTrue(grammar.isValid("1+1"));
        assertTrue(grammar.isValid("1 + 1"));
        assertTrue(grammar.isValid("1    / 1"));
        assertTrue(grammar.isValid("1/1/x/1"));
        assertTrue(grammar.isValid("100 mod 1000"));
        assertTrue(grammar.isValid("10 * (1 / (val * 3))"));
        assertTrue(grammar.isValid("(8 - 1 + 3) * 6 - ((3 +y) * 2)"));

        assertFalse(grammar.isValid(""));
        assertFalse(grammar.isValid("+"));
        assertFalse(grammar.isValid("1++1"));
        assertFalse(grammar.isValid("(1/5"));
        assertFalse(grammar.isValid("100.1 * 1, 90 */ 10"));
        assertFalse(grammar.isValid("100 mod (2 * (4/10)"));
    }
}
