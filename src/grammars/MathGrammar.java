package grammars;

import java.util.*;

public class MathGrammar {
    private final List<GrammarLevel> levels;

    public MathGrammar() {
        levels = new ArrayList<>();
        Rule baseDownRule = new Rule("(.*)", "DOWN_RULE"); //used throughout

        // <as_expr>
        Rule addRule = new Rule("(.*)\\+(.*)", "ADDITION");
        Rule addRuleRight = new Rule("(.*?)\\+(.*)", "ADDITION");
        Rule subRule = new Rule("(.*)-(.*)", "SUBTRACTION");
        Rule subRuleRight = new Rule("(.*?)-(.*)", "SUBTRACTION");
        Rule asDownRule = baseDownRule.clone();
        asDownRule.id += "_AS";
        GrammarLevel asExpr = new GrammarLevel(addRule, subRule, asDownRule,
                addRuleRight, subRuleRight);
        // <mmd_expr>
        Rule mulRule = new Rule("(.*)\\*(.*)", "MULTIPLICATION");
        Rule mulRuleRight = new Rule("(.*?)\\*(.*)", "MULTIPLICATION");
        Rule divRule = new Rule("(.*)/(.*)", "DIVISION");
        Rule divRuleRight = new Rule("(.*?)/(.*)", "DIVISION");
        Rule modRule = new Rule("(.*)mod(.*)", "MODULUS");
        Rule modRuleRight = new Rule("(.*?)mod(.*)", "MODULUS");
        Rule mmdDownRule = baseDownRule.clone();
        mmdDownRule.id += "_MMD";
        GrammarLevel mmdExpr = new GrammarLevel(mulRule, divRule, modRule,
                mmdDownRule, mulRuleRight, divRuleRight, modRuleRight);
        // <ex-expr>
        Rule expRule = new Rule("(.*)^(.*)", "EXPONENTIATION");
        Rule expRuleRight = new Rule("(.*?)^(.*)", "EXPONENTIATION");
        Rule expDownRule = baseDownRule.clone();
        expDownRule.id += "_ExP";
        GrammarLevel exExpr = new GrammarLevel(expRule, expDownRule);
        // <root>
        Rule varRule = new Rule("[a-zA-Z_]+[a-zA-Z_\\d]*", "VARIABLES");
        Rule intRule = new Rule("\\d+", "INTEGERS");
        Rule parenRule = new Rule("\\((.*)\\)", "PARENTHESES");
        GrammarLevel rootExpr = new GrammarLevel(varRule, intRule, parenRule);

        //// Populate the levels, bottom up
        // <root>
        parenRule.addChildren(1, asExpr);

        // <ex_expr>
        expRule.addChildren(1, exExpr);
        expRule.addChildren(2, rootExpr);
        expDownRule.addChildren(1, rootExpr);

        // <mmd_expr>
        populateBinaryRules(mmdExpr, exExpr, mulRule, divRule, modRule,
                mulRuleRight, divRuleRight, modRuleRight);
        mmdDownRule.addChildren(1, rootExpr);

        // <as_expr>
        populateBinaryRules(asExpr, mmdExpr, addRule, subRule, addRuleRight,
                subRuleRight);
        asDownRule.addChildren(1, mmdExpr);
        levels.addAll(List.of(asExpr, mmdExpr, exExpr, rootExpr));
    }

    private void populateBinaryRules(GrammarLevel left,
                                     GrammarLevel right, Rule... rules) {
        for (var rule : rules) {
            rule.addChildren(1, left);
            rule.addChildren(2, right);
        }
    }

    public void addLevel(GrammarLevel level) {
        levels.add(level);
    }

    public boolean isValid(CharSequence toCheck) {
        return levels.get(0).validate(toCheck);
    }
}
