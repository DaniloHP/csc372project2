package parser;

import static java.text.MessageFormat.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import parser.errors.VariableError;

public class ScopeStack extends Stack<Map<String, Parser.Variable>> {

    boolean testing;

    public ScopeStack() {
        super();
    }

    public ScopeStack(boolean testing) {
        this();
        this.testing = testing;
    }

    public void addToCurrScope(CharSequence varName, Parser.Variable var) {
        var currScope = this.peek();
        if (currScope.containsKey(varName.toString())) {
            throw new VariableError(
                format("Variable `{0}` already exists in this scope.", varName)
            );
        }
        currScope.put(varName.toString(), var);
    }

    public void pushNewScope() {
        this.push(new HashMap<>());
    }

    public Parser.Variable find(String varName, boolean doThrow) {
        //I can take advantage of the silliness of Java's built-in stack to just
        //iterate through it, top down.
        for (int i = this.size() - 1; i >= 0; i--) {
            var scope = this.get(i);
            if (scope.containsKey(varName)) {
                return scope.get(varName);
            }
        }
        if (doThrow && !this.testing) {
            throw new VariableError(format("Variable `{0}` not found", varName));
        } else if (testing) {
            return new Parser.Variable("~testing~", Type.INT_LIST);
        }
        return null;
    }

    public Parser.Variable find(String varName) {
        return this.find(varName, true);
    }
}