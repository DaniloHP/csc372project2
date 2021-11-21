package parser.errors;

public class IndentationError extends ParseError {

    public IndentationError(String message, int line) {
        super(message, line);
    }

    public String toString() {
        if (lineNumber > 0) {
            return String.format("Indentation error at line %d: %s", lineNumber, message);
        } else {
            return message;
        }
    }
}
