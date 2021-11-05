package parser;

import grammars.BooleanGrammar;
import grammars.Grammar;
import grammars.MathGrammar;
import grammars.RayGrammar;
import parser.Errors.WhitespaceError;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private final List<Line> lines;
    private Grammar mathGrammar, booleanGrammar, rayGrammar;
    private String whitespace = "";
    private static final String COMMENT_SYMBOL = "?";
    private Pattern wsYank;
    private Pattern wsEat;

    public Parser(String filename) {
        this.wsYank = Pattern.compile("([ \\t]+).*");
        this.wsEat = Pattern.compile("\\s+");
        this.mathGrammar = new MathGrammar();
        this.booleanGrammar = new BooleanGrammar(mathGrammar.exposeEntrypoint());
        this.rayGrammar = new RayGrammar();
        lines = new ArrayList<>();
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            int lineNum = 0;
            while (br.ready()) {
                lineNum++;
                String line = br.readLine();
                if (wsEat.matcher(line).matches()) {
                    continue; //lines of all whitespace are skipped
                }
                StringBuilder sb = new StringBuilder(line);
                Matcher wsm = wsYank.matcher(line);
                if (
                    wsm.matches() &&
                    !wsm.group(2).startsWith(COMMENT_SYMBOL) &&
                    this.whitespace.isEmpty()
                ) {
                    /*
                     * This finds the first instance of leading whitespace (that isn't a comment)
                     * and remembers it as this file's base unit of whitespace. Potential issues
                     * if we allow array declarations to be on multiple lines.
                     */
                    this.whitespace = wsm.group(1); //leading whitespace
                }
                lines.add(new Line(sb, lineNum));
            }
        } catch (FileNotFoundException e) {
            System.err.printf("File `%s` not found!\n", filename);
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Testing constructor
     */
    public Parser(String whitespace, String...lines) {
        this.whitespace = whitespace;
        this.lines = new ArrayList<>();
        int i = 1;
        for (String line : lines) {
            this.lines.add(new Line(line, i++));
        }
    }

    public int countIndents(String ws) {
        int indents = 0;
        for (int i = 0, j = 0; i < ws.length(); i++, j++) {
            if (j == whitespace.length()) {
                j = 0;
            }
            if (ws.charAt(i) != whitespace.charAt(j)) {
                throw new WhitespaceError();
            }
        }
        return indents;
    }

    public boolean parse() {
        Map<String, Variable> vars = new HashMap<>();
        int currWSLevel = 0;
        for (int i = 0; i < lines.size(); i++) {
            StringBuilder line = lines.get(i).code;

        }
        return false;
    }

    private static class Line {
        public Line(CharSequence code, int lineNum) {
            this.code = new StringBuilder(code);
            this.lineNum = lineNum;
        }

        StringBuilder code;
        int lineNum;
    }

    private static class Variable {
        String identifier;
        Type type;
    }
}
