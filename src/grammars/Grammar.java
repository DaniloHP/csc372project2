package grammars;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import parser.errors.TypeError;

public abstract class Grammar {

    protected final List<List<Rule>> levels;
    //Rules that show up in a lot of grammars. Paren rule could also be here
    protected static final Rule BASE_DOWN_RULE = new Rule("(?<inner>.*)", "DOWN_RULE");
    protected static final Rule INT_RULE = new Rule("\\d+", "INTEGERS");
    public static final VarRule VAR_RULE = new VarRule(
        "^ *(?<var>[\\w&&[^\\d]][\\w]{0,31}) *",
        "VAR"
    );

    protected Grammar() {
        levels = new ArrayList<>();
    }

    public boolean validate(CharSequence toCheck) {
        for (Rule r : levels.get(0)) {
            if (r.validate(toCheck)) {
                return true;
            }
        }
        return false;
    }

    public boolean validateNoThrow(CharSequence toCheck) {
        for (Rule r : levels.get(0)) {
            try {
                if (r.validate(toCheck)) {
                    return true;
                }
            } catch (TypeError e) {
                continue;
            }
        }
        return false;
    }

    public List<Rule> exposeEntrypoint() {
        return this.levels.get(0);
    }

    protected void populateBinaryRules(List<Rule> left, List<Rule> right, Rule... rules) {
        for (var rule : rules) {
            rule.addChildren("left", left);
            rule.addChildren("right", right);
        }
    }
}
