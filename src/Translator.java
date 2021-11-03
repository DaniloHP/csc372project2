import grammars.Grammar;
import grammars.MathGrammar;

public class Translator {

    public static void main(String[] args) {
        var grammar = new MathGrammar();
        var res = grammar.isValid("1+2+3/2");
        System.out.println(res);
    }
}
