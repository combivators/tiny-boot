package net.tiny.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class JsonParser {

    private static final Logger LOGGER  = Logger.getLogger(JsonParser.class.getName());

    //////////////////////////////////////////////////
    // Constants

    static final char LBRACKET = '[';
    static final char RBRACKET = ']';
    static final char LBRACE = '{';
    static final char RBRACE = '}';
    static final char COLON = ':';
    static final char COMMA = ',';
    static final char QUOTE = '"';
    static final String TRUE = "true";
    static final String FALSE = "false";

    //////////////////////////////////////////////////////
    // marshal
    public static String marshal(Object target) {
        StringWriter writer = new StringWriter();
        marshal(target, new PrintWriter(writer));
        return writer.toString();
    }

    public static void marshal(Object target, OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        marshal(target, writer);
    }

    public static void marshal(Object target, PrintWriter writer) {
        if(null == target)
        	return;
        boolean listed = (target instanceof Collection);
        Class<?> type = target.getClass();
        if (!listed) writer.print("{");

        if(Reflections.isJavaType(type)) {
            marshalJava(target, writer);
        } else if(Reflections.isJavaArrayType(type)) {
            marshalJavaArray(1, target, writer);
        } else if(Reflections.isCollectionType(type)) {
            marshalCollection(1, target, writer);
        } else if(target instanceof Map) {
            marshalMap(1, target, writer);
        } else {
            writer.println();
            marshalJson(1, target, writer);
            writer.println();
        }

        if(!listed)	writer.println("}");
    }

    static void marshalJson(int indent, Object target, PrintWriter writer) {
        List<Field> fields = Reflections.getFieldStream(target.getClass()).collect(Collectors.toList());
        final String prefix = indentString(indent);
        boolean first = true;
        for(Field field : fields) {
            Object value = getFieldValue(target, field);
            if(null != value) {
                if(first) {
                    first = false;
                } else {
                    writer.println(",");
                }
                printField(indent, prefix, field, value, writer);
            }
        }
    }

    static void marshalJava(Object target, PrintWriter writer) {
        writer.write(toJsonValue(0, "", target.getClass(), target, null));
    }

    static void marshalJavaArray(int num, Object target, PrintWriter writer) {
        writer.write("[");
        int length = Array.getLength(target);
        for (int i = 0; i < length; i++) {
            if(i > 0) writer.write(",");
            marshalJava(Array.get(target, i), writer);
        }
        writer.write("]");
    }

    static void marshalCollection(int num, Object target, PrintWriter writer) {
        writer.write("[");
        boolean first = true;
        Iterator<?> iterator = ((Collection<?>)target).iterator();
        while(iterator.hasNext()) {
            if(!first) {
                writer.write(",");
            } else {
                 first = false;
            }
            marshalJava(iterator.next(), writer);
        }
        writer.write("]");
    }

    @SuppressWarnings("unchecked")
    static void marshalMap(int num, Object target, PrintWriter writer) {
        boolean first = true;
        String prefix = indentString(num);
        Map<Object, Object> map = ((Map<Object, Object>)target);
        Set<Object> keys = map.keySet();
        for(Object key : keys) {
            if(!first) {
                writer.println(",");
            } else {
                writer.println();
                first = false;
            }
            Object value = map.get(key);
            if (Reflections.isCollectionType(value.getClass())) {
                writer.print(prefix);
                writer.print("\"");
                writer.print(key);
                writer.print("\" : ");
                marshalCollection(1, value, writer);
            } else {
                writer.print(prefix);
                writer.print("\"");
                writer.print(key);
                writer.print("\" : ");
                writer.print(toJsonValue(num+1, prefix, value.getClass(), value, null));
            }
        }
        writer.println();
    }


    private static String indentString(int num) {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<num; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    private static void printField(int indent, String prefix, Field field, Object value, PrintWriter writer) {
        String string = toJsonValue(indent, prefix, value.getClass(), value, Reflections.getFieldGenericType(field));
        writer.print(prefix);
        writer.print("\"");
        writer.print(field.getName());
        writer.print("\" : ");
        writer.print(string);
    }


    private static String toJsonValue(int indent, String prefix, Class<?> type, Object value, Type fieldType) {
        String string = "";
        if (type.isEnum()) {
            string = "\"" + value.toString() + "\"";
        } else if (value instanceof LocalDate) {
            string = "\"" + DateTimeFormatter.ofPattern("yyyy/MM/dd").format((LocalDate)value) + "\"";
        } else if (value instanceof LocalTime) {
            string = "\"" + DateTimeFormatter.ofPattern("HH:mm:ss").format((LocalTime)value) + "\"";
        } else if (value instanceof LocalDateTime) {
            string = "\"" + DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format((LocalDateTime)value) + "\"";
        } else if (value instanceof Timestamp) {
            string = "" + ((Timestamp)value).getTime();
        } else if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            string = "\"" + sdf.format((Date)value) + "\"";
        } else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            string = "\"" + (boolean) value + "\"";
        } else if (int.class.equals(type) || Integer.class.equals(type)) {
            string = "" + (int)value;
        } else if (short.class.equals(type) || Short.class.equals(type)) {
            string = "" + (short)value;
        } else if (long.class.equals(type) || Long.class.equals(type)) {
            string = "" + (long)value;
        } else if (float.class.equals(type) || Float.class.equals(type)) {
            string = "" + (float)value;
        } else if (double.class.equals(type) || Double.class.equals(type)) {
            string = "" + (double)value;
        } else if (String.class.equals(type) || Character.class.equals(type) || char.class.equals(type)) {
            string = "\"" + (String) value + "\"";
        } else if (BigDecimal.class.equals(type)) {
            string = "" + (BigDecimal) value;
        } else if (BigInteger.class.equals(type)) {
            string = "" + (BigInteger) value;
        } else if (type.isArray()) {
            string = toJsonArray(indent, value, (Class<?>)fieldType);
        } else if (value instanceof Collection) {
            string = toJsonCollection(indent, (Collection<?>) value, (Class<?>)fieldType);
        } else if (value instanceof Map) {
            String pre = indentString(indent);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.print(pre);
            pw.println("{");
            marshalMap(indent+1, value, pw);
            pw.println();
            pw.print(pre);
            pw.print("}");
            string = sw.toString();
        } else {
            String pre = indentString(indent);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.print(pre);
            pw.println("{");
            marshalJson(indent+1, value, pw);
            pw.println();
            pw.print(pre);
            pw.print("}");
            string = sw.toString();
        }
        return string;
    }


    private static String toJsonArray(int indent, Object array, Class<?> type)  {
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        writer.print("[");

        int length = Array.getLength(array);
        boolean oneline = true;
        if(length > 0) {
            Object value = Array.get(array, 0);
            if(null != value) {
                oneline = Reflections.isJavaType(value.getClass());
            }
        }
        if(!oneline) {
            writer.println();
        }

        String prefix = indentString(indent);
        boolean first = true;
        for (int i = 0; i < length; i++) {
            Object value = Array.get(array, i);
            if(null != value) {
                if(first) {
                    first = false;
                } else {
                    writer.print(",");
                    if(!oneline) {
                        writer.println();
                    }
                }
                if(!oneline) {
                    writer.print(prefix);
                }
                writer.print(toJsonValue(indent+1, prefix, value.getClass(), value, null));
            }
        }
        if(!oneline) {
            writer.println();
            writer.print(prefix);
        }
        writer.print("]");
        return sw.toString();
    }

    private static String toJsonCollection(int indent, Collection<?> list, Class<?> type) {
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        writer.print("[");
        boolean oneline = Reflections.isJavaType(type);
        if(!oneline) {
            writer.println();
        }
        String prefix = indentString(indent);
        boolean first = true;
        for (Object target : list) {
            if(first) {
                first = false;
            } else {
                writer.print(",");
                if(!oneline) {
                    writer.println();
                }
            }
            if(!oneline) {
                writer.print(prefix);
            }
            writer.print(toJsonValue(indent+1, prefix, target.getClass(), target, null));
        }

        if(!oneline) {
            writer.println();
            writer.print(prefix);
        }
        writer.print("]");
        return sw.toString();
    }

    public static String toString(Object obj) {
        StringBuilder buf = new StringBuilder();
        toString(obj, buf, 0);
        return buf.toString();
    }

    @SuppressWarnings("unchecked")
    protected static void toString(Object obj, StringBuilder buf, int indent) {
        if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            if (list.size() == 0) {
                buf.append(LBRACKET);
                buf.append(RBRACKET);
            } else {
                buf.append(LBRACKET);
                buf.append('\n');
                for (int i = 0; i < list.size(); i++) {
                    Object e = list.get(i);
                    buf.append(indent(indent));
                    toString(e, buf, indent + 2);
                    if (i < list.size() - 1)
                        buf.append(",");
                    buf.append("\n");
                }
                buf.append(indent(indent));
                buf.append(RBRACKET);
            }
        } else if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            if (map.size() == 0) {
                buf.append(LBRACE);
                buf.append(RBRACE);
            } else {
                buf.append(LBRACE);
                buf.append('\n');
                int i = 0;
                for (Map.Entry<String, Object> e : map.entrySet()) {
                    buf.append(indent(indent + 2));
                    buf.append(QUOTE);
                    buf.append(e.getKey());
                    buf.append(QUOTE);
                    buf.append(' ');
                    buf.append(COLON);
                    buf.append(' ');
                    toString(e.getValue(), buf, indent + 2);
                    if (i < map.size() - 1)
                        buf.append(",");
                    buf.append("\n");
                    i++;
                }
                buf.append(indent(indent));
                buf.append(RBRACE);
            }
        } else if ((obj instanceof Long) || (obj instanceof Boolean)) {
            buf.append(obj.toString());
        } else {
            buf.append(QUOTE);
            buf.append(obj.toString());
            buf.append(QUOTE);
        }
    }

    static String blanks = "                                                  ";
    protected static String indent(int n) {
        while (n > blanks.length()) {
            blanks = blanks + blanks;
        }
        return blanks.substring(0, n);
    }


    //////////////////////////////////////////////////////
    // unmarshal
    public static <T> T unmarshal(InputStream in, Class<T> type) {
        return unmarshal(new InputStreamReader(in), type, new Mapper());
    }

    public static <T> T unmarshal(Reader reader, Class<T> type) {
    	return unmarshal(reader, type, new Mapper());
    }

    public static <T> T unmarshal(final String value, final Class<T> type) {
    	return unmarshal(value, type, new Mapper());
    }

    public static <T> List<T> unmarshals(InputStream in, Class<T> type) {
        return unmarshals(new InputStreamReader(in), type, new Mapper());
    }

    public static <T> List<T> unmarshals(Reader reader, Class<T> type) {
    	return unmarshals(reader, type, new Mapper());
    }

    @SuppressWarnings("unchecked")
    private static <T> T unmarshal(Reader reader, Class<T> type, Mapper mapper) {
        try {
        	Object ret = new JsonParser().parse(reader);
        	if(null == ret) {
        		return null;
        	}
        	if (type.isInstance(ret)) {
        		return type.cast(ret);
        	} else if (ret instanceof Map) {
        		return mapper.convert((Map<String, ?>)ret, type);
        	} else {
        		throw new ClassCastException(String.format("Can not cast '%s' to '%s'",
        				ret.getClass().getName(), type.getSimpleName()));
        	}

        } catch (InstantiationException | IllegalAccessException | IOException e) {
            LOGGER.log(Level.WARNING, String.format("Parse json '%s' error - %s.",
                    type.getSimpleName(), e.getMessage()), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> unmarshals(Reader reader, Class<T> type, Mapper mapper) {
        try {
        	Object ret = new JsonParser().parse(reader);
        	if(null == ret) {
        		return null;
        	}
        	if (ret instanceof List) {
        		List<?> list = (List<?>)ret;
                List<T> objs = new ArrayList<>();
                for(int i=0; i<list.size(); i++) {
                	objs.add(mapper.convert((Map<String, ?>)list.get(i), type));
                }
        		return objs;
        	} else {
        		throw new ClassCastException(String.format("Can not cast '%s' to '%s'",
        				ret.getClass().getName(), type.getSimpleName()));
        	}

        } catch (InstantiationException | IllegalAccessException | IOException e) {
            LOGGER.log(Level.WARNING, String.format("Parse json '%s' error - %s.",
                    type.getSimpleName(), e.getMessage()), e);
            return null;
        }
    }

    private static <T> T unmarshal(final String value, final Class<T> type, Mapper mapper) {
        if (Reflections.isJavaType(type)
                || Reflections.isJavaArrayType(type)
                || Reflections.isCollectionType(type)) {
                String data = value;
                int pos = data.lastIndexOf("}");
                if (pos != -1) {
                    data = data.substring(0, pos+1);
                }
                if( data.startsWith("{") && data.endsWith("}")) {
                    data = data.substring(1, data.length()-1);
                }
                if (data.startsWith("\"")) {
                    data = data.substring(1, data.length()-1);
                }
                if (data.endsWith("\"")) {
                    data = data.substring(0, data.length()-1);
                }
                return mapper.convert(data, type);
            } else {
                return unmarshal(new StringReader(value), type, mapper);
            }
    }

    private static Object getFieldValue(Object obj, Field field) {
        try {
            field.setAccessible(true);
            Object value = field.get(obj);
            field.setAccessible(false);
            return value;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            LOGGER.log(Level.WARNING, String.format("Get field value '%s.%s' error : %s.",
                    obj.getClass().getSimpleName(), field.getName(), e.getMessage()), e);
            return null;
        }
    }

    static StreamTokenizer createStreamTokenizer(Reader reader) {
        StreamTokenizer tokenizer = new StreamTokenizer(reader);
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('$', '$');
        tokenizer.quoteChar(QUOTE);
        tokenizer.eolIsSignificant(false);
        //tokenizer.slashSlashComments(true);
        return tokenizer;
    }

    //////////////////////////////////////////////////
    // Parser methods

    // Simple recursive descent
    public Object parse(Reader reader) throws IOException {
        return parseTokens(createStreamTokenizer(reader));
    }

    protected Object parseTokens(StreamTokenizer tokens) throws IOException {
        int token = tokens.nextToken();
        switch (token) {
        case StreamTokenizer.TT_EOF:
            return null;
        case StreamTokenizer.TT_NUMBER:
        case StreamTokenizer.TT_WORD:
        case QUOTE:
            return parseAtomic(tokens);
        case LBRACE:
            return parseMap(tokens);
        case LBRACKET:
            return parseArray(tokens);
        default:
            throw new IOException(String.format("Unexpected token: '0x%02x' on %d", (byte)token, tokens.lineno()));
        }
    }

    protected Object parseAtomic(StreamTokenizer tokens) throws IOException {
        assert (tokens.ttype == StreamTokenizer.TT_WORD ||
                tokens.ttype == StreamTokenizer.TT_NUMBER ||
                tokens.ttype == QUOTE);
        final String word = tokens.sval;
        switch (tokens.ttype) {
        case QUOTE:
            return word;
        case StreamTokenizer.TT_NUMBER:
            return tokens.nval;
        case StreamTokenizer.TT_WORD:
        default:
            if (word.equalsIgnoreCase(TRUE))
                return Boolean.TRUE;
            if (word.equalsIgnoreCase(FALSE))
                return Boolean.FALSE;
            try {
                return parseNumber(word);
            } catch (NumberFormatException e) {
                //Ignore
            }
            return word;
        }
    }

    private Object parseNumber(String value) throws NumberFormatException {
        NumberFormatException err = null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            err = e;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            err = e;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            err = e;
        }
        throw err;
    }

    protected Object parseArray(StreamTokenizer tokens) throws IOException {
        assert (tokens.ttype == LBRACKET);
        List<Object> array = new ArrayList<>();
        loop: for (;;) {
            int token = tokens.nextToken();
            switch (token) {
            case StreamTokenizer.TT_EOL:
                break; // ignore
            case StreamTokenizer.TT_EOF:
                throw new IOException("Unexpected EOF.");
            case RBRACKET:
                break loop;
            default:
                tokens.pushBack();
                Object o = parseTokens(tokens);
                tokens.nextToken();
                if (tokens.ttype == StreamTokenizer.TT_EOF)
                    break;
                else if (tokens.ttype == RBRACKET)
                    tokens.pushBack();
                else if (tokens.ttype != COMMA)
                    throw new IOException("Missing comma in list on " + tokens.lineno());
                array.add(o);
            }
        }
        return array;
    }

    protected Object parseMap(StreamTokenizer tokens) throws IOException {
        assert (tokens.ttype == LBRACE);
        Map<String, Object> map = new LinkedHashMap<>(); // Keep insertion order
        loop: for (;;) {
            int token = tokens.nextToken();
            switch (token) {
            case StreamTokenizer.TT_EOL:
                break; // ignore
            case StreamTokenizer.TT_EOF:
                throw new IOException("Unexpected EOF.");
            case RBRACE:
                break loop;
            default:
                tokens.pushBack();
                Object name = parseTokens(tokens);
                if (tokens.ttype == StreamTokenizer.TT_EOF)
                    break;
                if (name instanceof String || name instanceof Long || name instanceof Boolean) {
                    /* ok */
                } else
                    throw new IOException(String.format("Unexpected map name type:'%s' on %d", name, tokens.lineno()));
                if (tokens.nextToken() != COLON)
                    throw new IOException(String.format("Expected ':'; found: %d on %d", tokens.ttype, tokens.lineno()));
                Object o = parseTokens(tokens);
                tokens.nextToken();
                if (tokens.ttype == StreamTokenizer.TT_EOF)
                    break;
                else if (tokens.ttype == RBRACE)
                    tokens.pushBack();
                else if (tokens.ttype != COMMA)
                    throw new IOException(String.format("Missing comma in list on %d", tokens.lineno()));
                map.put(name.toString(), o);
            }
        }
        return map;
    }

}
