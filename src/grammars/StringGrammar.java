package grammars;

import java.util.Arrays;
import java.util.List;
import parser.Type;

public class StringGrammar extends Grammar {

    //This very simple grammar allows string literals and variables, no string
    //concatenation.
    public StringGrammar(VarGrammar vg) {
        super();
        Rule strLiteralRule = new Rule("\\\"[\\p{Print}&&[^\\\"]]*?\\\"", "STR_LITERAL");
        VarRule strVarRule = new VarRule(VAR_RULE, "STR_VAR");
        strVarRule.useType(Type.STRING);
        List<Rule> strExpr = Arrays.asList(strLiteralRule, strVarRule);
        strVarRule.addChildren("var", vg.exposeEntrypoint());
        this.levels.add(strExpr);
    }
}
