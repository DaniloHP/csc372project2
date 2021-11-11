package tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.errors.IndentationError;

public class ParserTests {

    static final String OUT_DIR = "src/tests/generated-java/";

    @Test
    void testCountIndents() {
        final Parser p = new Parser("    ", "");
        assertEquals(1, p.countIndents("    "));
        assertEquals(2, p.countIndents("        "));
        assertEquals(3, p.countIndents("            "));
        assertThrows(IndentationError.class, () -> p.countIndents("  "));
        assertThrows(IndentationError.class, () -> p.countIndents("\t\t"));
        final Parser p1 = new Parser("\t", "");
        assertEquals(1, p1.countIndents("\t"));
        assertEquals(2, p1.countIndents("\t\t"));
        assertEquals(3, p1.countIndents("\t\t\t"));
        assertThrows(IndentationError.class, () -> p1.countIndents("    "));
        final Parser p2 = new Parser("\t\t", "");
        assertEquals(1, p2.countIndents("\t\t"));
        assertEquals(2, p2.countIndents("\t\t\t\t"));
        assertThrows(IndentationError.class, () -> p2.countIndents("\t"));
    }

    @Test
    void testAssignments() {
        System.out.println("<!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!>");
        System.out.println("<! Make sure you're using Java 8 to test !>");
        System.out.println("<!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!>");
        final Parser p = new Parser("judo-files/assign.judo");
        String className = "TestAssignments";
        String code = p.parse(className);
        assertTrue(runGeneratedJava(code, className));
    }

    public boolean runGeneratedJava(String code, String className) {
        System.out.printf("Running test for class %s\n", className);
        String classFileName = OUT_DIR + className;
        String fileName = classFileName + ".java";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(code);
        } catch (IOException e) {
            //don't particularly care about failures here
            e.printStackTrace();
        }
        try {
            Runtime.getRuntime().exec(String.format("javac %s", fileName));
            Runtime.getRuntime().exec(String.format("java %s", classFileName));
        } catch (IOException e) {
            Assertions.fail(String.format("Class %s crashed when running: %s\n", className, e));
            return false;
        }
        System.out.printf("Class %s didn't crash\n", className);
        return true;
    }

    public String inputStreamToString(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (stream.available() > 0) {
            sb.append(new String(stream.readAllBytes()));
        }
        return new String(sb);
    }
}
