package grammars;

import java.util.ArrayList;
import java.util.List;

public class StringGrammar extends Grammar {

    //This very simple grammar allows string literals and variables, no string
    //concatenation.
    public StringGrammar() {
        super();
        Rule strLiteralRule = new Rule("\\\"[\\p{Print}&&[^\\\"]]*?\\\"", "STR_LITERAL");
        List<Rule> strExpr = new ArrayList<>(List.of(strLiteralRule, varRule));
        this.levels.add(strExpr);
    }
}
