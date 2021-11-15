package grammars;

import static java.text.MessageFormat.format;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import parser.Parser;
import parser.ScopeStack;
import parser.Type;
import parser.errors.TypeError;
import parser.errors.VariableError;

public class VarRule extends Rule {

    public static final Set<String> RESERVED_KEYWORDS = new HashSet<>(
        List.of("let", "if", "elf", "else", "argos", "hallpass", "out", "for", "loop", "T", "F")
    );
    private static ScopeStack scopes;

    public VarRule(CharSequence regexStr, String id) {
        super(regexStr, id);
    }

    public static void setScopes(ScopeStack scopes) {
        VarRule.scopes = scopes;
    }

    @Override
    public boolean validate(CharSequence toCheck) {
        return this.validate(toCheck, null, false, false);
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
            if (doKWCheck && VarRule.RESERVED_KEYWORDS.contains(varName)) {
                throw new VariableError(
                    format("Variable `{0}` uses a reserved keyword for its name", varName)
                );
            }
            if (scopes != null) {
                Parser.Variable var = scopes.find(varName, true);
                if (doTypeCheck && var.type != expected) {
                    throw new TypeError(
                        format(
                            "Variable `{0}` was expected to be of type {1}",
                            varName,
                            var.type.javaType
                        )
                    );
                }
            }
            //^will throw an exception if the variable isn't found
            return true;
        }
        return false;
    }
}
