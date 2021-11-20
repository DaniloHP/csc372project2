package grammars;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import parser.Type;
import parser.errors.InvalidStatementError;
import parser.errors.TypeError;

public class RayGrammar extends Grammar {

    private Pattern rayUnwrap;
    private RayRule intRayRule, boolRayRule, strRayRule;

    public RayGrammar(BoolGrammar bg, MathGrammar mg, StringGrammar sg) {
        super();
        //<rays>, <int_list>, <string_list>
        this.rayUnwrap = Pattern.compile("\\[ *(?<ray>.+?) *\\]");
        String rayRe = "( *(?<curr>.+?) *(, *(?<rest>.*)))|( *(?<last>.+?) *)";
        this.intRayRule = new RayRule(rayRe, "INT_LIST");
        List<Rule> intRayExpr = new ArrayList<>(List.of(intRayRule));

        this.boolRayRule = new RayRule(rayRe, "BOOL_LIST");
        List<Rule> boolRayExpr = new ArrayList<>(List.of(boolRayRule));

        this.strRayRule = new RayRule(rayRe, "STRING_LIST");
        List<Rule> strRayExpr = new ArrayList<>(List.of(strRayRule));

        populateRayRules(mg.exposeEntrypoint(), intRayExpr, intRayRule);
        populateRayRules(bg.exposeEntrypoint(), boolRayExpr, boolRayRule);
        populateRayRules(sg.exposeEntrypoint(), strRayExpr, strRayRule);
    }

    @Override
    public boolean validate(CharSequence toCheck) {
        return this.categorize(toCheck, true) != null;
    }

    @Override
    public boolean validateNoThrow(CharSequence toCheck) {
        return this.categorize(toCheck, false) != null;
    }

    public Type categorizeNoThrow(CharSequence toCheck) {
        return this.categorize(toCheck, false);
    }

    public Type categorize(CharSequence toCheck) {
        return this.categorize(toCheck, true);
    }

    private Type categorize(CharSequence toCheck, boolean doThrow) {
        Type found = null;
        Matcher m = rayUnwrap.matcher(toCheck);
        int numTypeErrors = 0;
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
