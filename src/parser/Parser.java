package parser;

import static java.text.MessageFormat.format;

import grammars.BoolGrammar;
import grammars.MathGrammar;
import grammars.RayGrammar;
import grammars.StringGrammar;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import parser.errors.IndentationError;
import parser.errors.InvalidStatementError;
import parser.errors.VariableError;

public class Parser {

    private static final String COMMENT_SYMBOL = "?";
    private static final Pattern WS_YANK = Pattern.compile("([ \\t]+).*");
    private static final Pattern WS_SPLIT = Pattern.compile("([ \\t]*)(.*)");
    private static final Pattern EMPTY_LINE = Pattern.compile("\\s*(\\?.*)?");

    private static final Pattern IF_STMT = Pattern.compile("[ \\t]*if +(?<condition>.*?) *: *");
    private static final Pattern ELF_STMT = Pattern.compile("[ \\t]*elf +(?<condition>.*?) *: *");
    private static final Pattern ELSE_STMT = Pattern.compile("[ \\t]*else *: *");
    private static final Pattern ASSIGN_STMT = Pattern.compile(
        "[ \\t]*let +(?<var>[\\w&&[^\\d]]+[\\w]*) += +(?<rValue>.*)"
    );
    private static final Pattern REASSIGN_STMT = Pattern.compile(
        "[ \\t]*(?<var>[\\w&&[^\\d]]+[\\w]*) += +(?<rValue>.*)"
    );
    private static final Pattern FORRANGE_STMT = Pattern.compile(
        "[ \\\\t]*for +(?<loopVar>[a-zA-Z_]+[a-zA-Z_\\\\d]*) +in +(?<lo>.*?)\\.\\.(?<hi>.*?)(\\[(?<step>.+)])? *: *"
    );
    private static final Pattern FOREACH_STMT = Pattern.compile(
        "[ \\t]*for +(?<loopVar>[\\w&&[^\\d]]+[\\w]*) +in +(?<array>[\\w&&[^\\d]]+[\\w]*) *: *"
    );
    private static final Pattern LOOP_STMT = Pattern.compile("[ \\t]*loop +(?<condition>.*?) *: *");
    private static final Pattern PRINT_STMT = Pattern.compile("out\\((?<argument>.*)\\)");

    private static final Variable ARGOS = new Variable("argos", Type.INT_LIST);
    private static final StringGrammar STRING_GRAMMAR = new StringGrammar();
    private static final MathGrammar MATH_GRAMMAR = new MathGrammar();
    private static final BoolGrammar BOOL_GRAMMAR = new BoolGrammar(MATH_GRAMMAR);
    private static final RayGrammar RAY_GRAMMAR = new RayGrammar(
        BOOL_GRAMMAR,
        MATH_GRAMMAR,
        STRING_GRAMMAR
    );

    private final List<Line> lines;
    private String whitespace;
    private String wsEnglishName;

    public Parser(String filename) {
        this.whitespace = this.wsEnglishName = "";
        lines = new ArrayList<>();
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            int lineNum = 0;
            while (br.ready()) {
                lineNum++;
                String line = br.readLine().stripTrailing();
                if (EMPTY_LINE.matcher(line).matches()) {
                    //lines of all whitespace or only comments are skipped
                    continue;
                }
                StringBuilder sb = new StringBuilder(line);
                Matcher wsm = WS_YANK.matcher(line);
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
                    if (stringIsHeterogeneous(this.whitespace)) {
                        throw new IndentationError("Invalid mixing of tabs and spaces", lineNum);
                    }
                    int numChars = this.whitespace.length();
                    String charInUse = this.whitespace.charAt(0) == ' ' ? "space" : "tab";
                    String plural = numChars == 1 ? "" : "s";
                    this.wsEnglishName = String.format("%d %s%s", numChars, charInUse, plural);
                    //result will be something like "1 tab", "4 spaces", etc.
                }
                int index = sb.indexOf(COMMENT_SYMBOL);
                if (index > 0) {
                    sb.setLength(index); //cut off comments
                    sb.trimToSize();
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
    public Parser(String whitespace, String... lines) {
        this.whitespace = whitespace;
        this.wsEnglishName = "";
        this.lines = new ArrayList<>();
        int i = 1;
        for (String line : lines) {
            this.lines.add(new Line(line, i++));
        }
    }

    private boolean stringIsHeterogeneous(String s) {
        char toMatch = s.charAt(0);
        for (char c : s.toCharArray()) {
            if (c != toMatch) return true;
        }
        return false;
    }

    //this method basically only exists for unit testing purposes
    public int countIndents(String ws) {
        return countIndents(ws, -1);
    }

    public int countIndents(String ws, int ln) {
        //is all the same character
        //that character is the same as we expect
        if (stringIsHeterogeneous(ws)) {
            throw new IndentationError("Invalid mixing of tabs and spaces", ln);
        } else if (ws.charAt(0) != whitespace.charAt(0)) {
            throw new IndentationError(
                "Unexpected whitespace character, file is using " + this.wsEnglishName,
                ln
            );
        } else if (ws.length() % whitespace.length() != 0) {
            throw new IndentationError("Whitespace is not aligned to " + this.wsEnglishName, ln);
        }
        return ws.length() / whitespace.length();
    }

    public boolean parse() {
        Stack<Map<String, Variable>> scopes = new Stack<>();
        Map<String, Variable> defaultScope = new HashMap<>(1);
        defaultScope.put("argos", ARGOS);
        scopes.push(defaultScope);
        StringBuilder java = new StringBuilder();
        java.append("public class JudoTranslated {\npublic static void main(String[] args) {");
        return parseHelper(0, scopes, java);
    }

    public boolean parseHelper(
        int lineStart,
        Stack<Map<String, Variable>> scopes,
        StringBuilder java
    ) {
        for (int i = lineStart; i < lines.size(); i++) {
            int currDepth = scopes.size() - 1; //-1 because scope includes the global scope of argos
            Line lineObj = lines.get(i);
            checkIndentation(lineObj, currDepth);
            Line trimmed = lineObj.trimmedCopy();
            StringBuilder line = lineObj.code;
            int ln = lineObj.lineNum;
            if (ASSIGN_STMT.matcher(line).matches()) {
                handleAssignment(trimmed, java, scopes);
            } else if (REASSIGN_STMT.matcher(line).matches()) {
                handleReassignment(trimmed, java, scopes);
            } else if (IF_STMT.matcher(line).matches()) {
                handleIf(trimmed, java, scopes);
            } else if (ELF_STMT.matcher(line).matches()) {
                handleElf(trimmed, java, scopes);
            } else if (ELSE_STMT.matcher(line).matches()) {
                handleElse(trimmed, java, scopes);
            } else if (FORRANGE_STMT.matcher(line).matches()) {
                handleForRange(trimmed, java, scopes);
            } else if (FOREACH_STMT.matcher(line).matches()) {
                handleForEach(trimmed, java, scopes);
            } else if (LOOP_STMT.matcher(line).matches()) {
                handleLoop(trimmed, java, scopes);
            } else if (PRINT_STMT.matcher(line).matches()) {
                handlePrint(trimmed, java, scopes);
            } else {
                throw new InvalidStatementError("Invalid statement", ln);
            }
        }
        return true;
    }

    public void checkIndentation(Line line, int level) {
        Matcher m = WS_SPLIT.matcher(line.code);
        boolean matches = m.matches();
        if (matches && countIndents(m.group(1), line.lineNum) != level) {
            throw new IndentationError("Unexpected change in indentation level", line.lineNum);
        } else if (!matches) {
            //should literally never happen because "" will match WS_SPLIT
            throw new IllegalStateException();
        }
    }

    public Matcher armMatcher(Pattern toUse, CharSequence toMatch) {
        Matcher m = toUse.matcher(toMatch);
        m.matches();
        //this has to be done to set internal state of the matcher to be ready
        //to do things like query the groups. Thanks OOP.
        return m;
    }

    public void handleAssignment(
        Line line,
        StringBuilder java,
        Stack<Map<String, Variable>> scopes
    ) {
        Matcher m = armMatcher(ASSIGN_STMT, line.code);
        String varName = m.group("var");
        String value = m.group("rValue");
        var curScope = scopes.peek();
        if (curScope.containsKey(varName)) {
            throw new VariableError(
                format("Variable `{0}`is already defined in this scope.", varName),
                line.lineNum
            );
        }
        if (MATH_GRAMMAR.isValid(value)) {
            curScope.put(varName, new Variable(varName, Type.INT));
        } else if (BOOL_GRAMMAR.isValid(value)) {
            curScope.put(varName, new Variable(varName, Type.BOOL));
        } else if (RAY_GRAMMAR.isValid(value)) {}
    }

    public void handleReassignment(
        Line line,
        StringBuilder java,
        Stack<Map<String, Variable>> scopes
    ) {}

    public void handleIf(Line line, StringBuilder java, Stack<Map<String, Variable>> scopes) {}

    public void handleElf(Line line, StringBuilder java, Stack<Map<String, Variable>> scopes) {}

    public void handleElse(Line line, StringBuilder java, Stack<Map<String, Variable>> scopes) {}

    public void handleForRange(
        Line line,
        StringBuilder java,
        Stack<Map<String, Variable>> scopes
    ) {}

    public void handleForEach(Line line, StringBuilder java, Stack<Map<String, Variable>> scopes) {}

    public void handleLoop(Line line, StringBuilder java, Stack<Map<String, Variable>> scopes) {}

    public void handlePrint(Line line, StringBuilder java, Stack<Map<String, Variable>> scopes) {}

    private static class Line {

        final StringBuilder code;
        final int lineNum;

        public Line(CharSequence code, int lineNum) {
            this.code = new StringBuilder(code);
            this.lineNum = lineNum;
        }

        public Line(Line other) {
            this.code = new StringBuilder(other.code);
            this.lineNum = other.lineNum;
        }

        public Line trimmedCopy() {
            StringBuilder sb = new StringBuilder(code.toString().strip());
            return new Line(sb, lineNum);
        }
    }

    private static class Variable {

        public Variable(String identifier, Type type) {
            this.identifier = identifier;
            this.type = type;
        }

        String identifier;
        Type type;
    }
}
