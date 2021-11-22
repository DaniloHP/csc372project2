import static java.text.MessageFormat.format;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import parser.Parser;

public class Translator {

    /**
     * Entrypoint to the parser. The output java file will be put into a file
     * with the same name as the input file, but the .java extension.
     * @param args Should be one path to a judo file to translate.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Expected the filename of a Judo file.");
            System.exit(1);
        }
        Parser parser = new Parser(args[0]);
        Path judoFile = FileSystems.getDefault().getPath(args[0]);
        String judoFileName = judoFile.getFileName().toString();
        int index = judoFileName.lastIndexOf('.');
        String javaFileName = index > 0 ? judoFileName.substring(0, index) : judoFileName;
        javaFileName = javaFileName.replaceAll("-", "_");
        String java = parser.parseFull(javaFileName);
        javaFileName += ".java";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(javaFileName))) {
            writer.write(java);
        } catch (IOException e) {
            System.err.println("Unable to output java file to " + javaFileName);
            System.exit(1);
        }
        System.out.println(format("Java file successfully outputted to:\n{0}", javaFileName));
    }
}
