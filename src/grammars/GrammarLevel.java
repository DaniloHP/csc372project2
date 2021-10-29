package grammars;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GrammarLevel {
    List<Rule> rules;

    public GrammarLevel() {
        rules = new ArrayList<>();
    }

    public GrammarLevel(Rule... rules) {
        this();
        this.rules.addAll(List.of(rules));
        assert rules.length > 0;
    }

    public boolean validate(CharSequence toCheck) {
        for (Rule rule : rules) {
            if (rule.validate(toCheck)) {
                return true;
            }
        }
        return false;
    }

    public void forEachRule(Consumer<? super Rule> fn) {
        rules.forEach(fn);
    }

    public void addRule(Rule... rule) {
        rules.addAll(List.of(rule));
    }
}
