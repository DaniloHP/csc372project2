package parser.errors;

public class IndentationError extends ParseError {

    public IndentationError(String message) {
        super(message);
    }

    public IndentationError(int line) {
        super("Indentation error");
        this.lineNumber = line;
    }

    public IndentationError(String message, int line) {
        super(message, line);
    }

    public String toString() {
        if (lineNumber > 0) {
            return String.format("\n\nIndentation error at line %d: %s", lineNumber, message);
        } else {
            return "\n\n" + message;
        }
    }
}
