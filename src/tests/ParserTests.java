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
import parser.errors.InvalidStatementError;
import parser.errors.TypeError;
import parser.errors.VariableError;

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
        final Parser p = new Parser("judo-files/valid/assign.judo");
        String className = "TestAssignments";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testLoops() {
        final Parser p = new Parser("judo-files/valid/loops.judo");
        String className = "TestLoops";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testIfs() {
        final Parser p = new Parser("judo-files/valid/ifs.judo");
        String className = "TestIfs";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testRays() {
        final Parser p = new Parser("judo-files/valid/rays.judo");
        String className = "TestRays";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testIndexing() {
        final Parser p = new Parser("judo-files/valid/indexing.judo");
        String className = "TestIndexing";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testReplacement() {
        final Parser p = new Parser("judo-files/valid/replacement.judo");
        String className = "TestReplacement";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testGeneral() {
        final Parser p = new Parser("judo-files/valid/general.judo");
        String className = "TestGeneral";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testTypeErrors() {
        final Parser p = new Parser("judo-files/invalid/badtypereassign.judo");
        String className = "Test";
        assertThrows(TypeError.class, () -> p.parseFull(className));
    }

    @Test
    void testStatementErrors() {
        final Parser p = new Parser("judo-files/invalid/badprint.judo");
        String className = "TestBadPrint";
        assertThrows(InvalidStatementError.class, () -> p.parseFull(className));
    }

    @Test
    void testVariableErrors() {
        final Parser p = new Parser("judo-files/invalid/reserved.judo");
        String className = "TestBadPrint";
        assertThrows(VariableError.class, () -> p.parseFull(className));
    }

    @Test
    void testProgram1() {
        final Parser p = new Parser("judo-files/required/Program1.txt");
        String className = "TestProgram1";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testProgram2() {
        final Parser p = new Parser("judo-files/required/Program2.txt");
        String className = "TestProgram2";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testProgram3() {
        final Parser p = new Parser("judo-files/required/Program3.txt");
        String className = "TestProgram3";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testProgram4() {
        final Parser p = new Parser("judo-files/required/Program4.txt");
        String className = "TestProgram4";
        String code = p.parseFull(className);
        runGeneratedJava(code, className);
    }

    @Test
    void testProgram5() {
        final Parser p = new Parser("judo-files/required/Program5.txt");
        String className = "TestProgram5";
        String code = p.parseFull(className);
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
                .exec(String.format("sh -c javac %s && java %s", fileName, classFileName))
                .waitFor();
        } catch (IOException | InterruptedException e) {
            //TODO: this always tells you it didn't crash even when it did.
            Assertions.fail(String.format("Class %s crashed when running: %s\n", className, e));
            return;
        }
        System.out.printf("Class %s didn't crash\n", className);
    }
}
