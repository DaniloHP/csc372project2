package parser;

/**
 * Simple enum representing our types. Has a slightly recursive structure in that
 * ray/list types point at the Type that they are a list of. Also has the legal
 * Java type identifier associated with each type.
 */
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

    /**
     * @return Whether this type is one of int[], String[], or boolean[]
     */
    public boolean isRayType() {
        return this.javaType.endsWith("[]");
    }

    /**
     * @return The Java type identifier
     */
    public String toString() {
        return this.javaType;
    }
}
