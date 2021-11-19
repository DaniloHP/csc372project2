package parser;

import static java.text.MessageFormat.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import parser.errors.VariableError;

public class ScopeStack extends Stack<Map<String, Variable>> {


    public ScopeStack() {
        super();
    }

    public ScopeStack(boolean testing) {
        this();
    }

    public void addToCurrScope(CharSequence varName, Variable var) {
        var currScope = this.peek();
        if (currScope.containsKey(varName.toString())) {
            throw new VariableError(
                format("Variable `{0}` already exists in this scope.", varName)
            );
        }
        currScope.put(varName.toString(), var);
    }

    public void addToCurrScope(Variable... vars) {
        for (Variable v : vars) {
            this.addToCurrScope(v.identifier, v);
        }
    }

    public void pushNewScope() {
        this.push(new HashMap<>());
    }

    public Variable find(String varName, boolean doThrow) {
        //I can take advantage of the silliness of Java's built-in stack to just
        //iterate through it, top down.
        String trimmed = varName.trim();
        for (int i = this.size() - 1; i >= 0; i--) {
            var scope = this.get(i);
            if (scope.containsKey(trimmed)) {
                return scope.get(trimmed);
            }
        }
        if (doThrow) {
            throw new VariableError(format("Variable `{0}` not found", trimmed));
        }
        return null;
    }

    public Variable find(String varName) {
        return this.find(varName, true);
    }
}
