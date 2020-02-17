package net.tiny.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public final class ConfigurationHandler implements ContextHandler {

    private static Logger LOGGER = Logger.getLogger(ConfigurationHandler.class.getName());

    private final static char COMMENT_EXCITE = '#';
    private final static char DOLLAR_SYMBOL = '$';
    private final static char BRACKET_START = '{';
    private final static char BRACKET_END = '}';
    private final static char EQUALS = '=';
    private final static char COLON = ':';
    private final static char QUOTE = '\'';
    private final static char DOUBLE_QUOTE = '"';
    private final static String INCLUDE = "include";

    private Listener listener = null;
    private String resource;
    private Configuration configuration;

    @Override
    public Listener getListener() {
        return listener;
    }
    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public String getResource() {
        return this.resource;
    }

    @Override
    public void setResource(String resource) {
        this.resource = resource;
    }

    @Override
    public Configuration getConfiguration() {
        if(null == this.configuration) {
            if(null != this.resource) {
                parse();
            } else {
                throw new RuntimeException("Must be set a resource before load it.");
            }
        }
        return this.configuration;
    }

    private URL toURL(String res) {
        URL url = null;
        try {
            File file = new File(res);
            if (file.exists()) {
                url = file.toURI().toURL();
            } else {
                url = new URL(res);
            }
        } catch (MalformedURLException ex) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            url = loader.getResource(res);
            if (url == null) {
                throw new RuntimeException("Not found " + res);
            }
        }
        return url;
    }


    private Type guessType(String resource) {
        int pos = resource.lastIndexOf(".");
        Type type = Type.HOCON;
        if(pos > 0) {
            String ext = resource.substring(pos+1);
            if(Type.JSON.name().equalsIgnoreCase(ext)) {
                type = Type.JSON;
            } else if(Type.PROPERTIES.name().equalsIgnoreCase(ext)) {
                type = Type.PROPERTIES;
            } else if("conf".equalsIgnoreCase(ext)) {
                type = Type.HOCON;
            } else if("yml".equalsIgnoreCase(ext) || "yaml".equalsIgnoreCase(ext) ) {
                type = Type.YAML;
            }
        }
        return type;
    }

    @Override
    public void parse() {
        parse(false);
    }

    @Override
    public void parse(boolean logging) {
        URL url = toURL(resource);
        try {
            Type type = guessType(resource);
            parse(url.openStream(), type, logging);
        } catch(IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void parse(InputStream in, Type type) {
        parse(in, type, false);
    }

    @Override
    public void parse(InputStream in, Type type, boolean logging) {
        //Properties properties = load(in, type);
        Properties properties = PropertiesLoader.load(in, type, null);
        this.configuration = new Configuration(properties, listener, logging);
        if (listener != null) {
            listener.parsed(type.name(), resource, properties.size());
        }
        //properties.list(System.out); //TODO
    }
/*
    protected Properties load(InputStream in, Type type) {
        try {
            Properties properties = new Properties();
            switch(type) {
            case HOCON:
                parseConf(new BufferedReader(new InputStreamReader(in, "UTF-8")), properties);
                break;
            case JSON:
                parseJson(new BufferedReader(new InputStreamReader(in, "UTF-8")), properties);
                //properties = JsonLoader.load(new BufferedReader(new InputStreamReader(in, "UTF-8")), null);
                break;
            case YAML:
                properties = YamlLoader.load(new BufferedReader(new InputStreamReader(in, "UTF-8")), null);
                //parseYaml(new BufferedReader(new InputStreamReader(in, "UTF-8")), properties);
                break;
            case PROPERTIES:
                properties.load(new BufferedReader(new InputStreamReader(in, "UTF-8")));
                break;
            }
            return properties;
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
*/
    /*
    private void include(String href, Deque<String> parents, Properties properties) {
        String res;
        URL url;
        final StringBuffer parentKey = new StringBuffer();
        parents.stream()
               .forEach(p -> parentKey.insert(0, ".").insert(0, p));
        try {
            if(href.startsWith("classpath")) {
                res = href.substring(href.indexOf("(")+1, href.lastIndexOf(")"));
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                url = loader.getResource(res);
                if (url == null) {
                    throw new RuntimeException("Not found " + res);
                }
            } else if(href.startsWith("file")) {
                res = href.substring(href.indexOf("(")+1, href.lastIndexOf(")"));
                File file = new File(this.resource, res);
                url = file.toURI().toURL();
            } else if(href.startsWith("url")) {
                res = href.substring(href.indexOf("(")+1, href.lastIndexOf(")"));
                url = new URL(res);
            } else {
                LOGGER.warning(String.format("Unknow resource '%1$s'", href));
                return;
            }
            Type type = guessType(res);
            // Load included config values
            Properties prop = load(url.openStream(), type);
            if(parentKey.length() == 0) {
                properties.putAll(prop);
            } else {
                // Set included config values into parent
                Set<String> names = prop.stringPropertyNames();
                for(String name : names) {
                    properties.setProperty((parentKey + name), prop.getProperty(name));
                }
            }
        } catch(IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private void parseConf(Reader reader, Properties properties) throws IOException {
        StreamTokenizer tokenizer = createStreamTokenizer(reader);
        Deque<String> parents = new ArrayDeque<String>();
        int token = 0;
        boolean ref = false;
        boolean symbol = false;
        boolean commentout = false;
        boolean included = false;
        String key = null;
        StringBuilder buffer = new StringBuilder();
        while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            switch (token) {
            case StreamTokenizer.TT_EOL:
                if(buffer.length() > 0) {
                    if(included) {
                        include(buffer.toString(), parents, properties);
                        buffer.setLength(0);
                        key = null;
                    } else {
                        properties.setProperty(key, buffer.toString());
                        key = null;
                    }
                    buffer.setLength(0);
                }
                commentout = false;
                included = false;
                break;
            case StreamTokenizer.TT_NUMBER:
                // Never come here because disable parseNumbers
                if(!commentout) {
                    // Append value number
                    buffer.append(tokenizer.nval);
                }
                break;
            case StreamTokenizer.TT_WORD:
                if(!commentout) {
                    if(buffer.length() == 0 && INCLUDE.equals(tokenizer.sval)) {
                        included = true;
                    } else if(included) {
                        // Append value word
                        buffer.append(tokenizer.sval);
                        buffer.append(" ");
                    } else {
                        if(null == key) {
                            key = tokenizer.sval;
                        } else {
                            // Append value word
                            buffer.append(tokenizer.sval);
                        }
                    }
                }
                break;
            case QUOTE:
                if(!commentout) {
                    // Append value string
                    buffer.append(tokenizer.sval);
                }
                break;
            case DOUBLE_QUOTE:
                if(!commentout) {
                    // Append value string
                    buffer.append(tokenizer.sval);
                }
                break;
            case BRACKET_START:
                if(!commentout) {
                    if(symbol) {
                        ref = true;
                        // Append value character
                        buffer.append(BRACKET_START);
                    } else {
                        if(buffer.length() > 0) {
                            key = key.concat(buffer.toString());
                            buffer.setLength(0);
                        }
                        parents.push(key);
                        key = null;
                    }
                }
                break;
            case BRACKET_END:
                if(!commentout) {
                    if(ref) {
                        // Append value character
                        buffer.append(BRACKET_END);
                        symbol = false;
                        ref = false;
                    } else {
                        parents.pop();
                    }
                }
                break;
            case COLON: // ':' or '='
            case EQUALS:
                if(!commentout) {
                    if(null != key) {
                        final StringBuffer keyBuffer = new StringBuffer(key);
                        parents.stream()
                               .forEach(p -> keyBuffer.insert(0, ".").insert(0, p));
                        key = keyBuffer.toString();
                    }
                }
                break;
            case COMMENT_EXCITE:
                commentout = true;
                break;
            case DOLLAR_SYMBOL:
                if(!commentout) {
                    if(null != key || included) {
                        // Append value character
                        buffer.append(DOLLAR_SYMBOL);
                        symbol = true;
                    }
                }
                break;
            default:
                if(!commentout) {
                    if(null != key || included) {
                        char cto = (char)tokenizer.ttype;
                        // Append value character
                        buffer.append(cto);
                    }
                }
                break;
            }
        }
    }

    private void parseJson(Reader reader, Properties properties) throws IOException {
        StreamTokenizer tokenizer = createStreamTokenizer(reader);
        Deque<String> parents = new ArrayDeque<String>();
        int token = 0;
        boolean ref = false;
        boolean symbol = false;
        boolean commentout = false;
        String key = null;
        StringBuilder buffer = new StringBuilder();
        while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            switch (token) {
            case StreamTokenizer.TT_EOL:
                commentout = false;
                if(buffer.length() > 0) {
                    String value = buffer.toString().trim();
                    if(value.endsWith(",")) { // ending is comma
                        value = value.substring(0, value.length()-1);
                    }
                    final StringBuffer keyBuffer = new StringBuffer(key);
                    parents.stream()
                           .forEach(p -> keyBuffer.insert(0, ".").insert(0, p));
                    //System.out.println(String.format("[#] %1$s - %2$s - %3$s", parents, key, keyBuffer.toString()));
                    key = keyBuffer.toString();
                    properties.setProperty(key, value);
                    key = null;
                    buffer.setLength(0);
                }
                break;
            case StreamTokenizer.TT_NUMBER:
                // Never come here because disable parseNumbers
                if(!commentout && null != key) {
                    // Append value number
                    buffer.append(tokenizer.nval);
                }
                break;
            case StreamTokenizer.TT_WORD:
                if(!commentout) {
                    if(null == key) {
                        key = tokenizer.sval;
                    } else {
                        // Append value word
                        buffer.append(tokenizer.sval);
                    }
                }
                break;
            case QUOTE:
                if(!commentout) {
                    // Append value string
                    buffer.append(tokenizer.sval);
                }
                break;
            case DOUBLE_QUOTE:
                if(!commentout) {
                    // Append value string
                    buffer.append(tokenizer.sval);
                }
                break;
            case BRACKET_START:
                if(!commentout) {
                    if(symbol) {
                        ref = true;
                        // Append value character
                        buffer.append(BRACKET_START);
                    } else {
                        if(parents != null && null != key)
                            parents.push(key);
                    }
                }
                break;
            case BRACKET_END:
                if(!commentout) {
                    if(ref) {
                        // Append value character
                        buffer.append(BRACKET_END);
                        symbol = false;
                        ref = false;
                    } else {
                        if(parents != null && !parents.isEmpty())
                            parents.pop();
                    }
                }
                break;
            case COLON: // ':' or '='
                if(buffer.length() > 0) {
                    key = buffer.toString();
                    buffer.setLength(0);
                }
                break;
            case COMMENT_EXCITE:
                commentout = true;
                break;
            case DOLLAR_SYMBOL:
                if(!commentout) {
                    if(null != key) {
                        // Append value character
                        buffer.append(DOLLAR_SYMBOL);
                        symbol = true;
                    }
                }
                break;
            default:
                if(!commentout && null != key) {
                    char cto = (char)tokenizer.ttype;
                    // Append value character
                    buffer.append(cto);
                }
                break;
            }
        }
    }

    private void parseYaml(Reader reader, Properties properties) throws IOException {
        String key = null;
        String value = null;
        int level = 0;
        int lastBlanks = 0;
        int blanks = 0;
        StringBuffer prekey = new StringBuffer();

        LineNumberReader lineReader = new LineNumberReader(reader);
        String line;
        while((line = lineReader.readLine()) != null) {
            int pos = line.indexOf(COMMENT_EXCITE); //#
            if(pos != -1) {
                line = line.substring(0, pos);
            }
            if(line.length() == 0)
                continue;
            pos = line.indexOf(COLON); //:
            if(pos == -1) {
                //
                LOGGER.warning(String.format("Illegal yaml format whitout colon. %d: '%s'", lineReader.getLineNumber(), line));
                continue;
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
                properties.put(key, value);
            }
            lastBlanks = blanks;
        }
    }

    private int getFirstBlanks(String line) {
        int len = line.length();
        int i = 0;
        while(line.charAt(i++) == ' ' && i < len);
        return i - 1;
    }

    private StringBuffer getPrekey(StringBuffer buffer, int level) {
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

    @Override
    public String toString() {
        return String.format("%s#%d:('%s')", getClass().getSimpleName(), hashCode(), String.valueOf(resource));
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

/*
    static void print(String config, PrintStream out) throws IOException {
        int token;
        StreamTokenizer tokenizer = createStreamTokenizer(new StringReader(config));
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
*/
    public static <T> Configuration config(Class<T> beanType) {
        Config annotation = beanType.getAnnotation(Config.class);
        if (annotation == null) {
            return null;
        }
        String location = annotation.location();
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource(location);
        handler.parse();
        return handler.getConfiguration().getConfiguration(annotation.value());
    }

    public static <T> T parse(Class<T> beanType) {
        Config annotation = beanType.getAnnotation(Config.class);
        if (annotation == null) {
            return null;
        }
        String location = annotation.location();
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource(location);
        handler.parse();
        Configuration config = handler.getConfiguration();
        if (config == null) {
            throw new RuntimeException(
                    String.format("'%1$s' Class annotation '@%2$s' has not been granted, or not value has been set.",
                            beanType.getName(), Config.class.getSimpleName()));
        }
        return config.getAs(beanType);
    }
}
