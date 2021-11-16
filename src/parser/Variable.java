package parser;

public class Variable {

    public final String identifier;
    public final Type type;

    public Variable(String identifier, Type type) {
        this.identifier = identifier;
        this.type = type;
    }
}
