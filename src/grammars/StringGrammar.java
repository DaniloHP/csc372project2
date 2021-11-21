package grammars;

import java.util.Arrays;
import java.util.List;
import parser.Type;

public class StringGrammar extends Grammar {

    /**
     * This very simple grammar allows string literals and variables, no string
     * concatenation. Uses Java's \p{Print} character class, which matches all
     * printable ASCII characters, but no unicode or ascii control characters,
     * including \t and \n.
     * @param vg The VarGrammar on which all other grammars depend for making
     *           sure that variables within expressions exist and are of the
     *           right type.
     */
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
