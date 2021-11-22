package parser;

import static java.text.MessageFormat.format;

import grammars.BoolGrammar;
import grammars.Grammar;
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

/**
 * This is the main parser class. It has some instance variables, but this whole
 * program is really designed to process one Judo file in its lifetime, there's
 * too must static state that breaks subsequent runs.
 */
public class Parser {

    private static final String COMMENT_SYMBOL = "?";
    private static final Pattern WS_YANK = Pattern.compile("(?<whitespace>[ \\t]+)(?<rest>.*)");
    private static final Pattern WS_SPLIT = Pattern.compile("(?<whitespace>[ \\t]*)(?<rest>.*)");
    private static final Pattern EMPTY_LINE = Pattern.compile("\\s*(\\?.*)?");
    private static final Pattern RAY_EXTRACTOR = Pattern.compile("\\[ *(?<innerRay>.*) *]");
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
    private static final VarGrammar VAR_GRAMMAR = new VarGrammar();
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

    /**
     * Ingests the Judo file at the given filename. The file is broken down
     * line-by-line, which means no single valid statement spans more than one line,
     * including array literals. No actual Java code is produced until one of the
     * Parse methods is called.
     * @param filename The path to the Judo file to translate to java.
     */
    public Parser(String filename) {
        this.whitespace = this.wsEnglishName = "";
        lines = new ArrayList<>();
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            int lineNum = 0;
            while (br.ready()) {
                lineNum++;
                String line = br.readLine();
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
                    //This finds the first instance of leading whitespace (that isn't a comment)
                    //and remembers it as this file's base unit of whitespace.
                    this.whitespace = wsm.group("whitespace"); //leading whitespace
                    if (stringIsHeterogeneous(this.whitespace)) {
                        throw new IndentationError("Invalid mixing of tabs and spaces", lineNum);
                    }
                    int numChars = this.whitespace.length();
                    String charInUse = this.whitespace.charAt(0) == ' ' ? "space" : "tab";
                    String plural = numChars == 1 ? "" : "s";
                    this.wsEnglishName = String.format("%d %s%s", numChars, charInUse, plural);
                    //result will be something like "1 tab", "4 spaces", etc.
                    //This is for error messages.
                }
                int index = sb.indexOf(COMMENT_SYMBOL);
                if (index > 0) {
                    sb.setLength(index); //cut off comments entirely
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
     * @param whitespace characters to use as the "file"'s whitespace
     * @param lines Lines representing the "file".
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

    /**
     * this method basically only exists for unit testing purposes.
     * @param ws A specific whitespace character to use.
     * @return The number of indents counted.
     */
    public int countIndents(CharSequence ws) {
        return countIndents(ws, -1);
    }

    /**
     * Counts the number of indents at the beginning of line. Uses the class's
     * whitespace instance variable which is determined in the constructor based
     * on the Judo file. Will throw an IndentationError if the whitespace is
     * found to contain a mix of tabs and spaces or a whitespace character that
     * differs from that in the whitespace instance variable. Also throws an
     * IndentationError if the number of whitespace characters isn't in line
     * with the expected amount, i.e. a line indented with 6 spaces in a file
     * that uses 4 spaces.
     * @param line The line whose leading whitespace to use.
     * @param ln The line's line number.
     * @return The number of indents counted.
     */
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

    /**
     * Using the already ingested Judo file (from the constructor), attempts to
     * create an entire legal Java file and return it as a string for the
     * consumer to do with as they please.
     * @param className The classname to give the new Java file.
     * @return An entire legal Java file, translated from the Judo file whose
     * path was provided in the constructor.
     */
    public String parseFull(String className) {
        ScopeStack scopes = new ScopeStack();
        VarRule.useScopes(scopes); //not proud of this
        Map<String, Variable> defaultScope = new HashMap<>(1);
        defaultScope.put("argos", ARGOS);
        scopes.push(defaultScope);
        StringBuilder java = new StringBuilder();
        java
                .append("// GENERATED: ")
                .append(LocalDateTime.now()) //timestamp
                .append("\n")
                .append("public class ")
                .append(className)
                .append(" {\npublic static void main(String[] argos) ");
        parseBlock(0, scopes, java, this.whitespace);
        java.append("}"); //closes class {
        return java.toString();
    }

    /**
     * This has to be done to set internal state of the matcher to be ready
     * to do things like query the groups. Thanks OOP. This method makes no
     * guarantees that the returned matcher actually matches toMatch.
     * @param toUse The pattern from which to generate the matcher.
     * @param toMatch The string to match against.
     * @return A matcher which is "armed", that is, ready to have its groups
     * queried.
     */
    public Matcher armMatcher(Pattern toUse, CharSequence toMatch) {
        Matcher m = toUse.matcher(toMatch);
        m.matches();
        return m;
    }

    /**
     * Given a matching matcher from pattern RAY_INIT, returns the type of the
     * ray being initialized. RAY_INIT allows users to create a type ray of an
     * arbitrary size with the syntax
     * <pre>
let arr = i{100}
     * </pre>
     * Which would initialize an int array of size 100.
     * @param initMatcher An armed and matching matcher of the RAY_INIT pattern.
     * @return The appropriate type for this ray.
     */
    private Type initMatcherType(Matcher initMatcher) {
        switch (initMatcher.group("type")) {
            case "i":
                return Type.INT_LIST;
            case "b":
                return Type.BOOL_LIST;
            default:
                return Type.STRING_LIST;
        }
    }

    /**
     * Validates the given expression under the grammar pertaining to expected.
     * Will throw IllegalArgumentException if expected is a ray type. Uses
     * the throwing version of validation.
     * @param expression A scalar type, i.e. INT, BOOL, or STRING
     * @param expected The type expression is expected to validate as.
     */
    private void validateByScalarType(CharSequence expression, Type expected) {
        if (expected.isRayType()) {
            //The RayGrammar's .validate() method doesn't really supply satisfactory
            //validation because it doesn't check types.
            throw new IllegalArgumentException("Only use this function with scalar types");
        } else {
            typeToGrammar(expected).validate(expression);
        }
    }

    /**
     * Returns the Grammar pertaining to the given type
     * @param t The type of the grammar to fetch
     * @return Math, Bool, or String grammar for scalar t's, or a RayGrammar
     * for anything else.
     */
    private Grammar typeToGrammar(Type t) {
        switch (t) {
            case INT:
                return MATH_GRAMMAR;
            case BOOL:
                return BOOL_GRAMMAR;
            case STRING:
                return STRING_GRAMMAR;
            default:
                return RAY_GRAMMAR;
        }
    }

    /**
     * Intelligently replaces all necessary replacements for Java legality.
     * Should be done just before being appended to the Java StringBuilder,
     * because the result of these replacements will no longer be valid Judo.
     * @param expression The expression to replace in.
     * @param t The type of expression.
     * @return expression with the necessary replacements made, which may
     * actually be none at all. For example strings don't need any replacements
     * done.
     */
    private String finalReplacements(CharSequence expression, Type t) {
        String trimmed = expression.toString().trim();
        String res = null;
        if (t == null) {
            res = trimmed;
        } else if (t.isRayType()) {
            //if the given expression is an array, split it by comma and run the
            //replacer on each scalar element.
            Matcher m = RAY_EXTRACTOR.matcher(trimmed);
            if (m.matches()) {
                String[] els = m.group("innerRay").split(",");
                StringBuilder sb = new StringBuilder("{");
                for (int i = 0; i < els.length; i++) {
                    sb.append(finalReplacements(els[i], t.listOf));
                    if (i < els.length - 1) {
                        sb.append(",");
                    }
                }
                res = sb.append("}").toString();
            }
        } else if (t != Type.STRING) {
            //strings require no replacements
            Grammar g = typeToGrammar(t);
            res = g.keywordsToJava(trimmed);
        }
        return res == null ? trimmed : res;
    }

    /**
     * Parses an entire block of Judo starting at the index lineStart.
     * @param lineStart The first line of the block to parse in the instance
     *                  variable lines.
     * @param scopes The ScopeStack to query and modify as the block is parsed
     *               and translated.
     * @param java The ongoing Java document which is appended to by the various
     *             handle* functions, all of which are called from in here.
     * @param currWhitespace The current amount of whitespace to put before each
     *                       line of Java. This doesn't work perfectly, but it's
     *                       a little better than zero indentation.
     * @return The total number of lines that were parsed as a result of this
     * call, including all recursive calls.
     */
    public int parseBlock(
        int lineStart,
        ScopeStack scopes,
        StringBuilder java,
        String currWhitespace
    ) {
        java.append("{\n"); //open the block with a {
        boolean ifOpen = false;
        int linesParsed = 0;
        for (int i = lineStart; i < lines.size(); i++) {
            int currDepth = scopes.size() - 1; //-1 because scopes includes the global scope of argos
            Line lineObj = lines.get(i);
            if (countIndents(lineObj.judo) < currDepth) {
                //this indicates that the current block is over because we went
                //up by an indentation level.
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
        java.append(currWhitespace).append("}\n"); //close the block with a }
        return linesParsed;
    }

    /**
     * Handles the assignment, and thus creation, of a new variable. This
     * statement always begins with a "let", and the type of the value is
     * inferred based on various things. If it is a variable, it takes on the
     * type of that variable. If it is an expression, Grammars are tried on it
     * until one validates. The type connected to the variable here cannot be
     * changed.
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
    public void handleAssignment(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(ASSIGN_STMT, line.judo);
        String varName = m.group("var");
        String value = m.group("rValue");
        Map<String, Variable> curScope = scopes.peek();
        if (curScope.containsKey(varName)) {
            throw new VariableError(
                format("Variable `{0}`is already defined in this scope.", varName),
                line.lineNum
            );
        }
        Type t;
        if (MATH_GRAMMAR.validateNoThrow(value)) {
            //int literal expression or variable
            curScope.put(varName, new Variable(varName, Type.INT));
            t = Type.INT;
        } else if (BOOL_GRAMMAR.validateNoThrow(value)) {
            //boolean literal expression or variable
            curScope.put(varName, new Variable(varName, Type.BOOL));
            t = Type.BOOL;
        } else if (STRING_GRAMMAR.validateNoThrow(value)) {
            //string literal expression or variable
            curScope.put(varName, new Variable(varName, Type.STRING));
            t = Type.STRING;
        } else if (RAY_GRAMMAR.validateNoThrow(value)) {
            //array literal expression or variable
            t = RAY_GRAMMAR.categorizeNoThrow(value);
            curScope.put(varName, new Variable(varName, t));
        } else if (INDEXER_ACCESS.matcher(value).matches()) {
            //Assigning FROM an array, i.e. let b = arr[10]
            Matcher indexer = armMatcher(INDEXER_ACCESS, value);
            String rayName = indexer.group("var");
            //the variable being indexed
            Variable ray = scopes.find(rayName);
            if (!ray.type.isRayType()) {
                throw new TypeError(
                    format("Variable `{0}` isn't an array type and can't be indexed", rayName),
                    line.lineNum
                );
            } else if (ray.equals(ARGOS)) {
                //special treatment for argos, our alias for the program args
                value = format("Integer.parseInt({0})", value);
            }
            t = ray.type.listOf;
            validateByScalarType(indexer.group("index"), t);
            curScope.put(varName, new Variable(varName, t));
        } else if (RAY_INIT.matcher(value).matches()) {
            //ray initiation using the syntax: let a = i{N}
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

    /**
     * Handles the reassignment of variables. This is different from assignment
     * in that it does not begin with the keyword let. If the variable being
     * reassigned didn't already exist, a VariableError will be thrown. Also,
     * the value being assigned to the variable must be of the same type as the
     * variable was originally.
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
    public void handleReassignment(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(REASSIGN_STMT, line.judo);
        String varName = m.group("var");
        String value = m.group("rValue");
        Variable toReassign = scopes.find(varName);
        boolean passed;
        String arrayReinit = "";
        Matcher indexer = INDEXER_ACCESS.matcher(value);
        if (indexer.matches()) {
            String rayName = indexer.group("var");
            Variable ray = scopes.find(rayName);
            if (ray.type.isRayType()) {
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
            if (toReassign.type != t) {
                throw new TypeError(
                    format(
                        "Variable `{0}` was expected to be of type {1}, but found {2}",
                        varName,
                        toReassign.type.javaType,
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
            //switch on the type of the variable being reassigned.
            switch (toReassign.type) {
                case INT:
                    passed = MATH_GRAMMAR.validateNoThrow(value);
                    break;
                case BOOL:
                    passed = BOOL_GRAMMAR.validateNoThrow(value);
                    break;
                case STRING:
                    passed = STRING_GRAMMAR.validateNoThrow(value);
                    break;
                default: //one of the list types
                    passed = RAY_GRAMMAR.categorize(value) == toReassign.type;
                    arrayReinit = format("new {0}[]", toReassign.type.listOf.javaType);
            }
        }
        if (!passed) {
            throw new TypeError(
                format(
                    "Variable `{0}` was previously assigned type {1}",
                    varName,
                    toReassign.type.javaType
                ),
                line.lineNum
            );
        }
        java
            .append(varName)
            .append(" = ")
            .append(arrayReinit)
            .append(finalReplacements(value, toReassign.type))
            .append(";\n");
    }

    /**
     * handles our if statement, which is
     * <pre>
if condition:
     do stuff
     * </pre>
     * As with all conditionals, the condition does not have to be in parentheses.
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
    public void handleIf(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(IF_STMT, line.judo);
        String condition = m.group("condition");
        BOOL_GRAMMAR.validate(condition);
        scopes.pushNewScope();
        java.append("if (").append(finalReplacements(condition, Type.BOOL)).append(") ");
    }

    /**
     * Handles our elf statement, which is
     * <pre>
efl condition:
     do stuff
     * </pre>
     * We went with one word because our language is whitespace sensitive, and
     * also because we thought elf is funny. There must be an if statement already
     * "open" for an elf statement to be valid.
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
    public void handleElf(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(ELF_STMT, line.judo);
        String condition = m.group("condition");
        BOOL_GRAMMAR.validate(condition);
        scopes.pushNewScope();
        java.append("else if (").append(finalReplacements(condition, Type.BOOL)).append(") ");
    }

    /**
     * Handles our else statement. There must be an if statement already "open"
     * for an else statement to be valid.
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
    public void handleElse(Line line, StringBuilder java, ScopeStack scopes) {
        scopes.pushNewScope();
        java.append("else ");
    }

    /**
     * Handles our for range loop, which is
     * <pre>
for i in 1..10:
     do stuff
     * </pre>
     * Which translates to a normal for loop in Java.
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
    public void handleForRange(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(FORRANGE_STMT, line.judo);
        String loopVar = m.group("loopVar");
        scopes.pushNewScope();
        scopes.addToCurrScope(loopVar, new Variable(loopVar, Type.INT));

        //Groups are as follows: for (int loopVar = lo; lo < hi; loopVar+=step)
        String lo = m.group("lo");
        String hi = m.group("hi");
        MATH_GRAMMAR.validate(lo);
        MATH_GRAMMAR.validate(hi);

        String stepGroup = m.group("step");
        String step = "1";
        //step is optional and defaults to 1. null if not included.
        if (stepGroup != null && MATH_GRAMMAR.validate(stepGroup)) {
            step = stepGroup;
        }
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

    /**
     * Handles our for each statement, which is
     * <pre>
for element in array:
     do stuff
     * </pre>
     * which translates to Java's "enhanced" for loop.
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
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

    /**
     * Handles our loop statement, which is
     * <pre>
loop condition:
     do stuff
     * </pre>
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
    public void handleLoop(Line line, StringBuilder java, ScopeStack scopes) {
        scopes.pushNewScope();
        Matcher m = armMatcher(LOOP_STMT, line.judo);
        String condition = m.group("condition");
        BOOL_GRAMMAR.validate(condition); //throws on its own
        java.append("while(").append(finalReplacements(condition, Type.BOOL)).append(") ");
    }

    /**
     * Handles our print statements, which are
     * <pre>
out(expr)
outln(expr)
     * </pre>
     * out() prints the given expression, and outln() does the same but followed
     * by a newline.
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
    public void handlePrint(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(PRINT_STMT, line.judo);
        String arg = m.group("argument");
        String ln = m.group("line");
        Type argType;
        if (arg.isEmpty()) {
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
            if (!v.type.isRayType()) {
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

    /**
     * Handles assignment to rays by index, i.e:
     * <pre>
list[2] = 10
     * </pre>
     * @param line The line to process.
     * @param java The ongoing Java file to append translated java to.
     * @param scopes The ScopeStack representing current variables.
     */
    public void handleRayIndexAssignment(Line line, StringBuilder java, ScopeStack scopes) {
        Matcher m = armMatcher(INDEXER_ASSIGN, line.judo);
        String rayName = m.group("var");
        String index = m.group("index");
        String value = m.group("value");
        Variable ray = scopes.find(rayName);
        if (!ray.type.isRayType()) {
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

    /**
     * Checks if the given string contains more than type of character. Used for
     * making sure tabs and spaces aren't mixed.
     * @param s The string to check
     * @return Whether the given string contains more than one type of character.
     */
    private boolean stringIsHeterogeneous(String s) {
        if (s.isEmpty()) return false;
        char toMatch = s.charAt(0);
        for (char c : s.toCharArray()) {
            if (c != toMatch) return true;
        }
        return false;
    }

    /**
     * Simple dataclass representing a line in the original Judo file, including
     * its line number in that file. This is necessary for descriptive error
     * messages because the index into the lines array is not necessary the
     * original line number because blank lines are dropped.
     */
    private static class Line {

        final StringBuilder judo;
        final int lineNum;

        public Line(CharSequence code, int lineNum) {
            this.judo = new StringBuilder(code);
            this.lineNum = lineNum;
        }

        /**
         * @return A copy of this line, but with the Judo code being trimmed on
         * both sides. Same line number.
         */
        public Line trimmedCopy() {
            String sb = judo.toString().trim();
            return new Line(sb, lineNum);
        }

        public String toString() {
            return format("{0}|{1}", lineNum, judo);
        }
    }
}
