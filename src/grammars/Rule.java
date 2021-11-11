package grammars;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule implements Cloneable {

    protected Pattern regex;
    protected Map<String, List<Rule>> children;
    public String id; //for debugging

    public Rule(CharSequence regexStr) {
        children = new HashMap<>();
        regex = Pattern.compile(regexStr.toString());
    }

    public Rule(CharSequence regexStr, String id) {
        this(regexStr);
        this.id = id;
    }

    public void addChildren(String group, List<Rule> level) {
        if (children == null) children = new HashMap<>();
        children.put(group, level);
    }

    protected boolean allTrue(boolean[] arr) {
        for (boolean b : arr) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public boolean validate(CharSequence toCheck) {
        Matcher matcher = regex.matcher(toCheck);
        if (!toCheck.isEmpty() && matcher.matches()) {
            if (children.isEmpty()) {
                //it's a matching terminal
                return true;
            }
            var resultVector = new boolean[children.size()];
            int i = 0;
            for (String k : children.keySet()) {
                String currGroup = matcher.group(k).strip();
                for (Rule rule : children.get(k)) {
                    if (rule.validate(currGroup)) {
                        resultVector[i] = true;
                    }
                }
                i++;
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
            clone.children = new HashMap<>();
            clone.id = this.id;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String toString() {
        return String.format("Rule %s", id);
    }
}
