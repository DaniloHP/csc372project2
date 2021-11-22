package parser;

import static java.text.MessageFormat.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import parser.errors.VariableError;

/**
 * Small wrapper around a Stack<Map<String, Variable>> with convenience methods
 * added. This is meant to represent a stack of scopes similar to what you see
 * in real programming languages, where each block has access to the variables
 * declared only in its own scope and parent scopes.
 */
public class ScopeStack extends Stack<Map<String, Variable>> {

    public ScopeStack() {
        super();
    }

    /**
     * Adds the given key value pair to the top scope of this ScopeStack
     * @param varName The string identifier of the variable
     * @param var The Variable object representing it
     */
    public void addToCurrScope(CharSequence varName, Variable var) {
        Map<String, Variable> currScope = this.peek();
        if (currScope.containsKey(varName.toString())) {
            throw new VariableError(
                format("Variable `{0}` already exists in this scope.", varName)
            );
        }
        currScope.put(varName.toString(), var);
    }

    /**
     * Convenience method for adding variables en masse to the current scope.
     * Mostly for testing.
     * @param vars The variables to add to the current scope.
     */
    public void addToCurrScope(Variable... vars) {
        for (Variable v : vars) {
            this.addToCurrScope(v.identifier, v);
        }
    }

    /**
     * Adds a new scope to the top of the stack
     */
    public void pushNewScope() {
        this.push(new HashMap<>());
    }

    /**
     * I can take advantage of the silliness of Java's built-in stack to just
     * iterate through it, top down, looking for the first variable that has
     * the identifier varName
     * @param varName The variable identifier to look for
     * @param doThrow Whether to throw a VariableError if no such variable is
     *                found.
     * @return The Variable object associated with the given id, or null if none
     * was found and doThrow is false.
     */
    public Variable find(String varName, boolean doThrow) {
        String trimmed = varName.trim();
        for (int i = this.size() - 1; i >= 0; i--) {
            Map<String, Variable> scope = this.get(i);
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
