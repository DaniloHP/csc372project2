package grammars;

import java.util.ArrayList;
import java.util.List;

public abstract class Grammar {

    protected final List<List<Rule>> levels;
    //Rules that show up in a lot of grammars. Paren rule could also be here
    protected final Rule baseDownRule = new Rule("(?<inner>.*)", "DOWN_RULE");
    protected final Rule varRule = new Rule("[\\w&&[^\\d]]+[\\w]*", "VARIABLES");
    protected final Rule intRule = new Rule("\\d+", "INTEGERS");

    protected Grammar() {
        levels = new ArrayList<>();
    }

    public boolean isValid(CharSequence toCheck) {
        for (Rule r : levels.get(0)) {
            if (r.validate(toCheck)) {
                return true;
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
