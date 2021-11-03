package grammars;

public class ConditionalGrammar extends Grammar {

    public ConditionalGrammar() {
        super();
        //<if_statement>, <elf_statement>, <else_statement>
        Rule ifRule = new Rule("if +(.*): +\\n(.*)", "IF_STATEMENT");
        Rule elfRule = new Rule("elf +(.*): +\\n(.*)", "ELF_STATEMENT");
        Rule elseRule = new Rule("else: +\\n(.*)", "ELSE_STATEMENT");

        //Grammar Levels for statements
        GrammarLevel ifExpr = new GrammarLevel(ifRule);
        GrammarLevel elfExpr = new GrammarLevel(elfRule);
        GrammarLevel elseExpr = new GrammarLevel(elseRule);
    }
}
