package grammars;

import java.util.ArrayList;
import java.util.List;

public class RayGrammar extends Grammar {

    public RayGrammar(BoolGrammar bg, MathGrammar mg, StringGrammar sg) {
        super();
        //<rays>, <int_list>, <string_list>
        Rule rayRule = new Rule("\\[ *(?<ray>.+?) *\\]", "RAY_RULE");
        List<Rule> rayExpr = new ArrayList<>(List.of(rayRule));
        Rule intRayRule = new RayRule(
            "( *(?<curr>.+?) *(, *(?<rest>.*)))|( *(?<last>.+?) *)",
            "INT_LIST"
        );
        List<Rule> intRayExpr = new ArrayList<>(List.of(intRayRule));

        Rule boolRayRule = new RayRule(
            "( *(?<curr>.+?) *(, *(?<rest>.*)))|( *(?<last>.+?) *)",
            "BOOL_LIST"
        );
        List<Rule> boolRayExpr = new ArrayList<>(List.of(boolRayRule));

        Rule strRayRule = new RayRule(
            "( *(?<curr>.+?) *(, *(?<rest>.*)))|( *(?<last>.+?) *)",
            "STRING_LIST"
        );
        List<Rule> strRayExpr = new ArrayList<>(List.of(strRayRule));

        rayRule.addChildren("ray", List.of(intRayRule, boolRayRule, strRayRule));
        populateRayRules(mg.exposeEntrypoint(), intRayExpr, intRayRule);
        populateRayRules(bg.exposeEntrypoint(), boolRayExpr, boolRayRule);
        populateRayRules(sg.exposeEntrypoint(), strRayExpr, strRayRule);
        this.levels.addAll(List.of(rayExpr, boolRayExpr, intRayExpr, strRayExpr));
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
