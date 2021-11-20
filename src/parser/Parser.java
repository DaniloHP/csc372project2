package parser;

import static java.text.MessageFormat.format;

import grammars.BoolGrammar;
import grammars.MathGrammar;
import grammars.RayGrammar;
import grammars.StringGrammar;
import grammars.VarGrammar;
import grammars.VarRule;
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
    private static final Pattern INDEXER_ACCESS = Pattern.compile(
        " *(?<var>[\\w&&[^\\d]][\\w]{0,31}) *\\[ *(?<index>.*?) *]"
    );
    private static final Pattern INDEXER_ASSIGN = Pattern.compile(
        " *(?<var>[\\w&&[^\\d]][\\w]{0,31}) *\\[ *(?<index>.*?) *] *= *(?<value>.+)"
    );
    private static final Pattern RAY_INIT = Pattern.compile(" *(?<type>[bsi])\\{ *(?<n>.*?) *} *");

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
        "[ \\t]*out(?<line>ln)?\\((?<argument>.*)\\)[ \\t]*"
    );
    private static final Pattern PASS_STMT = Pattern.compile("[ \\t]*hallpass[ \\t]*");

    public static final Variable ARGOS = new Variable("argos", Type.INT_LIST);
    private static final VarGrammar VAR_GRAMMAR = new VarGrammar(null);
    private static final StringGrammar STRING_GRAMMAR = new StringGrammar(VAR_GRAMMAR);
    private static final MathGrammar MATH_GRAMMAR = new MathGrammar(VAR_GRAMMAR);
    private static final BoolGrammar BOOL_GRAMMAR = new BoolGrammar(MATH_GRAMMAR, VAR_GRAMMAR);
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
        VarRule.useScopes(scopes); //not proud of this
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
            .append(" {\npublic static void main(String[] argos) ");
        parseBlock(0, scopes, java, this.whitespace);
        java.append("}"); //closes class {
        return java.toString();
    }

    public int parseBlock(
        int lineStart,
        ScopeStack scopes,
        StringBuilder java,
        String currWhitespace
    ) {
        java.append("{\n");
        boolean ifOpen = false;
        int linesParsed = 0;
        for (int i = lineStart; i < lines.size(); i++) {
            int currDepth = scopes.size() - 1; //-1 because scope includes the global scope of argos
            Line lineObj = lines.get(i);
            if (countIndents(lineObj.judo) < currDepth) {
                break;
            }
            linesParsed++;
            Line trimmed = lineObj.trimmedCopy();
            StringBuilder line = lineObj.judo;
            int ln = lineObj.lineNum;
            boolean wasConditional = false;
            java.append(currWhitespace);
            if (ASSIGN_STMT.matcher(line).matches()) {
                handleAssignment(trimmed, java, scopes);
            } else if (REASSIGN_STMT.matcher(line).matches()) {
                handleReassignment(trimmed, java, scopes);
            } else if (IF_STMT.matcher(line).matches()) {
                wasConditional = true;
                handleIf(trimmed, java, scopes);
                ifOpen = true;
                int parsed = parseBlock(i + 1, scopes, java, currWhitespace);
                i += parsed;
                linesParsed += parsed;
                //you'd think all these identical 3 line blocks could be in a
                //statement at the end of the loop but that breaks things
            } else if (ELF_STMT.matcher(line).matches()) {
                wasConditional = true;
                if (!ifOpen) {
                    throw new InvalidStatementError("No if is currently open", ln);
                }
                handleElf(trimmed, java, scopes);
                int parsed = parseBlock(i + 1, scopes, java, currWhitespace + this.whitespace);
                i += parsed;
                linesParsed += parsed;
            } else if (ELSE_STMT.matcher(line).matches()) {
                wasConditional = true;
                if (!ifOpen) {
                    throw new InvalidStatementError("No if is currently open", ln);
                }
                handleElse(trimmed, java, scopes);
                int parsed = parseBlock(i + 1, scopes, java, currWhitespace + this.whitespace);
                i += parsed;
                linesParsed += parsed;
            } else if (FORRANGE_STMT.matcher(line).matches()) {
                handleForRange(trimmed, java, scopes);
                int parsed = parseBlock(i + 1, scopes, java, currWhitespace + this.whitespace);
                i += parsed;
                linesParsed += parsed;
            } else if (FOREACH_STMT.matcher(line).matches()) {
                handleForEach(trimmed, java, scopes);
                int parsed = parseBlock(i + 1, scopes, java, currWhitespace + this.whitespace);
                i += parsed;
                linesParsed += parsed;
            } else if (LOOP_STMT.matcher(line).matches()) {
                handleLoop(trimmed, java, scopes);
                int parsed = parseBlock(i + 1, scopes, java, currWhitespace + this.whitespace);
                i += parsed;
                linesParsed += parsed;
            } else if (PRINT_STMT.matcher(line).matches()) {
                handlePrint(trimmed, java, scopes);
            } else if (INDEXER_ASSIGN.matcher(line).matches()) {
                handleRayIndexAssignment(trimmed, java, scopes);
            } else if (!PASS_STMT.matcher(line).matches()) {
                throw new InvalidStatementError("`" + trimmed.judo.toString() + "`", ln);
            }
            if (!wasConditional) {
                ifOpen = false;
            }
        }
        scopes.pop();
        java.append(currWhitespace).append("}\n");
        return linesParsed;
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
        Type t;
        if (MATH_GRAMMAR.validateNoThrow(value)) {
            curScope.put(varName, new Variable(varName, Type.INT));
            t = Type.INT;
        } else if (BOOL_GRAMMAR.validateNoThrow(value)) {
            curScope.put(varName, new Variable(varName, Type.BOOL));
            t = Type.BOOL;
        } else if (STRING_GRAMMAR.validateNoThrow(value)) {
            curScope.put(varName, new Variable(varName, Type.STRING));
            t = Type.STRING;
        } else if (RAY_GRAMMAR.validateNoThrow(value)) {
            t = RAY_GRAMMAR.categorizeNoThrow(value);
            curScope.put(varName, new Variable(varName, t));
            value = value.replaceAll("\\[", "{").replaceAll("]", "}");
        } else if (INDEXER_ACCESS.matcher(value).matches()) {
            Matcher indexer = armMatcher(INDEXER_ACCESS, value);
            String rayName = indexer.group("var");
            Variable ray = scopes.find(rayName);
            if (!ray.type.isArray()) {
                throw new TypeError(
                    format("Variable `{0}` isn't an array type and can't be indexed", rayName),
                    line.lineNum
                );
            } else if (ray.equals(ARGOS)) {
                value = format("Integer.parseInt({0})", value);
            }
            t = ray.type.listOf;
            validateByScalarType(indexer.group("index"), t);
            curScope.put(varName, new Variable(varName, t));
        } else if (RAY_INIT.matcher(value).matches()) {
            Matcher rayInit = armMatcher(RAY_INIT, value);
            t = initMatcherType(rayInit);
            String n = rayInit.group("n");
            validateByScalarType(n, Type.INT);
            value = format("new {0}[{1}]", t.listOf.javaType, n);
            curScope.put(varName, new Variable(varName, t));
        } else {
            throw new InvalidStatementError(
                format("Unrecognized expression: {0}", value),
                line.lineNum
            );
        }
        VAR_GRAMMAR.validate(varName);
        java
            .append(t.javaType)
            .append(" ")
            .append(varName)
            .append(" = ")
            .append(finalReplacements(value, t))
            .append(";\n");
    }

    private Type initMatcherType(Matcher initMatcher) {
        switch (initMatcher.group("type")) {
            case "i" -> {
                return Type.INT_LIST;
            }
            case "b" -> {
                return Type.BOOL_LIST;
            }
            default -> {
                return Type.STRING_LIST;
            }
        }
    }

    private void validateByScalarType(CharSequence expression, Type expected) {
        switch (expected) {
            case INT -> {
                MATH_GRAMMAR.validate(expression);
            }
            case BOOL -> {
                BOOL_GRAMMAR.validate(expression);
            }
            case STRING -> {
                STRING_GRAMMAR.validate(expression);
            }
        }
    }

    private String finalReplacements(CharSequence input, Type t) {
        String res = input.toString().trim();
        String re = "( +{0} +)|(^{0} +)|( +{0}$)|(^{0}$)";
        if (t != Type.STRING && t != Type.STRING_LIST) {
            res =
                res
                    .replaceAll(format(re, "T"), " true ")
                    .replaceAll(format(re, "F"), " false ")
                    .replaceAll(format(re, "and"), " && ")
                    .replaceAll(format(re, "mod"), " % ")
                    .replaceAll(format(re, "or"), " || ")
                    .replaceAll(format(re, "not"), " ! ");
        }
        return res;
    }

    public void handleReassignment(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(REASSIGN_STMT, line.judo);
        String varName = m.group("var");
        String value = m.group("rValue");
        Variable var = scopes.find(varName);
        boolean passed;
        String arrayReinit = "";
        Matcher indexer = INDEXER_ACCESS.matcher(value);
        if (indexer.matches()) {
            String rayName = indexer.group("var");
            Variable ray = scopes.find(rayName);
            if (ray.type.isArray()) {
                throw new TypeError(
                    format("Variable `{0}` isn't an array type and can't be indexed", rayName),
                    line.lineNum
                );
            }
            Type t = ray.type.listOf;
            MATH_GRAMMAR.validate(indexer.group("index"));
            //^ this will throw if it isn't valid
            passed = true;
        } else if (RAY_INIT.matcher(value).matches()) {
            Matcher rayInit = armMatcher(RAY_INIT, value);
            Type t = initMatcherType(rayInit);
            if (var.type != t) {
                throw new TypeError(
                    format(
                        "Variable `{0}` was expected to be of type {1}, but found {2}",
                        varName,
                        var.type.javaType,
                        t.javaType
                    ),
                    line.lineNum
                );
            }
            String n = rayInit.group("n");
            validateByScalarType(n, Type.INT);
            value = format("new {0}[{1}]", t.listOf.javaType, n);
            passed = true;
        } else {
            switch (var.type) {
                case INT -> {
                    passed = MATH_GRAMMAR.validateNoThrow(value);
                }
                case BOOL -> {
                    passed = BOOL_GRAMMAR.validateNoThrow(value);
                }
                case STRING -> {
                    passed = STRING_GRAMMAR.validateNoThrow(value);
                }
                default -> { //one of the list types
                    passed = RAY_GRAMMAR.categorize(value) == var.type;
                    arrayReinit = format("new {0}[]", var.type.listOf.javaType);
                    value = value.replaceFirst("\\[", "{").replace(']', '}');
                }
            }
        }
        if (!passed) {
            throw new TypeError(
                format(
                    "Variable `{0}` was previously assigned type {1}",
                    varName,
                    var.type.javaType
                ),
                line.lineNum
            );
        }
        java
            .append(varName)
            .append(" = ")
            .append(arrayReinit)
            .append(finalReplacements(value, var.type))
            .append(";\n");
    }

    public void handleIf(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(IF_STMT, line.judo);
        String condition = m.group("condition");
        BOOL_GRAMMAR.validate(condition);
        scopes.pushNewScope();
        java.append("if (").append(finalReplacements(condition, Type.BOOL)).append(") ");
    }

    public void handleElf(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(ELF_STMT, line.judo);
        String condition = m.group("condition");
        BOOL_GRAMMAR.validate(condition);
        scopes.pushNewScope();
        java.append("else if (").append(finalReplacements(condition, Type.BOOL)).append(") ");
    }

    public void handleElse(Line line, StringBuilder java, ScopeStack scopes) {
        scopes.pushNewScope();
        java.append("else ");
    }

    // Handling: Judo: for loopVar in lo:hi[step]
    //           Java: for(type loopVar = lo; lo<=hi; loopVar+=step)
    public void handleForRange(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(FORRANGE_STMT, line.judo);
        String loopVar = m.group("loopVar");
        scopes.pushNewScope();
        scopes.addToCurrScope(loopVar, new Variable(loopVar, Type.INT));

        String lo = m.group("lo");
        String hi = m.group("hi");
        MATH_GRAMMAR.validate(lo);
        MATH_GRAMMAR.validate(hi);

        //        int hi;
        //        try {
        //            hi = Integer.parseInt(m.group("hi"));
        //        } catch (NumberFormatException e) {
        //            throw new InvalidStatementError("Bad high value in range in for-loop");
        //        }

        String stepString = m.group("step");
        int step = 1;
        if (stepString != null) {
            try {
                step = Integer.parseInt(m.group("step"));
            } catch (NumberFormatException e) {
                throw new InvalidStatementError("Bad step value in for-loop");
            }
        }

        //for(int loopVar = lo; lo < hi; loopVar+=step;){
        java
            .append("for(int ")
            .append(loopVar)
            .append(" = ")
            .append(lo)
            .append("; ")
            .append(loopVar)
            .append(" < ")
            .append(hi)
            .append("; ")
            .append(loopVar)
            .append(" += ")
            .append(step)
            .append(") ");
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

    public void handleLoop(Line line, StringBuilder java, ScopeStack scopes) {
        scopes.pushNewScope();
        Matcher m = armMatcher(LOOP_STMT, line.judo);
        String condition = m.group("condition");
        BOOL_GRAMMAR.validate(condition); //throws on its own
        java.append("while(").append(finalReplacements(condition, Type.BOOL)).append(") ");
    }

    public void handlePrint(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(PRINT_STMT, line.judo);
        String arg = m.group("argument");
        String ln = m.group("line");
        Type argType;
        if (arg.isBlank()) {
            argType = null;
        } else if (VAR_GRAMMAR.validateNoThrow(arg)) {
            Variable v = scopes.find(arg);
            argType = v.type;
        } else if (MATH_GRAMMAR.validateNoThrow(arg)) {
            argType = Type.INT;
        } else if (BOOL_GRAMMAR.validateNoThrow(arg)) {
            argType = Type.BOOL;
        } else if (STRING_GRAMMAR.validateNoThrow(arg)) {
            argType = Type.STRING;
        } else if (INDEXER_ACCESS.matcher(arg).matches()) {
            Matcher idxM = armMatcher(INDEXER_ACCESS, arg);
            Variable v = scopes.find(idxM.group("var"));
            if (!v.type.isArray()) {
                throw new TypeError(
                    format("Variable `{0}` isn't an array type and can't be indexed", arg),
                    line.lineNum
                );
            }
            argType = v.type.listOf;
        } else {
            throw new InvalidStatementError("Invalid argument to out: " + arg, line.lineNum);
        }
        java
            .append(ln == null ? "System.out.print(" : "System.out.println(")
            .append(finalReplacements(arg, argType))
            .append(");\n");
    }

    public void handleRayIndexAssignment(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(INDEXER_ASSIGN, line.judo);
        String rayName = m.group("var");
        String index = m.group("index");
        String value = m.group("value");
        Variable ray = scopes.find(rayName);
        if (!ray.type.isArray()) {
            throw new TypeError(
                format("Variable `{0}` isn't an array type and can't be indexed", rayName),
                line.lineNum
            );
        }
        Type scalarType = ray.type.listOf;
        validateByScalarType(index, Type.INT);
        validateByScalarType(value, scalarType);
        java
            .append(rayName)
            .append("[")
            .append(finalReplacements(index, Type.INT))
            .append("] = ")
            .append(finalReplacements(value, scalarType))
            .append(";");
    }

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
}
