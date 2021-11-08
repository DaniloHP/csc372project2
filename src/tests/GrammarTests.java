package tests;

import static org.junit.jupiter.api.Assertions.*;

import grammars.BoolGrammar;
import grammars.MathGrammar;
import grammars.RayGrammar;
import grammars.StringGrammar;
import org.junit.jupiter.api.Test;

public class GrammarTests {

    MathGrammar mathGrammar = new MathGrammar();
    StringGrammar strGrammar = new StringGrammar();
    BoolGrammar boolGrammar = new BoolGrammar(mathGrammar);
    RayGrammar rayGrammar = new RayGrammar(boolGrammar, mathGrammar, strGrammar);

    @Test
    void testMathGrammar() {
        assertTrue(mathGrammar.isValid("x"));
        assertTrue(mathGrammar.isValid("-1"));
        assertTrue(mathGrammar.isValid("(1)"));
        assertTrue(mathGrammar.isValid("-(1)"));
        assertTrue(mathGrammar.isValid("(x)"));
        assertTrue(mathGrammar.isValid("___var_11_xy_9"));
        assertTrue(mathGrammar.isValid("100"));
        assertTrue(mathGrammar.isValid("1+1"));
        assertTrue(mathGrammar.isValid("1+-1"));
        assertTrue(mathGrammar.isValid("1 - 1"));
        assertTrue(mathGrammar.isValid("1 * -1"));
        assertTrue(mathGrammar.isValid("1 ^ 1"));
        assertTrue(mathGrammar.isValid("1    / 1"));
        assertTrue(mathGrammar.isValid("1/1/x/1"));
        assertTrue(mathGrammar.isValid("100 mod 1000"));
        assertTrue(mathGrammar.isValid("10 * (1 / (val * 3))"));
        assertTrue(mathGrammar.isValid("(8 - 1 + 3) * 6 - ((3 +y) * 2)"));

        assertFalse(mathGrammar.isValid(""));
        assertFalse(mathGrammar.isValid("+"));
        assertFalse(mathGrammar.isValid("1++1"));
        assertFalse(mathGrammar.isValid("(1/5"));
        assertFalse(mathGrammar.isValid("0__bad_var_11_xx_1"));
        assertFalse(mathGrammar.isValid("100.1 * 1, 90 */ 10"));
        assertFalse(mathGrammar.isValid("100 mod (2 * (4/10)"));
    }

    @Test
    void testBooleanGrammar() {
        assertTrue(boolGrammar.isValid("T"));
        assertTrue(boolGrammar.isValid("F"));
        assertTrue(boolGrammar.isValid("T and F"));
        assertTrue(boolGrammar.isValid("T or F"));
        assertTrue(boolGrammar.isValid("not T"));
        assertTrue(boolGrammar.isValid("not (not T)"));
        assertTrue(boolGrammar.isValid("1 == 1"));
        assertTrue(boolGrammar.isValid("1 == x"));
        assertTrue(boolGrammar.isValid("1 <= 99"));
        assertTrue(boolGrammar.isValid("1 >= 10"));
        assertTrue(boolGrammar.isValid("1 < 10"));
        assertTrue(boolGrammar.isValid("1 > 10"));
        assertTrue(boolGrammar.isValid("1 != 10"));
        assertTrue(boolGrammar.isValid("(1 != 10)"));
        assertTrue(boolGrammar.isValid("x or (1 != 10)"));
        assertTrue(boolGrammar.isValid("x and y and z"));
        assertTrue(boolGrammar.isValid("(x + y * 2) == z"));
        assertTrue(boolGrammar.isValid("(x and (z == 10)) and (y != 100 or l or 5 < x) or (T)"));

        assertFalse(boolGrammar.isValid("1 === 1"));
        assertFalse(boolGrammar.isValid("1 <== 1"));
        assertFalse(boolGrammar.isValid("1 <== 1 1"));
        assertFalse(boolGrammar.isValid("1 && 1"));
        assertFalse(boolGrammar.isValid("T and"));
        assertFalse(boolGrammar.isValid("T and ()"));
        assertFalse(boolGrammar.isValid("(1 + 1 == 2))"));
        assertFalse(boolGrammar.isValid("not 1"));
        assertFalse(boolGrammar.isValid("1 = 10"));
        assertFalse(boolGrammar.isValid("(x and (z == 10)) and () or (T)"));
    }

    @Test
    void testRays() {
        assertTrue(rayGrammar.isValid("[1]"));
        assertTrue(rayGrammar.isValid("[\"one\"]"));
        assertTrue(rayGrammar.isValid("[\"\", \"\"]"));
        assertTrue(rayGrammar.isValid("[1,2,3,4]"));
        assertTrue(rayGrammar.isValid("[var, x, y, z, 10]"));
        assertTrue(rayGrammar.isValid("[    var, x  , y   , z    , 10]"));
        assertTrue(rayGrammar.isValid("[\"1\",\"2\",\"3\",\"4\"]"));
        assertTrue(rayGrammar.isValid("[\"1\",\"2\",str1, str2]"));

        assertFalse(rayGrammar.isValid("[]"));
        assertFalse(rayGrammar.isValid("[\"]"));
        assertFalse(rayGrammar.isValid("[1,2,]"));
        assertFalse(rayGrammar.isValid("[1,2, \"three\"]"));
    }

    @Test
    void testStrings() {
        assertTrue(strGrammar.isValid("\"\""));
        assertTrue(strGrammar.isValid("\"string literal with some stuff\""));
        assertTrue(strGrammar.isValid("someStringVar"));
        assertTrue(
            strGrammar.isValid(
                "\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*()-=[]{}\\|';:<>,.?/`~\""
            )
        );

        assertFalse(strGrammar.isValid(""));
        assertFalse(strGrammar.isValid("\"a string with a\nline break\""));
        assertFalse(strGrammar.isValid("\"a string with poorly\"closed quotes\""));
    }
}
