package grammars;

import java.util.Arrays;
import java.util.List;
import parser.Type;

/**
 * Our string literal grammar:
 * <pre>
 &lt;string&gt; ::= &lt;char&gt;&lt;string&gt;  | &lt;char&gt;
 &lt;string_literal&gt; ::= &ldquo;&lt;string&gt;&rdquo;
 &lt;char&gt; ::= &lt;charLetter&gt; | &lt;digit&gt; | @|#|$|%|^|&amp;|*|(|)|-|=|[|]|{|}|\\|||'|;|:|&lt;|&gt;|,|.|?|/|`|~|&quot;
 * </pre>
 * The above lines will render correctly in a browser (JavaDoc), but look
 * terrible in source code. See this Grammar's constructor for a more readable
 * grammar in source code.
 */
public class StringGrammar extends Grammar {

    /**
     * This very simple grammar allows string literals and variables, no string
     * concatenation. Uses Java's \p{Print} character class, which matches all
     * printable ASCII characters, but no unicode or ascii control characters,
     * including \t and \n.
     * <string> ::= <char><string>  | <char>
     * <string_literal> ::= “<string>”
     * <char> ::= <charLetter> | <digit> | @|#|$|%|^|&|*|(|)|-|=|[|]|{|}|\\|||'|;|:|<|>|,|.|?|/|`|~|"
     * @param vg The VarGrammar on which all other grammars depend for making
     *           sure that variables within expressions exist and are of the
     *           right type.
     */
    public StringGrammar(VarGrammar vg) {
        super();
        Rule strLiteralRule = new Rule("\\\"[\\p{Print}&&[^\\\"]]*?\\\"", "STR_LITERAL");
        VarRule strVarRule = new VarRule(VAR_RULE, "STR_VAR");
        strVarRule.useType(Type.STRING);
        List<Rule> strExpr = Arrays.asList(strLiteralRule, strVarRule);
        strVarRule.addChildren("var", vg.exposeEntrypoint());
        this.levels.add(strExpr);
    }
}
