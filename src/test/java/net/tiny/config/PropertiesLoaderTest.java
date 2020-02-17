package net.tiny.config;

import org.junit.jupiter.api.Test;

public class PropertiesLoaderTest {

    static final String LS = System.getProperty("line.separator");

    @Test
    public void testPrintHocon() throws Exception {
        String config =
        "# HOCON Comment " + LS
        + "setting {" + LS
        + "  local = en-US" + LS
        + "}" + LS
        + LS
        + "APP {" + LS
        + "  sample {" + LS
        + "    local = ${setting.local}" + LS
        + "    url = \"http://www.abc.com/\"" + LS
        + "    cost = 1080" + LS
        + "    date = 2016/09/16" + LS
        + "    time = 09:15" + LS
        + "    datetime = 2016/09/16 09:15" + LS
        + "    nested {" + LS
        + "       name = child" + LS
        + "       threshold = 1.4" + LS
        + "    }" + LS
        + "  }" + LS
        + "}" + LS
        + "// HOCON Comment " + LS
        + LS;
        PropertiesLoader.print(config, System.out);
    }

    @Test
    public void testPrintJson() throws Exception {
        String config =
        "# JSON Comment " + LS
        + "{" + LS
        + " APP {" + LS
        + "  sample {" + LS
        + "    local = ${setting.local}" + LS
        + "    url = \"http://www.abc.com/\"" + LS
        + "    cost = 1080" + LS
        + "    date = 2016/09/16" + LS
        + "    time = 09:15" + LS
        + "    datetime = 2016/09/16 09:15" + LS
        + "    nested {" + LS
        + "       name = child" + LS
        + "       threshold = 1.4" + LS
        + "    }" + LS
        + "  }" + LS
        + " }" + LS
        + "}" + LS
        + "// JSON Comment " + LS
        + LS;
        PropertiesLoader.print(config, System.out);
    }
}
