package parser.errors;

public class TypeError extends ParseError {

    public TypeError(String message) {
        super(message);
    }

    public TypeError(int line) {
        super("Invalid type");
        this.lineNumber = line;
    }

    public TypeError(String message, int line) {
        super(message, line);
    }

    public String toString() {
        if (lineNumber > 0) {
            return String.format("\nInvalid type at line %d: %s", lineNumber, message);
        } else {
            return "\nTypeError: " + message;
        }
    }
}
