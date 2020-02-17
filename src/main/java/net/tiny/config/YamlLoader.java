package net.tiny.config;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class YamlLoader {

    protected static Logger LOGGER = Logger.getLogger(PropertiesLoader.class.getName());

    final static  String VAR_REGEX = "^[$][{][\\p{Alnum}[.][$][{][}]]+[}]$";
    private final static char LIST_HYPHEN = '-';
    private final static char COMMENT_EXCITE = '#';
    private final static char COLON = ':';

    static int getFirstBlanks(String line) {
        int len = line.length();
        int i = 0;
        while(line.charAt(i++) == ' ' && i < len);
        return i - 1;
    }

    static StringBuffer getPrekey(StringBuffer buffer, int level) {
        char[] array = buffer.toString().toCharArray();
        int c = 0;
        int len = 0;
        while(c < level) {
            if(array[len++] == '.')
                c++;
        }
        buffer.setLength(len);
        return buffer;
    }

    public static Properties load(Reader reader, PropertiesSupport.Listener listener) throws IOException {
        return read(reader, listener).get();
    }

    public static PropertiesSupport read(Reader reader, PropertiesSupport.Listener listener) throws IOException {
        final PropertiesSupport support = new PropertiesSupport(listener);
        String key = null;
        String value = null;
        int level = 0;
        int lastBlanks = 0;
        int blanks = 0;
        StringBuffer prekey = new StringBuffer();
        boolean hyphen = false;

        LineNumberReader lineReader = new LineNumberReader(reader);
        String line;
        while((line = lineReader.readLine()) != null) {
            int pos = line.indexOf(COMMENT_EXCITE); //#
            if(pos != -1) {
                line = line.substring(0, pos);
            }
            if(line.length() == 0)
                continue;

            pos = line.indexOf(LIST_HYPHEN); // -
            if (pos != -1) {
                String prefix = line.substring(0, pos);
                if (prefix.trim().isEmpty()) {
                    hyphen = true;
                    if (value.isEmpty()) {
                        //value = value.concat("[");
                    } else {
                        value = value.concat(",");
                    }
                    String var  = line.substring(pos+2).trim();
                    if (Pattern.matches(YamlLoader.VAR_REGEX, var)) {
                        value = value.concat(var);
                    } else {
                        value = value.concat("\"")
                                .concat(var)
                                .concat("\"");
                    }
                    continue;
                }
            }

            pos = line.indexOf(COLON); //:
            if(pos == -1) {
                //
                LOGGER.warning(String.format("Illegal yaml format whitout colon. %d: '%s'", lineReader.getLineNumber(), line));
                continue;
            }


            if (hyphen) { // hyphen list end
                hyphen = false;
                // put hyphen list value
                level = blanks / 2;
                prekey = getPrekey(prekey, level);
                key = prekey.toString().concat(key.trim());
                support.put(key, value);
            }

            key = line.substring(0, pos);
            value = line.substring(pos+1).trim();
            blanks = getFirstBlanks(key);
            if(blanks < lastBlanks) {
                level = level - ((lastBlanks - blanks) / 2);
                prekey = getPrekey(prekey, level);
            }
            if(blanks == 0) {
                level = 0;
                prekey.setLength(0);
            }
            if(value.isEmpty()) {
                level++;
                prekey.append(key.trim()).append(".");
            } else {
                key = prekey.toString().concat(key.trim());
                support.setProperty(key, value);
            }
            lastBlanks = blanks;
        }

        if (hyphen) { // The last hyphen list to close
            hyphen = false;
            // put hyphen list value
            //value = value.concat("]");
            if (level > 0)
                prekey = getPrekey(prekey, (level-1));
            key = prekey.toString().concat(key.trim());
            support.setProperty(key, value);
        }
        return support;
    }


}
