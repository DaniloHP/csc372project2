package grammars;

import java.util.ArrayList;
import java.util.List;

public class RayGrammar extends Grammar {

    public RayGrammar(BoolGrammar bg, MathGrammar mg, StringGrammar sg) {
        super();
        //<rays>, <int_list>, <string_list>
        Rule rayRule = new Rule("\\[ *(.+?) *\\]", "RAY_RULE");
        List<Rule> rayExpr = new ArrayList<>(List.of(rayRule));

        Rule intRayRule = new Rule(" *(?<curr>.*?) *, *(?<rest>.*)", "INT_LIST");
        List<Rule> intRayExpr = new ArrayList<>(List.of(intRayRule));

        Rule boolRayRule = new Rule(" *(?<curr>.*?) *, *(?<rest>.*)", "BOOL_LIST");
        List<Rule> boolRayExpr = new ArrayList<>(List.of(boolRayRule));

        Rule strRayRule = new Rule(" *(?<curr>.*?) *, *(?<rest>.*)", "STRING_LIST");
        List<Rule> strRayExpr = new ArrayList<>(List.of(strRayRule));

        rayRule.addChildren(1, List.of(intRayRule, boolRayRule, strRayRule));
        populateBinaryRules(mg.exposeEntrypoint(), intRayExpr, intRayRule);
        populateBinaryRules(bg.exposeEntrypoint(), boolRayExpr, boolRayRule);
        populateBinaryRules(sg.exposeEntrypoint(), strRayExpr, strRayRule);
        this.levels.addAll(List.of(rayExpr, boolRayExpr, intRayExpr, strRayExpr));
    }
}
