package grammars;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule {

    protected Pattern regex;
    protected Map<String, List<Rule>> children;
    protected String id; //for debugging
    protected Map<String, String> replacements;

    public Rule(CharSequence regexStr) {
        children = new HashMap<>();
        regex = Pattern.compile(regexStr.toString());
    }

    @SafeVarargs
    public Rule(CharSequence regexStr, String id, SimpleEntry<String, String>... replacePairs) {
        this(regexStr);
        this.id = id;
        if (replacePairs != null && replacePairs.length > 0) {
            this.replacements = new HashMap<>();
            for (SimpleEntry<String, String> pair : replacePairs) {
                this.replacements.put(pair.getKey(), pair.getValue());
            }
        }
    }

    public Rule(Rule other, String newId) {
        this(other.regex.pattern());
        this.id = newId;
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

    protected boolean allNonNull(Object[] arr) {
        for (Object o : arr) {
            if (o == null) {
                return false;
            }
        }
        return true;
    }

    public boolean validate(CharSequence toCheck) {
        Matcher matcher = regex.matcher(toCheck);
        if (toCheck.length() > 0 && matcher.matches()) {
            if (this.isTerminal()) {
                //it's a matching terminal
                return true;
            }
            boolean[] resultVector = new boolean[children.size()];
            int i = 0;
            for (String k : children.keySet()) {
                String currGroup = matcher.group(k).trim();
                for (Rule rule : children.get(k)) {
                    if (rule.validate(currGroup)) {
                        resultVector[i] = true;
                    }
                    //I'm not worried about the performance implications of calling
                    //allTrue every iteration because resultVector is at most len 2
                    if (allTrue(resultVector)) {
                        return true;
                    }
                }
                i++;
            }
            return false;
        } else {
            return false;
        }
    }

    public String replace(CharSequence toReplace) {
        Matcher matcher = regex.matcher(toReplace);
        if (toReplace.length() > 0 && matcher.matches()) {
            if (this.isTerminal()) {
                if (
                    this.replacements != null && this.replacements.containsKey(toReplace.toString())
                ) {
                    return this.replacements.get(toReplace.toString());
                }
                return toReplace.toString();
            }
            int i = 0;
            boolean[] resultVector = new boolean[children.size()];
            String replacedCopy = toReplace.toString();
            for (String groupName : children.keySet()) {
                String currGroup = matcher.group(groupName).trim();
                toReplace = replacedCopy;
                for (Rule rule : children.get(groupName)) {
                    String childReplaced = rule.replace(currGroup);
                    if (childReplaced != null) {
                        replacedCopy =
                            replaceGroupName(toReplace.toString(), groupName, childReplaced);
                        resultVector[i] = true;
                    }
                    if (allTrue(resultVector)) {
                        break;
                    }
                }
                i++;
            }
            if (this.replacements != null) {
                String group = matcher.group("replaceMe").trim();
                if (this.replacements.containsKey(group)) {
                    replacedCopy =
                        replaceGroupName(replacedCopy, "replaceMe", this.replacements.get(group));
                }
            }
            return allTrue(resultVector) ? replacedCopy : null;
        }
        return null;
    }

    protected String replaceGroupName(String toReplaceIn, String groupName, String replacement) {
        Matcher m = regex.matcher(toReplaceIn);
        m.matches();
        int startIndex = m.start(groupName);
        int endIndex = m.end(groupName);
        return new StringBuilder(toReplaceIn).replace(startIndex, endIndex, replacement).toString();
    }

    protected boolean isTerminal() {
        return this.children == null || this.children.isEmpty();
    }

    public String toString() {
        return String.format("Rule %s", id);
    }
}
