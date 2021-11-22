package grammars;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class RayRule extends Rule {

    public RayRule(CharSequence regexStr) {
        super(regexStr);
    }

    public RayRule(CharSequence regexStr, String id) {
        super(regexStr, id);
    }

    /**
     * Attempts to extract the group with the given name from the given matcher,
     * or returns null if the group doesn't exist.
     * @param m An "armed" matcher, that is, it's groups have already been
     *          calculated. Failing to arm the matcher will cause .group() calls
     *          to throw IllegalStateException.
     * @param groupName The name of the named capturing group to attempt to
     *                  extract.
     * @return The named capturing that pertains to the given groupName, or null
     * if such a group doesn't exist in m.
     */
    private String groupOrNull(Matcher m, String groupName) {
        String ret;
        try {
            ret = m.group(groupName);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return ret;
    }

    /**
     * Specialized validating logic for RayRules. Essentially, this function
     * validates a ray literal one element at a time, recursively removing the
     * head element until there are none left. The current "head" element is in
     * the capturing group "curr" and the rest of the ray in "rest". The pattern
     * is such that once there is only one element being evaluated, only the
     * "last" capturing group will be populated.
     * @param toCheck The ray literal to check, WITHOUT surrounding [] square
     *                brackets.
     * @return Whether the given literal is valid.
     */
    @Override
    public boolean validate(CharSequence toCheck) {
        Matcher matcher = regex.matcher(toCheck);
        if (toCheck.length() > 0 && matcher.matches()) {
            boolean[] resultVector;
            Map<String, String> groups = new HashMap<>();
            String lastGroup = groupOrNull(matcher, "last");
            String currGroup = groupOrNull(matcher, "curr");
            String restGroup = groupOrNull(matcher, "rest");
            if (lastGroup == null && currGroup != null && restGroup != null) {
                resultVector = new boolean[2];
                groups.put("curr", currGroup); //the current item
                groups.put("rest", restGroup); //the rest of the list, which will
                //be validated recursively.
            } else if (lastGroup != null && currGroup == null && restGroup == null) {
                //means we're down to validating the last item in the list.
                resultVector = new boolean[1]; //a little ridiculous I know
                groups.put("last", lastGroup);
            } else {
                throw new IllegalStateException(
                    "This shouldn't be possible, check the ray grammar!"
                );
            }
            int i = 0;
            for (String val : groups.keySet()) {
                String group = groups.get(val);
                for (Rule rule : children.get(val)) {
                    if (rule.validate(group)) {
                        resultVector[i] = true;
                    }
                    //cannot return false if the above isn't true.
                }
                i++;
            }
            return allTrue(resultVector);
        } else {
            return false;
        }
    }

    /**
     * @return String representation using "RayRule" instead of "Rule".
     */
    @Override
    public String toString() {
        return String.format("RayRule %s", id);
    }
}
