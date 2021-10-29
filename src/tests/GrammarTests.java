package tests;

import static org.junit.jupiter.api.Assertions.*;

import grammars.MathGrammar;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GrammarTests {

    @Test
    void testMathGrammar() {
        var grammar = new MathGrammar();
        var corrects = List.of("1+1", "1 + 1", "1    / 1", "1/1/x/1",
                "100 mod 1000", "10 * (1 / (val * 3))", "100", "x",
                "(8 - 1 + 3) * 6 - ((3 +y) * 2)");
        for (String test : corrects) {
            assertTrue(grammar.isValid(test));
        }
        var inCorrects = List.of("1++1", "(1/5", "100.1 * 1, 90 */ 10",
                "100 mod (2 * (4/10)", "");
        for (String test : inCorrects) {
            assertFalse(grammar.isValid(test));
        }
    }
}
