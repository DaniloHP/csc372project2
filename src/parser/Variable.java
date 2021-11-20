package parser;

public class Variable {

    public final String identifier;
    public final Type type;

    public Variable(String identifier, Type type) {
        this.identifier = identifier;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Variable variable = (Variable) o;
        return identifier.equals(variable.identifier) && type == variable.type;
    }
}
