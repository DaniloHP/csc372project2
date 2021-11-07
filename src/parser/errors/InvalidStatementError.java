package parser.errors;

public class InvalidStatementError extends ParseError {

    public InvalidStatementError(String message) {
        super(message);
    }

    public InvalidStatementError(int line) {
        super("Invalid statement");
        this.lineNumber = line;
    }

    public InvalidStatementError(String message, int line) {
        super(message, line);
    }

    public String toString() {
        if (lineNumber > 0) {
            return String.format("\n\nInvalid statement at line %d: %s", lineNumber, message);
        } else {
            return "\n\n" + message;
        }
    }
}
