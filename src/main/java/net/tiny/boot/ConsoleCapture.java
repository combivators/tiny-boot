package net.tiny.boot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public abstract class ConsoleCapture {

    static class HistoryPrintStream extends PrintStream {
        private PrintStream originalPrintStream = null;
        private ByteArrayOutputStream bout = new ByteArrayOutputStream();

        public HistoryPrintStream(PrintStream stream) {
            super(stream);
            originalPrintStream = stream;
        }

        public void write(byte[] buf, int off, int len) {
            bout.write(buf, off, len);
            super.write(buf, off, len);
        }

        public void write(int b) {
            bout.write(b);
            super.write(b);
        }

        public String toString() {
            return bout.toString();
        }

        public void reset() {
            bout.reset();
        }

        public PrintStream getOriginalStream() {
            return originalPrintStream;
        }

        public int length() {
            return bout.size();
        }

        public void close() {
            try {
                bout.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            super.close();
        }
    }

    final HistoryPrintStream historyPrintStream;
    private boolean enableHistory = false;
    private String history = null;
    private int historyLength = 0;

    protected ConsoleCapture(PrintStream out) {
        historyPrintStream = new HistoryPrintStream(out);
    }

    abstract void resetStream(PrintStream out);

    public void enable(boolean flag) {
        enableHistory = flag;
        if (enableHistory) {
            historyPrintStream.reset();
            resetStream(historyPrintStream);
        } else {
            resetStream(historyPrintStream.getOriginalStream());
        }
    }

    public boolean isEnable() {
        return enableHistory;
    }

    public void clear() {
        if (enableHistory) {
            historyPrintStream.reset();
            history = null;
            historyLength = 0;
        }
    }

    public String getHistory() {
        if (enableHistory) {
            return historyPrintStream.toString();
        }
        return "";
    }

    public boolean contains(String str) {
        if (!enableHistory) {
            return false;
        }

        if (null == history) {
            history = historyPrintStream.toString();
        } else if (null != history
                && historyLength != historyPrintStream.length()) {
            history = historyPrintStream.toString();
        }

        if (null != history) {
            historyLength = historyPrintStream.length();
            return (0 > history.indexOf(str) ? false : true);
        }
        return false;
    }

    @Override
    protected void finalize() {
        enable(false);
    }

    public static ConsoleCapture out = new StdoutCapture();
    public static ConsoleCapture err = new StderrCapture();

    static class StdoutCapture extends ConsoleCapture {
        private StdoutCapture() {
            super(System.out);
        }

        @Override
        void resetStream(PrintStream out) {
            System.setOut(out);
        }
    }

    static class StderrCapture extends ConsoleCapture {
        private StderrCapture() {
            super(System.err);
        }

        @Override
        void resetStream(PrintStream out) {
            System.setErr(out);
        }
    }
}
