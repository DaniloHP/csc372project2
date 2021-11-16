package tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testLoops() {
        final Parser p = new Parser("judo-files/loops.judo");
        String className = "TestLoops";
        String code = p.parseTesting(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testIfs() {
        final Parser p = new Parser("judo-files/ifs.judo");
        String className = "TestIfs";
        String code = p.parseTesting(className);
        runGeneratedJava(code, className);
    }

    public void runGeneratedJava(String code, String className) {
        Path dir = Paths.get(OUT_DIR);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
            Runtime
                .getRuntime()
                .exec(String.format("javac %s && java %s", fileName, classFileName))
                .waitFor();
        } catch (IOException | InterruptedException e) {
            //TODO: this always tells you it didn't crash even when it did.
            Assertions.fail(String.format("Class %s crashed when running: %s\n", className, e));
            return;
        }
        System.out.printf("Class %s didn't crash\n", className);
    }
}
