package grammars;

import java.util.List;
import parser.Type;

public class VarGrammar extends Grammar {

    public VarGrammar() {
        Rule varRule = new VarRule(VAR_RULE, "VAR");
        List<Rule> varStmt = List.of(varRule);
        this.levels.add(varStmt);
    }

    public List<Rule> exposeEntrypoint(Type expected) {
        VarRule specificRule = new VarRule(VAR_RULE, "VAR");
        specificRule.useType(expected);
        return List.of(specificRule);
    }
}
