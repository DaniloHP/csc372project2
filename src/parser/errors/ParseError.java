package parser.errors;

/**
 * RuntimeException-extending class upon which all of our errors are based. Has
 * a line number which is optionally used in printouts to help descriptiveness.
 * Unfortunately, many uses of this class's children do not include the line
 * number because they are, call-stack wise, very far away from the Parser,
 * which is the only class that actually know what line number is currently
 * being parsed. Given more time, I would try to find a solution to this issue.
 *
 * All subclasses basically just add different toString() methods for clearer
 * error printouts.
 */
public abstract class ParseError extends RuntimeException {

    protected int lineNumber;
    protected String message;

    public ParseError() {
        super();
        this.message = "";
        this.lineNumber = -1;
    }

    public ParseError(String message) {
        this();
        this.message = message;
    }

    public ParseError(String message, int lineNumber) {
        this(message);
        this.lineNumber = lineNumber;
    }
}
