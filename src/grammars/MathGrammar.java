package grammars;

import java.util.ArrayList;
import java.util.List;

public class MathGrammar extends Grammar {

    public MathGrammar() {
        super();
        // <as_expr>
        Rule addRule = new Rule("(.*)\\+(.*)", "ADDITION");
        Rule addRuleRight = new Rule("(.*?)\\+(.*)", "ADDITION_RIGHT");
        Rule subRule = new Rule("(.*)-(.*)", "SUBTRACTION");
        Rule subRuleRight = new Rule("(.*?)-(.*)", "SUBTRACTION_RIGHT");
        Rule asDownRule = baseDownRule.clone();
        asDownRule.id += "_AS";
        List<Rule> asExpr = new ArrayList<>(
            List.of(addRule, subRule, asDownRule, addRuleRight, subRuleRight)
        );
        // <mmd_expr>
        Rule mulRule = new Rule("(.*)\\*(.*)", "MULTIPLICATION");
        Rule mulRuleRight = new Rule("(.*?)\\*(.*)", "MULTIPLICATION_RIGHT");
        Rule divRule = new Rule("(.*)/(.*)", "DIVISION");
        Rule divRuleRight = new Rule("(.*?)/(.*)", "DIVISION_RIGHT");
        Rule modRule = new Rule("(.*)mod(.*)", "MODULUS");
        Rule modRuleRight = new Rule("(.*?)mod(.*)", "MODULUS_RIGHT");
        Rule mmdDownRule = baseDownRule.clone();
        mmdDownRule.id += "_MMD";
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
        Rule expRule = new Rule("(.*)\\^(.*)", "EXPONENTIATION");
        Rule expRuleRight = new Rule("(.*?)\\^(.*)", "EXPONENTIATION_RIGHT");
        Rule expDownRule = baseDownRule.clone();
        expDownRule.id += "_EXP";
        List<Rule> exExpr = new ArrayList<>(List.of(expRule, expRuleRight, expDownRule));
        // <root>
        Rule parenRule = new Rule("\\((.*)\\)", "PARENTHESES");
        Rule negRule = new Rule("-(.*)", "UNARY_NEGATIVE");
        List<Rule> rootExpr = new ArrayList<>(List.of(varRule, intRule, parenRule, negRule));

        //// Populate the levels, bottom up
        // <root>
        parenRule.addChildren(1, asExpr);
        negRule.addChildren(1, asExpr);

        // <ex_expr>
        populateBinaryRules(exExpr, rootExpr, expRule, expRuleRight);
        expDownRule.addChildren(1, rootExpr);

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
        mmdDownRule.addChildren(1, exExpr);

        // <as_expr>
        populateBinaryRules(asExpr, mmdExpr, addRule, subRule, addRuleRight, subRuleRight);
        asDownRule.addChildren(1, mmdExpr);
        levels.addAll(List.of(asExpr, mmdExpr, exExpr, rootExpr));
    }
}
