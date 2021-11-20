package grammars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class RayRule extends Rule {

    public RayRule(CharSequence regexStr) {
        super(regexStr);
    }

    public RayRule(CharSequence regexStr, String id) {
        super(regexStr, id);
    }

    //m should be armed already
    private String groupOrNull(Matcher m, String group) {
        String ret;
        try {
            ret = m.group(group);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return ret;
    }

    @Override
    public boolean validate(CharSequence toCheck) {
        Matcher matcher = regex.matcher(toCheck);
        if (!toCheck.isEmpty() && matcher.matches()) {
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
            for (var val : groups.keySet()) {
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

    @Override
    public String toString() {
        return String.format("RayRule %s", id);
    }
}
