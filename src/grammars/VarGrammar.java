package grammars;

import java.util.Collections;
import java.util.List;
import parser.Type;

public class VarGrammar extends Grammar {

    /**
     * Constructs a VarGrammar specifically with no expected type.
     */
    public VarGrammar() {
        VarRule varRule = new VarRule(VAR_RULE, "VAR");
        varRule.useType(null);
        List<Rule> varStmt = Collections.singletonList(varRule);
        this.levels.add(varStmt);
    }

    /**
     * Special implementation of exposeEntrypoint where you can provide a type
     * you expect to be evaluating. For example, the MathGrammar calls this with
     * Type.INT.
     * @param expected The type to be expecting when evaluating using this
     *                 entrypoint
     * @return A singleton list containing the rule. This is a little ridiculous,
     * but it's to fit into the API specified by Grammar.
     */
    public List<Rule> exposeEntrypoint(Type expected) {
        VarRule specificRule = new VarRule(VAR_RULE, "VAR");
        specificRule.useType(expected);
        return Collections.singletonList(specificRule);
    }
}
