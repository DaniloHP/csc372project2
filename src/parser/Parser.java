package parser;

import static grammars.Grammar.VAR_RULE;
import static java.text.MessageFormat.format;

import grammars.BoolGrammar;
import grammars.MathGrammar;
import grammars.RayGrammar;
import grammars.StringGrammar;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import parser.errors.IndentationError;
import parser.errors.InvalidStatementError;
import parser.errors.TypeError;
import parser.errors.VariableError;

public class Parser {

    private static final String COMMENT_SYMBOL = "?";
    private static final Pattern WS_YANK = Pattern.compile("(?<whitespace>[ \\t]+)(?<rest>.*)");
    private static final Pattern WS_SPLIT = Pattern.compile("(?<whitespace>[ \\t]*)(?<rest>.*)");
    private static final Pattern EMPTY_LINE = Pattern.compile("\\s*(\\?.*)?");
    private static final Pattern RAY_EXTRACT = Pattern.compile(" *\\[ *(?<ray>.*?) *] *");

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
    private static final Pattern PRINT_STMT = Pattern.compile(
        "[ \\t]*out\\((?<argument>.*)\\)[ \\t]*"
    );
    private static final Pattern PASS_STMT = Pattern.compile("[ \\t]*hallpass[ \\t]*");

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

    //TODO: consider adding instance variable which is current Line

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
                    !wsm.group("rest").startsWith(COMMENT_SYMBOL) &&
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
        if (s.isEmpty()) return false;
        char toMatch = s.charAt(0);
        for (char c : s.toCharArray()) {
            if (c != toMatch) return true;
        }
        return false;
    }

    //this method basically only exists for unit testing purposes
    public int countIndents(CharSequence ws) {
        return countIndents(ws, -1);
    }

    public int countIndents(CharSequence line, int ln) {
        Matcher m = armMatcher(WS_SPLIT, line);
        String ws = m.group("whitespace");
        if (ws.isEmpty()) {
            return 0;
        } else if (stringIsHeterogeneous(ws)) {
            //is all the same character
            throw new IndentationError("Invalid mixing of tabs and spaces", ln);
        } else if (ws.charAt(0) != whitespace.charAt(0)) {
            //that character is the same as we expect
            throw new IndentationError(
                "Unexpected whitespace character, file is using " + this.wsEnglishName,
                ln
            );
        } else if (ws.length() % whitespace.length() != 0) {
            throw new IndentationError("Whitespace is not aligned to " + this.wsEnglishName, ln);
        }
        return ws.length() / whitespace.length();
    }

    public String parseTesting(String className) {
        return this.parse(className, true);
    }

    //eventually will probably write to disk right here
    public String parseFull(String className) {
        return parse(className, false);
    }

    private String parse(String className, boolean testing) {
        ScopeStack scopes = new ScopeStack(testing);
        Map<String, Variable> defaultScope = new HashMap<>(1);
        defaultScope.put("argos", ARGOS);
        scopes.push(defaultScope);
        StringBuilder java = new StringBuilder();
        java
            .append("// GENERATED: ")
            .append(LocalDateTime.now())
            .append("\n")
            .append("public class ")
            .append(className)
            .append(" {\npublic static void main(String[] args)");
        parseBlock(0, scopes, java);
        java.append("}"); //closes class {
        return java.toString();
    }

    public void parseBlock(int lineStart, ScopeStack scopes, StringBuilder java) {
        java.append("{\n");
        boolean ifOpen = false;
        for (int i = lineStart; i < lines.size(); i++) {
            int currDepth = scopes.size() - 1; //-1 because scope includes the global scope of argos
            Line lineObj = lines.get(i);
            if (countIndents(lineObj.judo) < currDepth) {
                break;
            }
            Line trimmed = lineObj.trimmedCopy();
            StringBuilder line = lineObj.judo;
            int ln = lineObj.lineNum;
            boolean wasConditional = false;
            if (ASSIGN_STMT.matcher(line).matches()) {
                handleAssignment(trimmed, java, scopes);
            } else if (REASSIGN_STMT.matcher(line).matches()) {
                handleReassignment(trimmed, java, scopes);
            } else if (IF_STMT.matcher(line).matches()) {
                wasConditional = true;
                handleIf(trimmed, java, scopes);
                ifOpen = true;
            } else if (ELF_STMT.matcher(line).matches()) {
                wasConditional = true;
                if (!ifOpen) {
                    throw new InvalidStatementError("No if is currently open", ln);
                }
                handleElf(trimmed, java, scopes);
            } else if (ELSE_STMT.matcher(line).matches()) {
                wasConditional = true;
                if (!ifOpen) {
                    throw new InvalidStatementError("No if is currently open", ln);
                }
                handleElse(trimmed, java, scopes);
            } else if (FORRANGE_STMT.matcher(line).matches()) {
                handleForRange(trimmed, java, scopes);
                parseBlock(i + 1, scopes, java);
            } else if (FOREACH_STMT.matcher(line).matches()) {
                handleForEach(trimmed, java, scopes);
                parseBlock(i + 1, scopes, java);
            } else if (LOOP_STMT.matcher(line).matches()) {
                handleLoop(trimmed, java, scopes);
                parseBlock(i + 1, scopes, java);
            } else if (PRINT_STMT.matcher(line).matches()) {
                handlePrint(trimmed, java, scopes);
            } else if (!PASS_STMT.matcher(line).matches()) {
                throw new InvalidStatementError("`" + trimmed.judo.toString() + "`", ln);
            }
            if (!wasConditional) {
                ifOpen = false;
            }
        }
        scopes.pop();
        java.append("}\n");
    }

    public Matcher armMatcher(Pattern toUse, CharSequence toMatch) {
        Matcher m = toUse.matcher(toMatch);
        m.matches();
        //this has to be done to set internal state of the matcher to be ready
        //to do things like query the groups. Thanks OOP.
        return m;
    }

    public void handleAssignment(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(ASSIGN_STMT, line.judo);
        String varName = m.group("var");
        String value = m.group("rValue");
        var curScope = scopes.peek();
        if (curScope.containsKey(varName)) {
            throw new VariableError(
                format("Variable `{0}`is already defined in this scope.", varName),
                line.lineNum
            );
        }
        String javaType = "I WILL CAUSE AN ERROR";
        if (MATH_GRAMMAR.isValid(value)) {
            curScope.put(varName, new Variable(varName, Type.INT));
            javaType = "int";
        } else if (BOOL_GRAMMAR.isValid(value)) {
            curScope.put(varName, new Variable(varName, Type.BOOL));
            javaType = "boolean ";
        } else if (STRING_GRAMMAR.isValid(value)) {
            curScope.put(varName, new Variable(varName, Type.STRING));
            javaType = "String";
        } else if (RAY_GRAMMAR.isValid(value)) {
            Type t = categorizeRay(value, scopes, line);
            //noinspection ConstantConditions
            javaType = t.label;
            curScope.put(varName, new Variable(varName, t));
            value = value.replaceAll("\\[", "{").replaceAll("]", "}");
        } else {
            throw new InvalidStatementError("Unrecognized expression", line.lineNum);
        }
        java.append(javaType).append(" ").append(varName).append(" = ").append(value).append(";\n");
    }

    private Type categorizeRay(String ray, ScopeStack scopes, Line line) {
        Type overall = null;
        Matcher rayMatch = armMatcher(RAY_EXTRACT, ray);
        String[] list = rayMatch.group("ray").split(",");
        for (String el : list) {
            Type curr;
            if (VAR_RULE.validate(el)) {
                curr = scopes.find(el).type;
            } else if (MATH_GRAMMAR.isValid(el)) {
                curr = Type.INT;
            } else if (STRING_GRAMMAR.isValid(el)) {
                curr = Type.STRING;
            } else if (BOOL_GRAMMAR.isValid(el)) {
                curr = Type.BOOL;
            } else {
                throw new InvalidStatementError("Bad statement in ray literal");
            }
            if (overall == null) {
                overall = curr;
            } else if (curr != overall) {
                throw new TypeError("Inconsistent types in ray literal", line.lineNum);
            }
        }
        if (overall != null) {
            switch (overall) {
                case INT -> {
                    return Type.INT_LIST;
                }
                case STRING -> {
                    return Type.STRING_LIST;
                }
                case BOOL -> {
                    return Type.BOOL_LIST;
                }
            }
        }
        return null;
    }

    //TODO: need a translation function for converting keywords to Java equivalents

    public void handleReassignment(Line line, StringBuilder java, ScopeStack scopes) {}

    public void handleIf(Line line, StringBuilder java, ScopeStack scopes) {}

    public void handleElf(Line line, StringBuilder java, ScopeStack scopes) {}

    public void handleElse(Line line, StringBuilder java, ScopeStack scopes) {}

    // Handling: Judo: for loopVar in lo:hi[step]
    //           Java: for(type loopVar = lo; lo<=hi; loopVar+=step)
    public void handleForRange(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(FORRANGE_STMT, line.judo);
        String loopVar = m.group("loopVar");
        scopes.pushNewScope();
        scopes.addToCurrScope(loopVar, new Variable(loopVar, Type.INT));

        int lo;
        try {
            lo = Integer.parseInt(m.group("lo"));
        } catch (ClassCastException c) {
            throw new InvalidStatementError("Bad low value in range in for-loop.");
        }

        int hi;
        try {
            hi = Integer.parseInt(m.group("hi"));
        } catch (ClassCastException c) {
            throw new InvalidStatementError("Bad high value in range in for-loop.");
        }

        String stepString = m.group("step");
        int step = 1;
        if (stepString != null) {
            try {
                step = Integer.parseInt(m.group("step"));
            } catch (ClassCastException c) {
                throw new InvalidStatementError("Bad step value in for-loop.");
            }
        }

        //for(int loopVar = lo; lo <= hi; loopVar+=step;){
        java
            .append("for(int ")
            .append(loopVar)
            .append(" = ")
            .append(lo)
            .append("; ")
            .append(lo)
            .append("<=")
            .append(hi)
            .append("; ")
            .append(loopVar)
            .append(" += ")
            .append(step)
            .append(";)");
    }

    //Handling: Judo: for(loopVar : iterable)
    //          Java: for(type loopVar : iterable)
    public void handleForEach(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(FOREACH_STMT, line.judo);
        String loopVar = m.group("loopVar");
        Type rayType = scopes.find(m.group("array")).type;
        scopes.pushNewScope();
        scopes.addToCurrScope(loopVar, new Variable(loopVar, rayType));

        java
            .append("for(")
            .append(rayType.listOf)
            .append(" ")
            .append(loopVar)
            .append(" : ")
            .append(m.group("array"))
            .append(")");
    }

    public void handleLoop(Line line, StringBuilder java, ScopeStack scopes) {}

    public void handlePrint(Line line, StringBuilder java, ScopeStack scopes) {}

    private static class Line {

        final StringBuilder judo;
        final int lineNum;

        public Line(CharSequence code, int lineNum) {
            this.judo = new StringBuilder(code);
            this.lineNum = lineNum;
        }

        public Line(Line other) {
            this.judo = new StringBuilder(other.judo);
            this.lineNum = other.lineNum;
        }

        public Line trimmedCopy() {
            StringBuilder sb = new StringBuilder(judo.toString().strip());
            return new Line(sb, lineNum);
        }

        public String toString() {
            return format("{0}|{1}", lineNum, judo);
        }
    }

    public static class Variable {

        public Variable(String identifier, Type type) {
            this.identifier = identifier;
            this.type = type;
        }

        String identifier;
        Type type;
    }
}
