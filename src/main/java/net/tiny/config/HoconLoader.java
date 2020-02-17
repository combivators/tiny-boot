package net.tiny.config;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;
import java.util.Set;


public class HoconLoader extends PropertiesLoader {


    static StreamTokenizer createStreamTokenizer(Reader reader) {
        StreamTokenizer tokenizer = new StreamTokenizer(reader);
        tokenizer.resetSyntax();
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('_', '_');
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


    private static void include(String href, Deque<String> parents, PropertiesSupport support) {
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
                //File file = new File(this.resource, res); //TODO
                File file = new File(res);
                url = file.toURI().toURL();
            } else if(href.startsWith("url")) {
                res = href.substring(href.indexOf("(")+1, href.lastIndexOf(")"));
                url = new URL(res);
            } else {
                LOGGER.warning(String.format("Unknow resource '%s'", href));
                return;
            }
            // Load included config values
            Properties prop = load(url.openStream(), guessType(res), support.getListener());
            if(parentKey.length() == 0) {
                support.putAll(prop);
            } else {
                // Set included config values into parent
                Set<String> names = prop.stringPropertyNames();
                String pk = parentKey.toString().trim();
                for(String name : names) {
                    support.setProperty((pk + name), prop.getProperty(name));
                }
            }
        } catch(IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public static Properties load(Reader reader, PropertiesSupport.Listener listener) throws IOException {
        return read(reader, listener).get();
    }

    public static PropertiesSupport read(Reader reader, PropertiesSupport.Listener listener) throws IOException {
        final PropertiesSupport support = new PropertiesSupport(listener);

        final StreamTokenizer tokenizer = createStreamTokenizer(reader);
        final Deque<String> parents = new ArrayDeque<String>();
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
                        include(buffer.toString(), parents, support);
                        buffer.setLength(0);
                        key = null;
                    } else {
                        support.setProperty(key, buffer.toString());
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
                    if (buffer.length() > 0) {
                        buffer.append(" ");
                    }
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
//                            if (buffer.length() > 0) {
//                                buffer.append(" ");
//                            }
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
                            key = key.trim().concat(buffer.toString().trim());
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
            //case COLON: // ':' or '='
            case EQUALS: // '='
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
            case BLANK:
                if (buffer.length() > 0) {
                    buffer.append(BLANK);
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

        return support;
    }
}
