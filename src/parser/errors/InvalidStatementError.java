package parser.errors;

public class InvalidStatementError extends ParseError {

    public InvalidStatementError(String message) {
        super(message);
    }

    public InvalidStatementError(String message, int line) {
        super(message, line);
    }

    public String toString() {
        if (lineNumber > 0) {
            return String.format("\nInvalidStatementError at line %d: %s", lineNumber, message);
        } else {
            return "\nInvalidStatementError: " + message;
        }
    }
}
