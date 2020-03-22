package md2html;

public class StringMdSource extends MarkdownSource{
    private final String data;

    public StringMdSource(final String data) throws MarkdownException {
        this.data = data + END;
    }

    @Override
    protected char readChar() {
        return data.charAt(pos);
    }
}
