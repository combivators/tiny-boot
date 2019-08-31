package net.tiny.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ConsoleFormatterTest {
    @Test
    public void testShortClassName() throws Exception {
        final String className = "org.junit.jupiter.engine.config.EnumConfigurationParameterConverter";
        ConsoleFormatter formatter = new ConsoleFormatter();
        assertEquals(className, formatter.shortClassName(70, className));
        assertEquals("o.j.jupiter.engine.config.EnumConfigurationParameterConverter", formatter.shortClassName(64, className));
        assertEquals("o.j.j.engine.config.EnumConfigurationParameterConverter", formatter.shortClassName(60, className));
        assertEquals("o.j.j.e.config.EnumConfigurationParameterConverter", formatter.shortClassName(50, className));
        assertEquals("o.j.j.e.c.EnumConfigurationParameterConverter", formatter.shortClassName(45, className));
        assertEquals("o.j.j.e.c.E", formatter.shortClassName(40, className));
    }
}
