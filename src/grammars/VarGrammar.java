package grammars;

import java.util.List;
import java.util.regex.Pattern;

public class VarGrammar extends Grammar {

    public VarGrammar() {
        Rule varRule = VAR_RULE.clone();
        List<Rule> varStmt = List.of(varRule);
        this.levels.add(varStmt);
    }
}
