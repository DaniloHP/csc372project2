package parser;

public enum Type {
    INT("int", null),
    BOOL("boolean", null),
    STRING("String", null),
    INT_LIST("int[]", INT),
    STRING_LIST("String[]", STRING),
    BOOL_LIST("boolean[]", BOOL);

    public final String label;
    public final Type listOf;

    Type(String label, Type listOf) {
        this.label = label;
        this.listOf = listOf;
    }

    public boolean isArray() {
        return this.label.endsWith("[]");
    }

    public String toString() {
        return this.label;
    }
}
