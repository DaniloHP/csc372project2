package grammars;

import static java.text.MessageFormat.format;

import java.util.*;
import java.util.regex.Matcher;
import parser.ScopeStack;
import parser.Type;
import parser.Variable;
import parser.errors.TypeError;
import parser.errors.VariableError;

public class VarRule extends Rule {

    public static final Set<String> NONVALUE_KEYWORDS = new HashSet<>(
        List.of("let", "if", "elf", "else", "argos", "hallpass", "out", "for", "loop", "mod")
    );
    public static final Set<String> VALUE_KEYWORDS = new HashSet<>(List.of("T", "F"));
    private static final Map<String, Variable> BUILTINS_AS_VARIABLES = new HashMap<>() {
        {
            put("T", new Variable("T", Type.BOOL));
            put("F", new Variable("F", Type.BOOL));
            put("argos", new Variable("argos", Type.INT_LIST));
        }
    };

    public static final Set<String> RESERVED_KEYWORDS = new HashSet<>(
        List.of("let", "if", "elf", "else", "argos", "hallpass", "out", "for", "loop", "T", "F", "mod")
    ); // = NONVALUE_KEYWORDS U VALUE_KEYWORDS
    public static boolean checkVarTypes = true;
    public static boolean checkAgainstKeywords = true;
    private static ScopeStack scopes;

    private Type type;

    public VarRule(CharSequence regexStr, String id) {
        super(regexStr, id);
    }

    public VarRule(Rule other, String newId) {
        super(other, newId);
    }

    public VarRule(Rule other) {
        this(other, null);
    }

    public void useType(Type type) {
        this.type = type;
    }

    public static void useScopes(ScopeStack scopes) {
        VarRule.scopes = scopes;
    }

    @Override
    public boolean validate(CharSequence toCheck) {
        //this.type may be null, and that's fine.
        return this.validate(toCheck, this.type, checkAgainstKeywords, checkVarTypes);
    }

    public boolean validate(
        CharSequence toCheck,
        Type expected,
        boolean doKWCheck,
        boolean doTypeCheck
    ) {
        Matcher m = this.regex.matcher(toCheck);
        if (!toCheck.isEmpty() && m.matches()) {
            String varName = m.group("var");
            //In this block I check if it's a keyword?
            if (doKWCheck && VarRule.NONVALUE_KEYWORDS.contains(varName)) {
                //           ^This is to avoid treating T/F as a variable and
                //getting exceptions because "variable T uses a reserved..."
                throw new VariableError(
                    format("Variable `{0}` uses a reserved keyword for its name", varName)
                );
            }
            if (scopes != null) {
                //exists
                Variable var = BUILTINS_AS_VARIABLES.get(varName);
                var = var == null ? scopes.find(varName, true) : var;
                //is of the expected type
                if (doTypeCheck && expected != null && var.type != expected) {
                    throw new TypeError(
                        format(
                            "Variable `{0}` was expected to be of type {1}",
                            varName,
                            expected.javaType
                        )
                    );
                }
            }
            //^will throw an exception if the variable isn't found
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return format("VarRule {0} ({1})", id, type == null ? "untyped" : type.javaType);
    }
}
