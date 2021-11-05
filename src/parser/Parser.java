package parser;

import grammars.BooleanGrammar;
import grammars.Grammar;
import grammars.MathGrammar;
import grammars.RayGrammar;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class Parser {

    private BufferedReader file;
    private final Grammar mathGrammar, booleanGrammar, rayGrammar;

    public Parser(String filename) {
        this.mathGrammar = new MathGrammar();
        this.booleanGrammar = new BooleanGrammar(mathGrammar.exposeEntrypoint());
        this.rayGrammar = new RayGrammar();
        try {
            FileReader fr = new FileReader(filename);
            this.file = new BufferedReader(fr);
        } catch (FileNotFoundException e) {
            System.err.printf("File `%s` not found!\n", filename);
            System.exit(1);
        }
    }

    public boolean parse() {
        return false;
    }
}
