package net.tiny.boot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ConsoleCaptureTest {

    static final String LFCR = System.getProperty("line.separator");

    @Test
    public void testCaptureStdoutStderr() throws Exception {
        ConsoleCapture.out.enable(true);
        ConsoleCapture.err.enable(true);

        System.out.println("STD OUT - 1.");
        System.out.println("STD OUT - 2.");
        System.err.println("STD ERR - 1.");
        System.err.println("STD ERR - 2.");
        System.out.println("STD OUT - 3.");
        System.err.println("STD ERR - 3.");

        String out = ConsoleCapture.out.getHistory();
        String err = ConsoleCapture.err.getHistory();

        assertTrue(ConsoleCapture.out.contains("OUT"));
        assertTrue(ConsoleCapture.err.contains("ERR"));

        ConsoleCapture.out.enable(false);
        ConsoleCapture.err.enable(false);
        ConsoleCapture.out.clear();
        ConsoleCapture.err.clear();

        String resultOut =
        "STD OUT - 1." + LFCR +
        "STD OUT - 2." + LFCR +
        "STD OUT - 3." + LFCR;

        assertEquals(resultOut, out);

        String resultErr =
        "STD ERR - 1." + LFCR +
        "STD ERR - 2." + LFCR +
        "STD ERR - 3." + LFCR;

        assertEquals(resultErr, err);
    }
}