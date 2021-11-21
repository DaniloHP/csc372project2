package grammars;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import parser.Type;

public class MathGrammar extends Grammar {

    /**
     * Builds a MathGrammar as so:
     * <expr>     ::= <expr> + <mmd_expr> | <expr> - <mmd_expr> | <mmd_expr>
     * <mmd_expr> ::= <mmd_expr>*<root> | <mmd_expr>/<root> |                    <mmd_expr> mod <root> | <root>
     * <root>     ::= <integer> | <var> | (<expr>) | -<expr>
     * @param vg The VarGrammar on which all other grammars depend for making
     *           sure that variables within expressions exist and are of the
     *           right type.
     */
    public MathGrammar(VarGrammar vg) {
        super();
        // <as_expr>
        Rule addRule = new Rule("(?<left>.*)\\+(?<right>.*)", "ADDITION");
        Rule addRuleRight = new Rule("(?<left>.*?)\\+(?<right>.*)", "ADDITION_RIGHT");
        Rule subRule = new Rule("(?<left>.*)-(?<right>.*)", "SUBTRACTION");
        Rule subRuleRight = new Rule("(?<left>.*?)-(?<right>.*)", "SUBTRACTION_RIGHT");
        Rule asDownRule = new Rule(BASE_DOWN_RULE, "DOWN_AS");
        List<Rule> asExpr = Arrays.asList(addRule, subRule, asDownRule, addRuleRight, subRuleRight);
        // <mmd_expr>
        Rule mulRule = new Rule("(?<left>.*)\\*(?<right>.*)", "MULTIPLICATION");
        Rule mulRuleRight = new Rule("(?<left>.*?)\\*(?<right>.*)", "MULTIPLICATION_RIGHT");
        Rule divRule = new Rule("(?<left>.*)/(?<right>.*)", "DIVISION");
        Rule divRuleRight = new Rule("(?<left>.*?)/(?<right>.*)", "DIVISION_RIGHT");
        Rule modRule = new Rule(
            "(?<left>.*) +(?<replaceMe>mod) +(?<right>.*)",
            "MODULUS",
            new AbstractMap.SimpleEntry<>("mod", "%")
        );
        Rule modRuleRight = new Rule(
            "(?<left>.*?) +(?<replaceMe>mod) +(?<right>.*)",
            "MODULUS_RIGHT",
            new AbstractMap.SimpleEntry<>("mod", "%")
        );
        Rule mmdDownRule = new Rule(BASE_DOWN_RULE, "DOWN_MMD");
        List<Rule> mmdExpr = Arrays.asList(
            mulRule,
            divRule,
            modRule,
            mmdDownRule,
            mulRuleRight,
            divRuleRight,
            modRuleRight
        );
        // <root>
        Rule parenRule = new Rule("\\((?<inner>.*)\\)", "PARENTHESES");
        Rule negRule = new Rule("-(?<inner>.*)", "UNARY_NEGATIVE");
        VarRule mathVarRule = new VarRule(VAR_RULE, "MATH_VAR");
        mathVarRule.useType(Type.INT);
        List<Rule> rootExpr = Arrays.asList(mathVarRule, INT_RULE, parenRule, negRule);

        //// Populate the levels, bottom up
        // <root>
        mathVarRule.addChildren("var", vg.exposeEntrypoint(Type.INT));
        parenRule.addChildren("inner", asExpr);
        negRule.addChildren("inner", asExpr);

        // <mmd_expr>
        populateBinaryRules(
            mmdExpr,
            rootExpr,
            mulRule,
            divRule,
            modRule,
            mulRuleRight,
            divRuleRight,
            modRuleRight
        );
        mmdDownRule.addChildren("inner", rootExpr);

        // <as_expr>
        populateBinaryRules(asExpr, mmdExpr, addRule, subRule, addRuleRight, subRuleRight);
        asDownRule.addChildren("inner", mmdExpr);
        levels.addAll(Arrays.asList(asExpr, mmdExpr, rootExpr));
    }
}
