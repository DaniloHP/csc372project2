package grammars;

import java.util.ArrayList;
import java.util.List;
import parser.Type;

public class MathGrammar extends Grammar {

    public MathGrammar(VarGrammar vg) {
        super();
        // <as_expr>
        Rule addRule = new Rule("(?<left>.*)\\+(?<right>.*)", "ADDITION");
        Rule addRuleRight = new Rule("(?<left>.*?)\\+(?<right>.*)", "ADDITION_RIGHT");
        Rule subRule = new Rule("(?<left>.*)-(?<right>.*)", "SUBTRACTION");
        Rule subRuleRight = new Rule("(?<left>.*?)-(?<right>.*)", "SUBTRACTION_RIGHT");
        Rule asDownRule = new Rule(BASE_DOWN_RULE, "DOWN_AS");
        List<Rule> asExpr = new ArrayList<>(
            List.of(addRule, subRule, asDownRule, addRuleRight, subRuleRight)
        );
        // <mmd_expr>
        Rule mulRule = new Rule("(?<left>.*)\\*(?<right>.*)", "MULTIPLICATION");
        Rule mulRuleRight = new Rule("(?<left>.*?)\\*(?<right>.*)", "MULTIPLICATION_RIGHT");
        Rule divRule = new Rule("(?<left>.*)/(?<right>.*)", "DIVISION");
        Rule divRuleRight = new Rule("(?<left>.*?)/(?<right>.*)", "DIVISION_RIGHT");
        Rule modRule = new Rule("(?<left>.*)mod(?<right>.*)", "MODULUS");
        Rule modRuleRight = new Rule("(?<left>.*?)mod(?<right>.*)", "MODULUS_RIGHT");
        Rule mmdDownRule = new Rule(BASE_DOWN_RULE, "DOWN_MMD");
        List<Rule> mmdExpr = new ArrayList<>(
            List.of(
                mulRule,
                divRule,
                modRule,
                mmdDownRule,
                mulRuleRight,
                divRuleRight,
                modRuleRight
            )
        );
        // <ex-expr>
        Rule expRule = new Rule("(?<left>.*)\\^(?<right>.*)", "EXPONENTIATION");
        Rule expRuleRight = new Rule("(?<left>.*?)\\^(?<right>.*)", "EXPONENTIATION_RIGHT");
        Rule expDownRule = new Rule(BASE_DOWN_RULE, "DOWN_EXP");
        List<Rule> exExpr = new ArrayList<>(List.of(expRule, expRuleRight, expDownRule));
        // <root>
        Rule parenRule = new Rule("\\((?<inner>.*)\\)", "PARENTHESES");
        Rule negRule = new Rule("-(?<inner>.*)", "UNARY_NEGATIVE");
        Rule mathVarRule = new VarRule(VAR_RULE, "MATH_VAR");
        List<Rule> rootExpr = new ArrayList<>(List.of(mathVarRule, INT_RULE, parenRule, negRule));

        //// Populate the levels, bottom up
        // <root>
        mathVarRule.addChildren("var", vg.exposeEntrypoint(Type.INT));
        parenRule.addChildren("inner", asExpr);
        negRule.addChildren("inner", asExpr);

        // <ex_expr>
        populateBinaryRules(exExpr, rootExpr, expRule, expRuleRight);
        expDownRule.addChildren("inner", rootExpr);

        // <mmd_expr>
        populateBinaryRules(
            mmdExpr,
            exExpr,
            mulRule,
            divRule,
            modRule,
            mulRuleRight,
            divRuleRight,
            modRuleRight
        );
        mmdDownRule.addChildren("inner", exExpr);

        // <as_expr>
        populateBinaryRules(asExpr, mmdExpr, addRule, subRule, addRuleRight, subRuleRight);
        asDownRule.addChildren("inner", mmdExpr);
        levels.addAll(List.of(asExpr, mmdExpr, exExpr, rootExpr));
    }
}
