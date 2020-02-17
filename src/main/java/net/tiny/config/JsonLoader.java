package net.tiny.config;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

public class JsonLoader extends PropertiesLoader {

    public static Properties load(Reader reader, PropertiesSupport.Listener listener) throws IOException {
        return read(reader, listener).get();
    }

    public static PropertiesSupport read(Reader reader, PropertiesSupport.Listener listener) throws IOException {
        final PropertiesSupport support = new PropertiesSupport(listener);
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
                    key = keyBuffer.toString();
                    support.setProperty(key, value);
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
            case COLON: // ':'
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
        return support;
    }
}
