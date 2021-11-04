package grammars;

import java.util.ArrayList;
import java.util.List;

public class BooleanGrammar extends Grammar {

    /**
     * BooleanGrammar depends on MathGrammar for comparisons, so it's entrypoint
     * *must* be provided.
     * @param mathEntryPoint The correct place to start parsing a full math
     *                       expression.
     */
    public BooleanGrammar(List<Rule> mathEntryPoint) {
        super();
        //<or_expr>
        Rule orRule = new Rule("(.*) +or +(.*)", "OR");
        Rule orRuleRight = new Rule("(.*?) +or +(.*)", "OR_RIGHT");
        Rule orDownRule = baseDownRule.clone();
        orDownRule.id += "_OR";
        List<Rule> orExpr = new ArrayList<>(List.of(orRule, orRuleRight, orDownRule));

        //<and_expr>
        Rule andRule = new Rule("(.*) +and +(.*)", "AND");
        Rule andRuleRight = new Rule("(.*?) +and +(.*)", "AND_RIGHT");
        Rule andDownRule = baseDownRule.clone();
        andDownRule.id += "_AND";
        List<Rule> andExpr = new ArrayList<>(List.of(andRule, andRuleRight, andDownRule));

        //<not_expr>
        Rule notRule = new Rule("not +(.*)");
        Rule notDownRuleToRoot = baseDownRule.clone();
        notDownRuleToRoot.id += "_TO_ROOT";
        Rule notDownRuleToCmp = baseDownRule.clone();
        notDownRuleToRoot.id += "_TO_CMP";
        List<Rule> notExpr = new ArrayList<>(List.of(notRule, notDownRuleToRoot, notDownRuleToCmp));

        //<bool>
        Rule bool = new Rule("[TF]", "BOOL");
        Rule boolParenRule = new Rule("\\((.*)\\)", "PARENTHESES");
        List<Rule> boolRootExpr = new ArrayList<>(List.of(bool, varRule, boolParenRule));

        //<comparison>: !=, ==, <, <=, >, >=
        Rule notEqualRule = new Rule("(.*) +!= +(.*)", "NOT_EQUAL");
        Rule notEqualRuleRight = new Rule("(.*?) +!= +(.*)", "NOT_EQUAL_RIGHT");
        Rule equalRule = new Rule("(.*) +== +(.*)", "EQUAL");
        Rule equalRuleRight = new Rule("(.*?) +== +(.*)", "EQUAL_RIGHT");
        Rule ltRule = new Rule("(.*) +< +(.*)", "LT");
        Rule ltRuleRight = new Rule("(.*?) +< +(.*)", "LT_RIGHT");
        Rule gtRule = new Rule("(.*) +> +(.*)", "GT");
        Rule gtRuleRight = new Rule("(.*?) +> +(.*)", "GT_RIGHT");
        Rule lteRule = new Rule("(.*) +<= +(.*)", "LTE");
        Rule lteRuleRight = new Rule("(.*?) +<= +(.*)", "LTE_RIGHT");
        Rule gteRule = new Rule("(.*) +>= +(.*)", "GTE");
        Rule gteRuleRight = new Rule("(.*?) +>= +(.*)", "GTE_RIGHT");
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
            mathEntryPoint,
            mathEntryPoint,
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
        boolParenRule.addChildren(1, orExpr);

        //<not_expr>
        notRule.addChildren(1, boolRootExpr);
        notDownRuleToRoot.addChildren(1, boolRootExpr);
        notDownRuleToCmp.addChildren(1, comparisonExpr);

        //<and_expr>
        populateBinaryRules(andExpr, notExpr, andRule, andRuleRight);
        andDownRule.addChildren(1, notExpr);

        //<or_expr>
        populateBinaryRules(orExpr, andExpr, orRule, orRuleRight);
        orDownRule.addChildren(1, andExpr);
        levels.addAll(List.of(orExpr, andExpr, notExpr, boolRootExpr, comparisonExpr));
    }
}
