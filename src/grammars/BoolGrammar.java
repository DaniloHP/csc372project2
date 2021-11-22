package grammars;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import parser.Type;

/**
 * Our boolean grammar:
 * <pre>
 &lt;or_expr&gt;   ::= &lt;or_expr&gt; or &lt;and_expr&gt; | &lt;and_expr&gt;
 &lt;and_expr&gt;  ::= &lt;and_expr&gt; and &lt;not_expr&gt; | &lt;not_expr&gt;
 &lt;not_expr&gt;  ::= not &lt;bool_root&gt; | &lt;bool_root&gt; | &lt;comparison&gt;
 &lt;bool_root&gt; ::= &lt;boolean&gt; | (&lt;or_expr&gt;)
 &lt;boolean&gt;   ::= T | F
 * </pre>
 * The above lines will render correctly in a browser (JavaDoc), but look
 * terrible in source code. See this Grammar's constructor for a more readable
 * grammar in source code.
 */
public class BoolGrammar extends Grammar {

    /**
     * Constructs a boolean grammar as so:
     * <or_expr>   ::= <or_expr> or <and_expr> | <and_expr>
     * <and_expr>  ::= <and_expr> and <not_expr> | <not_expr>
     * <not_expr>  ::= not <bool_root> | <bool_root> | <comparison>
     * <bool_root> ::= <boolean> | (<or_expr>)
     * <boolean>   ::= T | F
     * @param mg MathGrammar which the boolean grammar depends on for arbitrary
     *           comparisons.
     * @param vg The VarGrammar on which all other grammars depend for making
     *           sure that variables within expressions exist and are of the
     *           right type.
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
        List<Rule> orExpr = Arrays.asList(orRule, orRuleRight, orDownRule);

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
        List<Rule> andExpr = Arrays.asList(andRule, andRuleRight, andDownRule);

        //<not_expr>
        Rule notRule = new Rule(
            "(?<replaceMe>not) +(?<inner>.*)",
            "UNARY NOT",
            new SimpleEntry<>("not", "!")
        );
        Rule notDownRuleToRoot = new Rule(BASE_DOWN_RULE, "DOWN_TO_ROOT");
        Rule notDownRuleToCmp = new Rule(BASE_DOWN_RULE, "DOWN_TO_CMP");
        List<Rule> notExpr = Arrays.asList(notRule, notDownRuleToRoot, notDownRuleToCmp);

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
        List<Rule> boolRootExpr = Arrays.asList(bool, boolParenRule, boolVarRule);

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
        List<Rule> comparisonExpr = Arrays.asList(
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
        levels.addAll(Arrays.asList(orExpr, andExpr, notExpr, boolRootExpr, comparisonExpr));
    }
}
