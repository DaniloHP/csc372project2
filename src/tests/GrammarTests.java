package tests;

import grammars.*;
import java.util.HashMap;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import parser.ScopeStack;
import parser.Type;
import parser.Variable;
import parser.errors.InvalidStatementError;
import parser.errors.TypeError;
import parser.errors.VariableError;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests are run sequentially in the order they appear (top to bottom)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GrammarTests {

    VarGrammar varGrammar = new VarGrammar();
    MathGrammar mathGrammar = new MathGrammar(varGrammar);
    StringGrammar strGrammar = new StringGrammar(varGrammar);
    BoolGrammar boolGrammar = new BoolGrammar(mathGrammar, varGrammar);
    RayGrammar rayGrammar = new RayGrammar(boolGrammar, mathGrammar, strGrammar);

    @Order(1)
    @Test
    void testMathGrammar() {
        VarRule.checkVarTypes = false;
        assertTrue(mathGrammar.validate("x"));
        assertTrue(mathGrammar.validate("-1"));
        assertTrue(mathGrammar.validate("(1)"));
        assertTrue(mathGrammar.validate("-(1)"));
        assertTrue(mathGrammar.validate("(x)"));
        assertTrue(mathGrammar.validate("100"));
        assertTrue(mathGrammar.validate("1+1"));
        assertTrue(mathGrammar.validate("1+-1"));
        assertTrue(mathGrammar.validate("1 - 1"));
        assertTrue(mathGrammar.validate("1 * -1"));
        assertTrue(mathGrammar.validate("1    / 1"));
        assertTrue(mathGrammar.validate("1/1/x/1"));
        assertTrue(mathGrammar.validate("100 mod 1000"));
        assertTrue(mathGrammar.validate("10 * (1 / (val * 3))"));
        assertTrue(mathGrammar.validate("(8 - 1 + 3) * 6 - ((3 +y) * 2)"));

        assertFalse(mathGrammar.validate(""));
        assertFalse(mathGrammar.validate("+"));
        assertFalse(mathGrammar.validate("1++1"));
        assertFalse(mathGrammar.validate("(1/5"));
        assertFalse(mathGrammar.validate("100.1 * 1, 90 */ 10"));
        assertFalse(mathGrammar.validate("100 mod (2 * (4/10)"));
    }

    @Order(2)
    @Test
    void testBooleanGrammar() {
        assertTrue(boolGrammar.validate("T"));
        assertTrue(boolGrammar.validate("F"));
        assertTrue(boolGrammar.validate("T and F"));
        assertTrue(boolGrammar.validate("T or F"));
        assertTrue(boolGrammar.validate("not T"));
        assertTrue(boolGrammar.validate("not (not T)"));
        assertTrue(boolGrammar.validate("1 == 1"));
        assertTrue(boolGrammar.validate("1 == x"));
        assertTrue(boolGrammar.validate("1 <= 99"));
        assertTrue(boolGrammar.validate("1 >= 10"));
        assertTrue(boolGrammar.validate("1 < 10"));
        assertTrue(boolGrammar.validate("1 > 10"));
        assertTrue(boolGrammar.validate("1 != 10"));
        assertTrue(boolGrammar.validate("(1 != 10)"));
        assertTrue(boolGrammar.validate("x or (1 != 10)"));
        assertTrue(boolGrammar.validate("x and y and z"));
        assertTrue(boolGrammar.validate("(x + y * 2) == z"));
        assertTrue(boolGrammar.validate("(x and (z == 10)) and (y != 100 or l or 5 < x) or (T)"));

        assertFalse(boolGrammar.validate("1 === 1"));
        assertFalse(boolGrammar.validate("1 <== 1"));
        assertFalse(boolGrammar.validate("1 <== 1 1"));
        assertFalse(boolGrammar.validate("1 && 1"));
        assertFalse(boolGrammar.validate("T and"));
        assertFalse(boolGrammar.validate("T and ()"));
        assertFalse(boolGrammar.validate("(1 + 1 == 2))"));
        assertFalse(boolGrammar.validate("not 1"));
        assertFalse(boolGrammar.validate("1 = 10"));
        assertFalse(boolGrammar.validate("(x and (z == 10)) and () or (T)"));
    }

    @Order(3)
    @Test
    void testRays() {
        assertTrue(rayGrammar.validate("[1]"));
        assertTrue(rayGrammar.validate("[\"one\"]"));
        assertTrue(rayGrammar.validate("[\"\", \"\"]"));
        assertTrue(rayGrammar.validate("[1,2,3,4]"));
        assertTrue(rayGrammar.validate("[var, x, y, z, 10]"));
        assertTrue(rayGrammar.validate("[    var, x  , y   , z    , 10]"));
        assertTrue(rayGrammar.validate("[\"1\",\"2\",\"3\",\"4\"]"));
        assertTrue(rayGrammar.validate("[\"1\",\"2\",str1, str2]"));
        assertTrue(
            rayGrammar.validate("[(x and (z == 10)) and (y != 100 or l or 5 < x) or (T), T, F]")
        );
        assertTrue(rayGrammar.validate("[(8 - 1 + 3) * 6 - ((3 +y) * 2), 1, 2, x]"));

        assertThrows(InvalidStatementError.class, () -> rayGrammar.validate("[]"));
        assertThrows(InvalidStatementError.class, () -> rayGrammar.validate("[\"]"));
        assertThrows(
            InvalidStatementError.class,
            () ->
                rayGrammar.validate("[(x and (z == 10)) and (y != 100 or l or 5 < x) or (T), 1, 2]")
        );
        assertThrows(InvalidStatementError.class, () -> rayGrammar.validate("[1,2,]"));
        assertThrows(InvalidStatementError.class, () -> rayGrammar.validate("[1,2, \"three\"]"));
    }

    @Order(4)
    @Test
    void testStrings() {
        assertTrue(strGrammar.validate("\"\""));
        assertTrue(strGrammar.validate("\"string literal with some stuff\""));
        assertTrue(strGrammar.validate("someStringVar"));
        assertTrue(
            strGrammar.validate(
                "\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*()-=[]{}\\|';:<>,.?/`~\""
            )
        );

        assertFalse(strGrammar.validate(""));
        assertFalse(strGrammar.validate("\"a string with a\nline break\""));
        assertFalse(strGrammar.validate("\"a string with poorly\"closed quotes\""));
    }

    @Order(5)
    @Test
    void testVars() {
        VarGrammar vg = new VarGrammar();
        VarRule rule = Grammar.VAR_RULE;
        assertTrue(rule.validate("___var_11_xy_9"));
        assertTrue(rule.validate("___var_11_xy_9"));
        assertTrue(rule.validate("i"));
        assertTrue(rule.validate("i"));
        for (String keyword : VarRule.RESERVED_KEYWORDS) {
            assertTrue(rule.validate("_" + keyword));
            assertTrue(rule.validate("_" + keyword));
            assertTrue(rule.validate(keyword + "_"));
            assertTrue(rule.validate(keyword + "_"));
        }

        assertFalse(rule.validate("_12345678901234567890123456789012"));
        assertFalse(rule.validate("_12345678901234567890123456789012"));
        //^ Variable longer than 32 chars
        assertFalse(rule.validate("0__bad_var_11_xx_1"));
        assertFalse(rule.validate("0__bad_var_11_xx_1"));
        for (String keyword : VarRule.NONVALUE_KEYWORDS) {
            assertThrows(VariableError.class, () -> rule.validate(keyword, null, true, false));
            assertFalse(rule.validate("T == 1 + " + keyword, null, true, false));
        }
    }

    @Order(6)
    @Test
    void testReplacement() {
        assertEquals("fal_F_se && false", boolGrammar.keywordsToJava("fal_F_se and F"));
        assertEquals("false || true", boolGrammar.keywordsToJava("F or T"));
        assertEquals("false || true && true", boolGrammar.keywordsToJava("F or T and T"));
        assertEquals("false || true && true || 1 < 1 % 0", boolGrammar.keywordsToJava("F or T and T or 1 < 1 mod 0"));
        assertEquals("true", boolGrammar.keywordsToJava("T"));
        assertEquals("! true", boolGrammar.keywordsToJava("not T"));
        assertEquals("! false", boolGrammar.keywordsToJava("not F"));
        assertEquals("! (! false)", boolGrammar.keywordsToJava("not (not F)"));
        assertEquals("True", boolGrammar.keywordsToJava("True"));
        assertEquals("True", boolGrammar.keywordsToJava("True"));
        assertEquals("falseF", boolGrammar.keywordsToJava("falseF"));
        assertEquals("fal_T_se && false", boolGrammar.keywordsToJava("fal_T_se and F"));
        assertEquals("fal_T_se && true", boolGrammar.keywordsToJava("fal_T_se and T"));
        assertEquals("falseF && false", boolGrammar.keywordsToJava("falseF and F"));
        assertEquals("trueT && false", boolGrammar.keywordsToJava("trueT and F"));
        assertEquals("false", boolGrammar.keywordsToJava("F"));
        assertEquals("1 % 10", mathGrammar.keywordsToJava("1 mod 10"));
        assertEquals("1 % 10 % 100 % 0", mathGrammar.keywordsToJava("1 mod 10 mod 100 mod 0"));
        assertEquals("1 % 10 % 100 % 0 < 10", boolGrammar.keywordsToJava("1 mod 10 mod 100 mod 0 < 10"));
        assertEquals("! (1 % 10 % 100 % 0 < 10)", boolGrammar.keywordsToJava("not (1 mod 10 mod 100 mod 0 < 10)"));
        assertEquals("(x && (z == 10)) && ! (y != 100 || l || 5 < x) || (true)", boolGrammar.keywordsToJava("(x and (z == 10)) and not (y != 100 or l or 5 < x) or (T)"));
    }

    /**
     * This test runs last because it messes with static fields, and if it
     * fails, the static fields might not be reset to their default values.
     */
    @Order(7)
    @Test
    void testStrictTypeChecking() {
        VarRule.checkVarTypes = true;
        ScopeStack scopes = new ScopeStack();
        VarRule.useScopes(scopes);

        VarRule rule = new VarRule(Grammar.VAR_RULE);
        rule.useType(Type.INT);
        scopes.add(new HashMap<>());
        Variable i = new Variable("i", Type.INT);
        scopes.addToCurrScope(i);
        assertTrue(rule.validate("i"));
        assertThrows(VariableError.class, () -> rule.validate("j"));

        rule.useType(Type.STRING);
        assertThrows(TypeError.class, () -> rule.validate("i")); //i is an int
        assertThrows(VariableError.class, () -> rule.validate(" str "));
        Variable str = new Variable("str", Type.STRING);
        scopes.addToCurrScope(str);
        assertTrue(rule.validate(" str "));

        VarGrammar vg = new VarGrammar();
        MathGrammar mg = new MathGrammar(vg);

        assertTrue(mg.validate("i"));
        assertTrue(mg.validate("i + i"));
        assertThrows(VariableError.class, () -> mg.validate("i + j"));
        assertThrows(TypeError.class, () -> mg.validate("i + str"));

        BoolGrammar bg = new BoolGrammar(mg, vg);
        Variable bool1 = new Variable("bool1", Type.BOOL);
        Variable bool2 = new Variable("bool2", Type.BOOL);
        Variable bool3 = new Variable("bool3", Type.BOOL);
        scopes.addToCurrScope(bool1, bool2, bool3);
        assertThrows(VariableError.class, () -> bg.validate("bool and bool2 and bool3"));
        assertTrue(bg.validate("bool1 and bool2 and bool3"));
        assertTrue(bg.validate("bool1 and bool2 and 1 < 2"));
        assertTrue(bg.validate("bool1 and bool2 and i < 2"));
        assertThrows(TypeError.class, () -> bg.validate("bool1 and bool2 < 2"));
        RayGrammar rg = new RayGrammar(bg, mg, new StringGrammar(varGrammar));
        assertThrows(InvalidStatementError.class, () -> rg.validate("[T,T,T,1]"));
        assertTrue(rg.validate("[bool1, bool2, bool3]"));
        assertTrue(rg.validate("[bool1, bool1, bool1]"));
        assertTrue(rg.validate("[bool1, bool2, T]"));
        assertTrue(rg.validate("[\"\", \"hi\"]"));
        assertTrue(rg.validate("[\"\", \"hi\", str]"));
        assertThrows(InvalidStatementError.class, () -> rg.validate("[bool1, bool2, bool3, 1]"));
        assertThrows(InvalidStatementError.class, () -> rg.validate("[bool1, bool2, bool3, i]"));
        assertThrows(InvalidStatementError.class, () -> rg.validate("[bool1, bool2, str, i]"));
        assertThrows(InvalidStatementError.class, () -> rg.validate("[bool1, bool2, str, i]"));
        VarRule.useScopes(null);
    }
}
