package grammars;

import java.util.ArrayList;
import java.util.List;

public class ConditionalGrammar extends Grammar {

    public ConditionalGrammar() {
        super();
        //<if_statement>, <elf_statement>, <else_statement>
        Rule ifRule = new Rule("if +(.*): +\\n(.*)", "IF_STATEMENT");
        Rule elfRule = new Rule("elf +(.*): +\\n(.*)", "ELF_STATEMENT");
        Rule elseRule = new Rule("else: +\\n(.*)", "ELSE_STATEMENT");

        //Grammar Levels for statements
        List<Rule> ifExpr = new ArrayList<>(List.of(ifRule));
        List<Rule> elfExpr = new ArrayList<>(List.of(elfRule));
        List<Rule> elseExpr = new ArrayList<>(List.of(elseRule));
    }
}
