package grammars;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule {

    protected Pattern regex;
    protected final Map<String, List<Rule>> children;
    protected String id; //for debugging
    protected Map<String, String> replacements;

    /**
     * Builds a new Rule with the given CharSequence as the regex.
     *
     * @param regexStr String that will be used as a regex for this rule's
     *                 validation purposes.
     */
    protected Rule(CharSequence regexStr) {
        children = new HashMap<>();
        regex = Pattern.compile(regexStr.toString());
    }

    /**
     * The most commonly used Rule constructor. If any replacementPairs are given,
     * constructs a HashMap containing them for use in the replace function.
     *
     * @param regexStr     String that will be used as a regex for this rule's
     *                     validation purposes.
     * @param id           A human-readable ID for use in the toString.
     * @param replacePairs An optional list of replacement key-value pairs that
     *                     are relevant to this rule and must be made for Java
     *                     correctness, with the key being the Judo keyword and
     *                     the value being its Java counterpart. For example, the
     *                     modulus rules needs to provide a mapping "mod" -> "%".
     */
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

    /**
     * Copy constructor that basically just takes the other Rule's regex. This is
     * used extensively for making copies of "down rules", Rules whose only
     * purpose is to go down one level.
     *
     * @param other The other Rule whose regex to copy.
     * @param newId The new ID to be used by this new Rule.
     */
    public Rule(Rule other, String newId) {
        this(other.regex.pattern());
        this.id = newId;
    }

    /**
     * Adds the given List of Rules as children of the given named capturing
     * group. For example, a left associative Rule for dealing with addition
     * would take the multiplication, division, modulus, and root as it's
     * right side children.
     *
     * @param groupName The name of the named capturing group with which to
     *                  associate the given children.
     * @param children  The list of children.
     */
    public void addChildren(String groupName, List<Rule> children) {
        this.children.put(groupName, children);
    }

    /**
     * Convenience method used in validate and replace.
     *
     * @param arr A boolean array
     * @return Whether all elements in arr are true.
     */
    protected boolean allTrue(boolean[] arr) {
        for (boolean b : arr) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    /**
     * In many ways, the heart of the grammar. Recursively makes sure that all
     * of this rule's children validate, all the way down to a terminal.
     * @param toCheck The expression to validate.
     * @return Whether the given expression is valid.
     */
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
        }
        return false;
    }

    /**
     * Makes any replacements in the given expression specified in the
     * replacements member.
     * @param toReplace The expression to do replacements in
     * @return The expression, with necessary replacements made, or null if this
     * Rule didn't match the expression.
     */
    public String replace(CharSequence toReplace) {
        Matcher matcher = regex.matcher(toReplace);
        if (toReplace.length() > 0 && matcher.matches()) {
            if (this.isTerminal()) {
                //TODO: clean this up
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

    /**
     * Replaces the capturing group with name groupName with the string
     * replacement, within the string toReplaceIn. It's assumed that the
     * capturing group does exist and the matcher matches.
     * @param toReplaceIn The string to replace in
     * @param groupName The name of the capturing group to replace
     * @param replacement What to replace it with
     * @return toReplaceIn, with the replacement in place.
     */
    protected String replaceGroupName(String toReplaceIn, String groupName, String replacement) {
        Matcher m = regex.matcher(toReplaceIn);
        m.matches();
        int startIndex = m.start(groupName);
        int endIndex = m.end(groupName);
        return new StringBuilder(toReplaceIn).replace(startIndex, endIndex, replacement).toString();
    }

    /**
     * @return Whether this Rule is a terminal, i.e. it has no children
     */
    protected boolean isTerminal() {
        return this.children == null || this.children.isEmpty();
    }

    /**
     * This is specifically here because when debugging in the IntelliJ, it
     * will put the toString() of an object next to it, so this greatly helped
     * with debugging.
     * @return A human-readable string that identifies this rule in the grammar.
     * Not the regex.
     */
    public String toString() {
        return String.format("Rule %s", id);
    }
}
