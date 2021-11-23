package parser.errors;

public class VariableError extends ParseError {

    public VariableError(String message) {
        super(message);
    }

    public VariableError(String message, int line) {
        super(message, line);
    }

    public String toString() {
        if (lineNumber > 0) {
            return String.format("\nVariableError at line %d: %s", lineNumber, message);
        } else {
            return "\nVariableError: " + message;
        }
    }
}
