import parser.Parser;

public class Translator {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Expected the filename of a Judo file.");
        }
        Parser parser = new Parser(args[1]);
    }
}
