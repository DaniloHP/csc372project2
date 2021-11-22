package grammars;

import java.util.ArrayList;
import java.util.List;
import parser.errors.TypeError;

public abstract class Grammar {

    protected final List<List<Rule>> levels;
    //Rules that show up in a lot of grammars. Paren rule could also be here
    protected static final Rule BASE_DOWN_RULE = new Rule("(?<inner>.*)", "DOWN_RULE");
    protected static final Rule INT_RULE = new Rule("\\d+", "INTEGERS");
    public static final VarRule VAR_RULE = new VarRule(
        "^ *(?<var>[\\w&&[^\\d]][\\w]{0,31}) *",
        "VAR"
    );

    protected Grammar() {
        levels = new ArrayList<>();
    }

    /**
     * Validates the given expression under this grammar. As soon as ANY rule
     * fully validates toCheck, we return true.
     * @param toCheck The expression to validate.
     * @return true if the given sequence is valid under the specific Grammar
     * this method was called under, false otherwise. This method is very likely
     * to cause exceptions to be thrown if the expressions contains variables
     * that are being misused or do not exist.
     */
    public boolean validate(CharSequence toCheck) {
        for (Rule r : levels.get(0)) {
            if (r.validate(toCheck)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does the same work as validate, but will not throw any TypeErrors.
     * VariableErrors will still be thrown if an unknown variable is used and
     * VarRule has a non-null ScopeStack.
     * @param toCheck The expression to validate.
     * @return true if the given sequence is valid under the specific Grammar
     * this method was called under, false otherwise
     */
    public boolean validateNoThrow(CharSequence toCheck) {
        for (Rule r : levels.get(0)) {
            try {
                if (r.validate(toCheck)) {
                    return true;
                }
            } catch (TypeError e) {
                continue;
            }
        }
        return false;
    }

    /**
     * Exposes the top level list of rules which other grammars can "plug into"
     * if they depend on it. For example, the boolean grammar depends on the
     * math grammar for parsing arbitrary integer expression in comparisons, so
     * it uses this method.
     * @return The top level list of rules. I.E, for the math grammar, it would
     * be the level that includes the addition and subtraction rules. From this
     * level, you can reach the entire rest of the grammar.
     */
    public List<Rule> exposeEntrypoint() {
        return this.levels.get(0);
    }

    /**
     * Convenience method for populating the children of binary rules such as
     * +,-,*, etc.
     * @param left List of rules that should be the children of the left side
     *             of the expression
     * @param right List of rules that should be the children of the right side
     *              of the expression
     * @param rules The binary rules to add these children to.
     */
    protected void populateBinaryRules(List<Rule> left, List<Rule> right, Rule... rules) {
        for (Rule rule : rules) {
            rule.addChildren("left", left);
            rule.addChildren("right", right);
        }
    }

    /**
     * Entrypoint for converting Judo's special keywords like and, or, T, F, mod
     * into their java equivalents. It is expected that the expressions passed
     * to this function have already bee validated.
     * @param toReplace The expression to do replacements within
     * @return The same expression semantically, but with Judo keywords
     * intelligently substituted for their Java equivalents. null if the
     * expression isn't actually valid, probably.
     */
    public String keywordsToJava(CharSequence toReplace) {
        for (Rule r : levels.get(0)) {
            String replaced = r.replace(toReplace);
            if (replaced != null) {
                return replaced;
            }
        }
        return null;
    }
}
