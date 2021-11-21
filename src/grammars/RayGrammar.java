package grammars;

import static java.text.MessageFormat.format;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import parser.Type;
import parser.errors.InvalidStatementError;
import parser.errors.TypeError;

public class RayGrammar extends Grammar {

    private final Pattern rayUnwrap;
    private final RayRule intRayRule;
    private final RayRule boolRayRule;
    private final RayRule strRayRule;

    /**
     * Builds a RayGrammar as so:
     * <ray>         ::= [<int_list>]  |  [<string_list>]  |  [<bool_list>]
     * <int_list>    ::= <integer>,<int_list>  |  <var>,<int_list>  |  <integer>  |  <var>
     * <string_list> ::= <string_literal>,<string_list>  |  <var>,<string_list>  |  <string_literal>  |  <var>
     * <bool_list>   ::= <bool>,<bool_list>  |  <var>,<bool_list>  |  <bool> | <var>
     * This grammar only deals with array literals.
     * @param bg BoolGrammar, used for validating rays of booleans.
     * @param mg MathGrammar, used for validating rays of ints.
     * @param sg StringGrammar, used for validating rays of Strings.
     */
    public RayGrammar(BoolGrammar bg, MathGrammar mg, StringGrammar sg) {
        super();
        //<rays>, <int_list>, <string_list>
        this.rayUnwrap = Pattern.compile("\\[ *(?<ray>.+?) *\\]");
        String rayRe = "( *(?<curr>.+?) *(, *(?<rest>.*)))|( *(?<last>.+?) *)";
        this.intRayRule = new RayRule(rayRe, "INT_LIST");
        List<Rule> intRayExpr = Collections.singletonList(intRayRule);

        this.boolRayRule = new RayRule(rayRe, "BOOL_LIST");
        List<Rule> boolRayExpr = Collections.singletonList(boolRayRule);

        this.strRayRule = new RayRule(rayRe, "STRING_LIST");
        List<Rule> strRayExpr = Collections.singletonList(strRayRule);

        populateRayRules(mg.exposeEntrypoint(), intRayExpr, intRayRule);
        populateRayRules(bg.exposeEntrypoint(), boolRayExpr, boolRayRule);
        populateRayRules(sg.exposeEntrypoint(), strRayExpr, strRayRule);
    }

    /**
     * Implements the special logic of the RayGrammar's validation. Will cause
     * an InvalidStatementError if the expression does not validate as an int,
     * bool, or string ray.
     * @param toCheck The expression to validate.
     * @return true if the expression is valid, throws otherwise.
     */
    @Override
    public boolean validate(CharSequence toCheck) {
        return this.categorize(toCheck, true) != null;
    }

    /**
     * Implements the special logic of the RayGrammar's validation. Will return
     * false if the expression does not validate as an int, bool, or string ray.
     * @param toCheck The expression to validate.
     * @return true if the expression is valid, false otherwise.
     */
    @Override
    public boolean validateNoThrow(CharSequence toCheck) {
        return this.categorize(toCheck, false) != null;
    }

    /**
     * @param toCheck The ray literal to categorize
     * @return the Type of the given ray literal, or null if it did not validate
     * under any grammar.
     */
    public Type categorizeNoThrow(CharSequence toCheck) {
        return this.categorize(toCheck, false);
    }

    /**
     * @param toCheck The ray literal to categorize
     * @return the Type of the given ray literal, or throws an
     * InvalidStatementError if it did not validate under any grammar.
     */
    public Type categorize(CharSequence toCheck) {
        return this.categorize(toCheck, true);
    }

    /**
     * Attempts to categorize the given ray literal into a Type.
     * @param toCheck The ray literal to check
     * @param doThrow Whether to throw an InvalidStatementError if the ray didn't
     *                validate under any grammar. Will still throw VariableError
     *                if an unknown variable is used and the VarRule has a
     *                ScopeStack. Will NOT throw TypeErrors no matter what.
     * @return The Type category that fits the given ray, or null if doThrow is
     * false. Otherwise, throws InvalidStatementError.
     */
    private Type categorize(CharSequence toCheck, boolean doThrow) {
        Type found = null;
        Matcher m = rayUnwrap.matcher(toCheck);
        if (m.matches()) {
            String unwrapped = m.group("ray");
            try {
                if (this.intRayRule.validate(unwrapped)) {
                    found = Type.INT_LIST;
                }
            } catch (TypeError e) {}
            try {
                if (this.boolRayRule.validate(unwrapped)) {
                    if (found != null) {
                        throw new TypeError(format("Is this possible?"));
                    }
                    found = Type.BOOL_LIST;
                }
            } catch (TypeError e) {}
            try {
                if (this.strRayRule.validate(unwrapped)) {
                    if (found != null) {
                        throw new TypeError(format("Is this possible?"));
                    }
                    found = Type.STRING_LIST;
                }
            } catch (TypeError e) {}
        }
        if (found == null && doThrow) {
            throw new InvalidStatementError(
                format("Invalid syntax or mixed types in ray declaration: {0}", toCheck)
            );
        }
        return found;
    }

    /**
     * Convenience method for populating RayRules.
     * @param curr The list of rules to use as children for evaluating the current
     *             item in the ray.
     * @param rest The list of rules to use as children for evaluating the rest
     *             of the items in the ray.
     * @param rules The rules to populate.
     */
    private void populateRayRules(List<Rule> curr, List<Rule> rest, Rule... rules) {
        if (rules != null) {
            for (Rule r : rules) {
                r.addChildren("curr", curr);
                r.addChildren("rest", rest);
                r.addChildren("last", curr);
            }
        }
    }
}
