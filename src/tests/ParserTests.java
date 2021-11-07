package tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import parser.errors.IndentationError;
import parser.Parser;

public class ParserTests {

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
}
