package grammars;

import static java.text.MessageFormat.format;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import parser.ScopeStack;
import parser.Type;
import parser.Variable;
import parser.errors.TypeError;
import parser.errors.VariableError;

public class VarRule extends Rule {

    /**
     * Keywords that don't carry a value.
     */
    public static final Set<String> NONVALUE_KEYWORDS = new HashSet<>(
        Arrays.asList("let", "if", "elf", "else", "argos", "hallpass", "out", "for", "loop", "mod")
    );

    /**
     * Maps builtin keywords that carry a value to a variable object that
     * appropriately represents them. I.e. "T", Judo's true, maps to a BOOL
     * variable.
     */
    private static final Map<String, Variable> BUILTINS_AS_VARIABLES = new HashMap<String, Variable>() {
        {
            put("T", new Variable("T", Type.BOOL));
            put("F", new Variable("F", Type.BOOL));
            put("argos", new Variable("argos", Type.INT_LIST));
        }
    };

    public static final Set<String> RESERVED_KEYWORDS = new HashSet<>(
        Arrays.asList(
            "let",
            "if",
            "elf",
            "else",
            "argos",
            "hallpass",
            "out",
            "for",
            "loop",
            "T",
            "F",
            "mod"
        )
    );

    /**
     * Whether to throw exceptions when vars are found to have the wrong type.
     * Only use false when testing.
     */
    public static boolean checkVarTypes = true;
    /**
     * Whether to throw exceptions when vars use keywords as their identifier.
     * Only use false when testing.
     */
    public static boolean checkAgainstKeywords = true;
    private static ScopeStack scopes;

    private Type expectedType;

    public VarRule(CharSequence regexStr, String id) {
        super(regexStr, id);
    }

    public VarRule(Rule other, String newId) {
        super(other, newId);
    }

    public VarRule(Rule other) {
        this(other, null);
    }

    /**
     * Equips this VarRule to expect the given type from variables it identifies.
     * @param type The type to expect
     */
    public void useType(Type type) {
        this.expectedType = type;
    }

    /**
     * Equips all VarRules (this is static) to check for variables in the given
     * ScopeStack. Until this is called with a non-null ScopeStack, no such
     * checking can or will be done. This means that the VarRule pulls double
     * duty of checking for syntactic validity and for semantic validity.
     * @param scopes The ScopeStack to use.
     */
    public static void useScopes(ScopeStack scopes) {
        VarRule.scopes = scopes;
    }

    /**
     * Returns whether the given expression, which should at this point just be
     * a variable, is valid under various trials. At most, will check if the
     * variable exists somewhere in the scope stack and if it is of the expected
     * type. At least will check if the variable name is valid under our regex,
     * which is made for compatibility with Java.
     * @param toCheck The expression to validate.
     * @return True if the expression is valid, false otherwise. Is liable to
     * throw TypeError or VariableError depending on the values of checkVarTypes
     * and checkAgainstKeywords respectively.
     */
    @Override
    public boolean validate(CharSequence toCheck) {
        //this.type may be null, and that's fine.
        return this.validate(toCheck, checkAgainstKeywords, checkVarTypes);
    }

    /**
     * Attempts to validate the given expression. See above for more details.
     * @param toCheck The expression to check.
     * @param doKWCheck Whether to check if the variable name is a keyword
     * @param doTypeCheck Whether to check if the variable is of the expected
     *                    type. Has no effect even if this is true but this rule
     *                    has no expected type.
     * @return Whether the given expression is valid.
     */
    public boolean validate(CharSequence toCheck, boolean doKWCheck, boolean doTypeCheck) {
        Matcher m = this.regex.matcher(toCheck);
        if (toCheck.length() > 0 && m.matches()) {
            String varName = m.group("var"); //the actual variable identifier.
            if (doKWCheck && VarRule.NONVALUE_KEYWORDS.contains(varName)) {
                //           ^This is to avoid treating T/F as a variable and
                //getting exceptions because "variable T uses a reserved..."
                throw new VariableError(
                    format("Variable `{0}` uses a reserved keyword for its name", varName)
                );
            }
            //VarRule will work if it doesn't have a ScopeStack, it'll just be
            //unable to check if variables exist and if they are of the right
            //type.
            if (VarRule.scopes != null) {
                //exists
                Variable var = BUILTINS_AS_VARIABLES.get(varName);
                //"T", "F", and "argos" will make their way into this function.
                //they are variables in that they hold value and are valid for
                //use in particular situation, so I keep them in this map. If
                //varName was something else, a user defined variable, var will
                //be null, and a lookup will be done in the ScopeStack.
                var = var == null ? scopes.find(varName, true) : var;
                //^will throw a VariableException if the variable isn't found
                if (doTypeCheck && this.expectedType != null && var.type != this.expectedType) {
                    throw new TypeError(
                        format(
                            "Variable `{0}` was expected to be of type {1}",
                            varName,
                            this.expectedType.javaType
                        )
                    );
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @return A similar message to other rules, but with including the type, or
     * untyped if the type is null.
     */
    @Override
    public String toString() {
        return format(
            "VarRule {0} ({1})",
            id,
            expectedType == null ? "untyped" : expectedType.javaType
        );
    }
}
