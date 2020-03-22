package md2html;

import java.io.IOException;

public abstract class MarkdownSource {
    public static char END = '\0';

    protected int pos;
    protected int line = 1;
    protected int posInLine;
    private char c;

    protected abstract char readChar() throws IOException;

    public char getChar() {
        return c;
    }

    public char nextChar() throws MarkdownException {
        try {
            if (c == '\n') {
                line++;
                posInLine = 0;
            }
            c = readChar();
            pos++;
            posInLine++;
            return c;
        } catch (final IOException e) {
            throw error("Source read error", e.getMessage());
        }
    }

    public String getNextLine() throws MarkdownException {
        StringBuilder res = new StringBuilder();
        while (true) {
            if (this.getChar() == '\n') {
                this.nextChar();
                if (this.getChar() != '\n' && this.getChar() != END)
                    res.append('\n');
                return res.toString();
            }
            if (this.getChar() == END) {
                return res.toString();
            }
            res.append(this.getChar());
            this.nextChar();
        }
    }

    public MarkdownException error(final String format, final Object... args) throws MarkdownException {
        return new MarkdownException(line, posInLine, String.format("%d:%d: %s", line, posInLine, String.format(format, args)));
    }
}
