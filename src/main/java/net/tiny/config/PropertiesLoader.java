package net.tiny.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Properties;
import java.util.logging.Logger;

public class PropertiesLoader {

    protected static Logger LOGGER = Logger.getLogger(PropertiesLoader.class.getName());

    final static char COMMENT_EXCITE = '#';
    final static char DOLLAR_SYMBOL = '$';
    final static char BRACKET_START = '{';
    final static char BRACKET_END = '}';
    final static char EQUALS = '=';
    final static char COLON = ':';
    final static char QUOTE = '\'';
    final static char DOUBLE_QUOTE = '"';
    final static char BLANK = ' ';
    final static String INCLUDE = "include";

    static ContextHandler.Type guessType(String resource) {
        int pos = resource.lastIndexOf(".");
        ContextHandler.Type type = ContextHandler.Type.HOCON;
        if(pos > 0) {
            String ext = resource.substring(pos+1);
            if(ContextHandler.Type.JSON.name().equalsIgnoreCase(ext)) {
                type = ContextHandler.Type.JSON;
            } else if(ContextHandler.Type.PROPERTIES.name().equalsIgnoreCase(ext)) {
                type = ContextHandler.Type.PROPERTIES;
            } else if("conf".equalsIgnoreCase(ext)) {
                type = ContextHandler.Type.HOCON;
            } else if("yml".equalsIgnoreCase(ext) || "yaml".equalsIgnoreCase(ext) ) {
                type = ContextHandler.Type.YAML;
            } else {
                throw new IllegalArgumentException(String.format("Unspuuort type resource '%s'", resource));
            }
        }
        return type;
    }

    public static Properties load(Reader reader, PropertiesSupport.Listener listener) throws IOException {
        return read(reader, listener).get();
    }

    public static PropertiesSupport read(Reader reader, PropertiesSupport.Listener listener) throws IOException {
        final PropertiesSupport support = new PropertiesSupport();
        support.load(reader);
        return support;
    }


    public static Properties load(InputStream in, ContextHandler.Type type, PropertiesSupport.Listener listener) {
        try {
            Properties properties = null;
            switch(type) {
            case HOCON:
                properties = HoconLoader.load(new BufferedReader(new InputStreamReader(in, "UTF-8")), listener);
                break;
            case JSON:
                properties = JsonLoader.load(new BufferedReader(new InputStreamReader(in, "UTF-8")), listener);
                break;
            case YAML:
                properties = YamlLoader.load(new BufferedReader(new InputStreamReader(in, "UTF-8")), listener);
                break;
            case PROPERTIES:
                properties = PropertiesLoader.load(new BufferedReader(new InputStreamReader(in, "UTF-8")), listener);
                break;
            }
            return properties;
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }


    static StreamTokenizer createStreamTokenizer(Reader reader) {
        StreamTokenizer tokenizer = new StreamTokenizer(reader);
        tokenizer.resetSyntax();
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('_', '_');
        tokenizer.whitespaceChars(' ', ' ');
        tokenizer.whitespaceChars('\t', '\t');
        tokenizer.whitespaceChars('\n', '\n');
        tokenizer.whitespaceChars('\r', '\r');
        tokenizer.quoteChar(QUOTE);
        tokenizer.quoteChar(DOUBLE_QUOTE);
        //tokenizer.parseNumbers();
        tokenizer.eolIsSignificant(true);
        //tokenizer.slashStarComments(true);
        tokenizer.slashSlashComments(true);
        return tokenizer;
    }


    static void print(String config, PrintStream out) throws IOException {
        StreamTokenizer tokenizer = createStreamTokenizer(new StringReader(config));
        print(tokenizer, out);
    }

    static void print(StreamTokenizer tokenizer, PrintStream out) throws IOException {
        int token;

        while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            switch (token) {
            case StreamTokenizer.TT_EOL:
                out.println("<EOL/>");
                break;
            case StreamTokenizer.TT_NUMBER:
                out.println("<number>" + tokenizer.nval + "</number>");
                break;
            case StreamTokenizer.TT_WORD:
                out.println("<word>" + tokenizer.sval + "</word>");
                break;
            case QUOTE:
                out.println("<char>" + tokenizer.sval + "</char>");
                break;
            case DOUBLE_QUOTE:
                out.println("<string>" + tokenizer.sval + "</string>");
                break;
            default:
                out.print("<token>" + (char)tokenizer.ttype + "</token>");
            }
        }
    }
}
