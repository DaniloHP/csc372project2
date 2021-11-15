package parser;

public enum Type {
    INT("int", null),
    BOOL("boolean", null),
    STRING("String", null),
    INT_LIST("int[]", INT),
    STRING_LIST("String[]", STRING),
    BOOL_LIST("boolean[]", BOOL);

    public final String javaType;
    public final Type listOf;

    Type(String javaType, Type listOf) {
        this.javaType = javaType;
        this.listOf = listOf;
    }

    public boolean isArray() {
        return this.javaType.endsWith("[]");
    }

    public String toString() {
        return this.javaType;
    }
}
