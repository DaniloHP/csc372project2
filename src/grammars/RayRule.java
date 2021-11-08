package grammars;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
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
            List<AbstractMap.SimpleEntry<String, String>> groups = new ArrayList<>();
            String lastGroup = groupOrNull(matcher, "last");
            String currGroup = groupOrNull(matcher, "curr");
            String restGroup = groupOrNull(matcher, "rest");
            if (lastGroup == null && currGroup != null && restGroup != null) {
                resultVector = new boolean[2];
                groups.add(new AbstractMap.SimpleEntry<>("curr", currGroup));
                groups.add(new AbstractMap.SimpleEntry<>("rest", restGroup));
            } else if (lastGroup != null && currGroup == null && restGroup == null) {
                resultVector = new boolean[1]; //a little ridiculous I know
                groups.add(new AbstractMap.SimpleEntry<>("last", lastGroup));
            } else {
                throw new IllegalStateException(
                    "This shouldn't be possible, check the ray grammar!"
                );
            }
            int i = 0;
            for (var kv : groups) {
                String group = kv.getValue();
                for (Rule rule : children.get(kv.getKey())) {
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
}
