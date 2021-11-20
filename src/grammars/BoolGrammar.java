package grammars;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import parser.Type;

public class BoolGrammar extends Grammar {

    /**
     * BooleanGrammar depends on MathGrammar for comparisons, so it's entrypoint
     * *must* be provided.
     */
    public BoolGrammar(MathGrammar mg, VarGrammar vg) {
        super();
        //<or_expr>
        Rule orRule = new Rule(
            "(?<left>.*) +(?<replaceMe>or) +(?<right>.*)",
            "OR",
            new SimpleEntry<>("or", "||")
        );
        Rule orRuleRight = new Rule(
            "(?<left>.*) +(?<replaceMe>or) +(?<right>.*)",
            "OR_RIGHT",
            new SimpleEntry<>("or", "||")
        );
        Rule orDownRule = new Rule(BASE_DOWN_RULE, "DOWN_OR");
        List<Rule> orExpr = new ArrayList<>(List.of(orRule, orRuleRight, orDownRule));

        //<and_expr>
        Rule andRule = new Rule(
            "(?<left>.*) +(?<replaceMe>and) +(?<right>.*)",
            "AND",
            new SimpleEntry<>("and", "&&")
        );
        Rule andRuleRight = new Rule(
            "(?<left>.*?) +(?<replaceMe>and) +(?<right>.*)",
            "AND_RIGHT",
            new SimpleEntry<>("and", "&&")
        );
        Rule andDownRule = new Rule(BASE_DOWN_RULE, "DOWN_AND");
        List<Rule> andExpr = new ArrayList<>(List.of(andRule, andRuleRight, andDownRule));

        //<not_expr>
        Rule notRule = new Rule(
            "(?<replaceMe>not) +(?<inner>.*)",
            "UNARY NOT",
            new SimpleEntry<>("not", "!")
        );
        Rule notDownRuleToRoot = new Rule(BASE_DOWN_RULE, "DOWN_TO_ROOT");
        Rule notDownRuleToCmp = new Rule(BASE_DOWN_RULE, "DOWN_TO_CMP");
        List<Rule> notExpr = new ArrayList<>(List.of(notRule, notDownRuleToRoot, notDownRuleToCmp));

        //<bool>
        Rule bool = new Rule(
            "(?<replaceMe>[TF])",
            "BOOL",
            new SimpleEntry<>("T", "true"),
            new SimpleEntry<>("F", "false")
        );
        Rule boolParenRule = new Rule("\\((?<inner>.*)\\)", "PARENTHESES");
        VarRule boolVarRule = new VarRule(VAR_RULE, "BOOL_VAR");
        boolVarRule.useType(Type.BOOL);
        List<Rule> boolRootExpr = new ArrayList<>(List.of(bool, boolParenRule, boolVarRule));

        //<comparison>: !=, ==, <, <=, >, >=
        Rule notEqualRule = new Rule("(?<left>.*) +!= +(?<right>.*)", "NOT_EQUAL");
        Rule notEqualRuleRight = new Rule("(?<left>.*) +!= +(?<right>.*)", "NOT_EQUAL_RIGHT");
        Rule equalRule = new Rule("(?<left>.*) +== +(?<right>.*)", "EQUAL");
        Rule equalRuleRight = new Rule("(?<left>.*) +== +(?<right>.*)", "EQUAL_RIGHT");
        Rule ltRule = new Rule("(?<left>.*) +< +(?<right>.*)", "LT");
        Rule ltRuleRight = new Rule("(?<left>.*) +< +(?<right>.*)", "LT_RIGHT");
        Rule gtRule = new Rule("(?<left>.*) +> +(?<right>.*)", "GT");
        Rule gtRuleRight = new Rule("(?<left>.*) +> +(?<right>.*)", "GT_RIGHT");
        Rule lteRule = new Rule("(?<left>.*) +<= +(?<right>.*)", "LTE");
        Rule lteRuleRight = new Rule("(?<left>.*) +<= +(?<right>.*)", "LTE_RIGHT");
        Rule gteRule = new Rule("(?<left>.*) +>= +(?<right>.*)", "GTE");
        Rule gteRuleRight = new Rule("(?<left>.*) +>= +(?<right>.*)", "GTE_RIGHT");
        List<Rule> comparisonExpr = new ArrayList<>(
            List.of(
                notEqualRule,
                notEqualRuleRight,
                equalRule,
                equalRuleRight,
                ltRule,
                ltRuleRight,
                gtRule,
                gtRuleRight,
                lteRule,
                lteRuleRight,
                gteRule,
                gteRuleRight
            )
        );

        //// Populate the levels, bottom up
        //<comparison>
        populateBinaryRules(
            mg.exposeEntrypoint(),
            mg.exposeEntrypoint(),
            notEqualRule,
            notEqualRuleRight,
            equalRule,
            equalRuleRight,
            ltRule,
            ltRuleRight,
            gtRule,
            gtRuleRight,
            lteRule,
            lteRuleRight,
            gteRule,
            gteRuleRight
        );
        //<root>
        boolParenRule.addChildren("inner", orExpr);
        boolVarRule.addChildren("var", vg.exposeEntrypoint());

        //<not_expr>
        notRule.addChildren("inner", boolRootExpr);
        notDownRuleToRoot.addChildren("inner", boolRootExpr);
        notDownRuleToCmp.addChildren("inner", comparisonExpr);

        //<and_expr>
        populateBinaryRules(andExpr, notExpr, andRule, andRuleRight);
        andDownRule.addChildren("inner", notExpr);

        //<or_expr>
        populateBinaryRules(orExpr, andExpr, orRule, orRuleRight);
        orDownRule.addChildren("inner", andExpr);
        levels.addAll(List.of(orExpr, andExpr, notExpr, boolRootExpr, comparisonExpr));
    }
}
