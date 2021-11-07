package grammars;

import java.util.ArrayList;
import java.util.List;

public class RayGrammar extends Grammar {

    public RayGrammar() {
        super();
        //<rays>, <int_list>, <string_list>
        Rule intRayRule = new Rule(
            "\\[( *[\\d|[\\w&&[^\\d]]+[\\w]*]* *, *)* *([\\d|[\\w&&[^\\d]]+[\\w]*]+) *\\]",
            "INT_LIST"
        );
        Rule strRayRule = new Rule(
            "\\[ *((( *\\\"\\p{Print}*\\\" *)|( *[\\w&&[^\\d]]+[\\w]*) *) *, *)*((\\\"\\p{Print}*\\\")|([a-zA-Z_]+[a-zA-Z_\\d]+)) *\\]",
            "STR_LIST"
        );

        //Grammar Levels for statements
        List<Rule> rayExpr = new ArrayList<>(List.of(intRayRule, strRayRule));

        levels.addAll(List.of(rayExpr));
    }
}
