package tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grammars.*;

import java.util.HashMap;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import parser.Parser;
import parser.ScopeStack;
import parser.Type;
import parser.errors.TypeError;
import parser.errors.VariableError;

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
        VarRule.checkVarTypes = true;
        assertTrue(mathGrammar.isValid("x"));
        assertTrue(mathGrammar.isValid("-1"));
        assertTrue(mathGrammar.isValid("(1)"));
        assertTrue(mathGrammar.isValid("-(1)"));
        assertTrue(mathGrammar.isValid("(x)"));
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
        assertFalse(mathGrammar.isValid("100.1 * 1, 90 */ 10"));
        assertFalse(mathGrammar.isValid("100 mod (2 * (4/10)"));
    }

    @Order(2)
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

    @Order(3)
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
        assertTrue(
            rayGrammar.isValid("[(x and (z == 10)) and (y != 100 or l or 5 < x) or (T), T, F]")
        );
        assertTrue(rayGrammar.isValid("[(8 - 1 + 3) * 6 - ((3 +y) * 2), 1, 2, x]"));

        assertFalse(rayGrammar.isValid("[]"));
        assertFalse(rayGrammar.isValid("[\"]"));
        assertFalse(
            rayGrammar.isValid("[(x and (z == 10)) and (y != 100 or l or 5 < x) or (T), 1, 2]")
        );
        assertFalse(rayGrammar.isValid("[1,2,]"));
        assertFalse(rayGrammar.isValid("[1,2, \"three\"]"));
    }

    @Order(4)
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
        for (String keyword : VarRule.RESERVED_KEYWORDS) {
            assertThrows(VariableError.class, () -> rule.validate(keyword, null, true, false));
            assertFalse(rule.validate("T == 1 + " + keyword, null, true, false));
        }
    }

    /**
     * This test runs last because it messes with static fields, and if it
     * fails, the static fields might not be reset to their default values.
     */
    @Order(6)
    @Test
    void testStrictTypeChecking() {
        VarRule.checkVarTypes = false;
        ScopeStack scopes = new ScopeStack();
        VarRule.useScopes(scopes);

        VarRule rule = new VarRule(Grammar.VAR_RULE);
        rule.useType(Type.INT);
        scopes.add(new HashMap<>());
        Parser.Variable i = new Parser.Variable("i", Type.INT);
        scopes.addToCurrScope(i);
        assertTrue(rule.validate("i"));
        assertThrows(VariableError.class, () -> rule.validate("j"));

        rule.useType(Type.STRING);
        assertThrows(TypeError.class, () -> rule.validate("i")); //i is an int
        assertThrows(VariableError.class, () -> rule.validate(" str "));
        Parser.Variable str = new Parser.Variable("str", Type.STRING);
        scopes.addToCurrScope(str);
        assertTrue(rule.validate(" str "));

        VarGrammar vg = new VarGrammar();
        MathGrammar mg = new MathGrammar(vg);

        assertTrue(mg.isValid("i"));
        assertTrue(mg.isValid("i + i"));
        assertThrows(VariableError.class, () -> mg.isValid("i + j"));
        assertThrows(TypeError.class, () -> mg.isValid("i + str"));

        BoolGrammar bg = new BoolGrammar(mg, vg);
        Parser.Variable bool1 = new Parser.Variable("bool1", Type.BOOL);
        Parser.Variable bool2 = new Parser.Variable("bool2", Type.BOOL);
        Parser.Variable bool3 = new Parser.Variable("bool3", Type.BOOL);
        scopes.addToCurrScope(bool1, bool2, bool3);
        assertThrows(VariableError.class, () -> bg.isValid("bool and bool2 and bool3"));
        assertTrue(bg.isValid("bool1 and bool2 and bool3"));
        assertTrue(bg.isValid("bool1 and bool2 and 1 < 2"));
        assertTrue(bg.isValid("bool1 and bool2 and i < 2"));
        assertThrows(TypeError.class, () -> bg.isValid("bool1 and bool2 < 2"));

        VarRule.useScopes(null);
    }
}
