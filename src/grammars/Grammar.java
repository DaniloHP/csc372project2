package grammars;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Grammar {

    public static final Set<String> RESERVED_KEYWORDS = new HashSet<>(
        List.of("let", "if", "elf", "else", "argos", "hallpass", "out", "for", "loop", "T", "F")
    );
    protected final List<List<Rule>> levels;
    //Rules that show up in a lot of grammars. Paren rule could also be here
    protected static final Rule BASE_DOWN_RULE = new Rule("(?<inner>.*)", "DOWN_RULE");
    protected static final Rule INT_RULE = new Rule("\\d+", "INTEGERS");
    public static final Rule VAR_RULE = genVarRule();

    protected Grammar() {
        levels = new ArrayList<>();
    }

    private static Rule genVarRule() {
        StringBuilder sb = new StringBuilder("(?!");
        int i = 1;
        for (String keyword : RESERVED_KEYWORDS) {
            sb.append(format("({0}\\b){1}", keyword, i < RESERVED_KEYWORDS.size() ? "|" : ""));
            i++;
        }
        sb.append(")");
        String re = String.format("^ *%s(?<var>[\\w&&[^\\d]][\\w]{0,31}) *", sb);
        return new Rule(re, "VARIABLES");
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
