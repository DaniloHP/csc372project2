package grammars;

import java.util.Collections;
import java.util.List;
import parser.Type;

/**
 * Our variable name grammar:
 * <pre>
&lt;charLetter&gt; ::= a | b | &hellip; | y | z | A |  &hellip; | Z
&lt;charNum&gt;   ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
&lt;var&gt;  ::= &lt;charLetter&gt;&lt;end&gt; | &lt;charLetter&gt; | _&lt;charLetter&gt; | _&lt;end&gt;
&lt;end&gt; ::= &lt;charLetter&gt;&lt;end&gt; | &lt;charNum&gt;&lt;end&gt; | &lt;empty&gt; | &lt;end&gt;
 * </pre>
 * Allows for a good variety of variable names, but with the expected rules,
 * such as no variables that start with digits or include - dashes. Variable
 * names can be up to 32 characters long, and a single underscore is a valid
 * name, as it is in Java 8. Naturally our grammar is not equipped to deal with
 * unicode variable names. The regex that actually enforces this is:
 * <br>
 * {@code ^ *(?<var>[\w&&[^\d]][\w]{0,31}) *}
 * <br>
 *
 * The above grammar will render correctly in a browser (JavaDoc), but looks
 * terrible in source code. See this Grammar's constructor for a more readable
 * grammar in source code.
 */
public class VarGrammar extends Grammar {

    /**
     * Constructs a VarGrammar specifically with no expected type.
     * <charLetter> ::= a | b | … | y | z | A |  … | Z
     * <charNum>   ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
     * <var>  ::= <charLetter><end> | <charLetter> | _<charLetter> | _<end>
     * <end> ::= <charLetter><end> | <charNum><end> | <empty> | <end>
     */
    public VarGrammar() {
        VarRule varRule = new VarRule(VAR_RULE, "VAR");
        varRule.useType(null);
        List<Rule> varStmt = Collections.singletonList(varRule);
        this.levels.add(varStmt);
    }

    /**
     * Special implementation of exposeEntrypoint where you can provide a type
     * you expect to be evaluating. For example, the MathGrammar calls this with
     * Type.INT.
     * @param expected The type to be expecting when evaluating using this
     *                 entrypoint
     * @return A singleton list containing the rule. This is a little ridiculous,
     * but it's to fit into the API specified by Grammar.
     */
    public List<Rule> exposeEntrypoint(Type expected) {
        VarRule specificRule = new VarRule(VAR_RULE, "VAR");
        specificRule.useType(expected);
        return Collections.singletonList(specificRule);
    }
}
