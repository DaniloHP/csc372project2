package parser.errors;

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
