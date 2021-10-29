package grammars;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule implements Cloneable {

    private GrammarLevel[] children;
    private Pattern regex;
    private final int NUM_GROUPS;
    public String id; //for debugging

    public Rule(CharSequence regexStr) {
        regex = Pattern.compile(regexStr.toString());
        NUM_GROUPS = regex.matcher("").groupCount();
        children = new GrammarLevel[NUM_GROUPS];
    }

    public Rule(CharSequence regexStr, String id) {
        this(regexStr);
        this.id = id;
    }

    public void addChildren(int group, GrammarLevel level) {
        children[group - 1] = level;
    }

    private boolean allTrue(boolean[] arr) {
        for (var b : arr) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public boolean validate(CharSequence toCheck) {
        if (toCheck.isEmpty()) {
            return false;
        }
        Matcher matcher = regex.matcher(toCheck);
        if (matcher.matches()) {
            if (children.length == 0) {
                //it's a matching terminal
                return true;
            }
            var resultVector = new boolean[NUM_GROUPS];
            for (int groupNum = 1; groupNum <= NUM_GROUPS; groupNum++) {
                int i = groupNum - 1;
                String currGroup = matcher.group(groupNum).strip();
                children[i].forEachRule(rule -> {
                        if (rule.validate(currGroup)) {
                            resultVector[i] = true;
                        }
                    });
            }
            return allTrue(resultVector);
        } else {
            return false;
        }
    }

    /**
     * SHOULD ONLY EVER BE USED WITH RULES THAT HAVE NO CHILDREN YET
     */
    @Override
    public Rule clone() {
        try {
            Rule clone = (Rule) super.clone();
            clone.regex = this.regex;
            clone.children = new GrammarLevel[NUM_GROUPS];
            clone.id = this.id;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String toString() {
        return String.format("grammars.Rule %s", id);
    }
}
