package md2html;

import java.util.*;

import static md2html.MarkdownSource.END;

public class MdParser {
    private final MarkdownSource source;
    private Deque<String> stack = new ArrayDeque<>();

    public MdParser(MarkdownSource source) {
        this.source = source;
    }

    public String parse() throws MarkdownException {
        StringBuilder sb = new StringBuilder();
        source.nextChar();
        skipLineBreaks();
        while (!test(END)) {
            parseParagraph(sb);
            skipLineBreaks();
        }
        return sb.toString();
    }

    private String parseParagraph(StringBuilder ans) throws MarkdownException {
        int headerLvl = 0;
        while (test('#')) {
            headerLvl++;
            source.nextChar();
        }
        if (Character.isWhitespace(source.getChar()) && headerLvl > 0) {
            skipSpaces();
            ans.append(String.format("<h%d>", headerLvl));
            stack.add(String.format("</h%d>\n", headerLvl));
        } else {
            ans.append("<p>");
            stack.add("</p>\n");
            ans.append(repeat('#', headerLvl));
        }

        ans.append(getPartOfText());
        return ans.toString();
    }

    private String getPartOfText() throws MarkdownException {
        StringBuilder ans = new StringBuilder();
        while (true) {
            if (test('\n') || test('\r')) {
                source.nextChar();
                if (test('\n') || test('\r') || test(END)) {
                    return clearStack(ans);
                } else {
                    ans.append('\n');
                }
            }
            if (test(END)) {
                return clearStack(ans);
            }
            char c = source.getChar();
            String tag = "" + c;
            switch (c) {
                case '>':
                case '<':
                case '&':
                    ans.append(htmlChar(tag));
                    break;
                case '`':
                case '~':
                    ans.append(checkForTag(tag));
                    break;
                case '*':
                case '_':
                    ans.append(checkForStrong(tag));
                    break;
                case '-':
                case '+':
                    ans.append(checkForDoubleTag(tag));
                    break;
                case '\\':
                    ans.append(source.nextChar());
                    source.nextChar();
                    break;
                case ']':
                    if (stack.getLast().equals("[")) {
                        stack.removeLast();
                        source.nextChar();
                        return ans.toString();
                    }
                case '[':
                    stack.add("[");
                    source.nextChar();
                    String annotationForLink = getPartOfText();
                    source.nextChar();
                    String link = getTextBlock(')');
                    source.nextChar();
                    ans.append(String.format("<a href='%s'>%s</a>", link, annotationForLink));
                    break;
                case '!':
                    if (source.nextChar() != '[') {
                        ans.append(c);
                        break;
                    }
                    source.nextChar();
                    String annotationForImage = getTextBlock(']');
                    source.nextChar();
                    source.nextChar();
                    String imageLink = getTextBlock(')');
                    source.nextChar();
                    ans.append(String.format("<img alt='%s' src='%s'>", annotationForImage, imageLink));
                    break;
                default:
                    ans.append(c);
                    source.nextChar();
            }
        }
    }

    private String getTextBlock(char c) throws MarkdownException {
        StringBuilder ans = new StringBuilder();
        while (!test(c)) {
            ans.append(source.getChar());
            source.nextChar();
        }
        return ans.toString();
    }

    private String clearStack(StringBuilder sb) {
        while (stack.size() > 0) {
            sb.append(stack.removeLast());
        }
        return sb.toString();
    }

    private void skipSpaces() throws MarkdownException {
        while (Character.isWhitespace(source.getChar())) {
            source.nextChar();
        }
    }

    private String htmlChar(String s) throws MarkdownException {
        String ans;
        switch (s) {
            case "<":
                ans = "&lt;";
                break;
            case ">":
                ans = "&gt;";
                break;
            case "&":
                ans = "&amp;";
                break;
            default:
                throw new MarkdownException(source.line, source.posInLine, "unknown symbol");
        }
        source.nextChar();
        return ans;
    }

    private String getHtmlTag(String c, boolean isOpenTag) throws MarkdownException {
        switch (c) {
            case "`":
                return isOpenTag ? "<code>" : "</code>";
            case "*":
                return isOpenTag ? "<em>" : "</em>";
            case "_":
                return isOpenTag ? "<em>" : "</em>";
            case "--":
                return isOpenTag ? "<s>" : "</s>";
            case "++":
                return isOpenTag ? "<u>" : "</u>";
            case "~":
                return isOpenTag ? "<mark>" : "</mark>";
            case "**":
                return isOpenTag ? "<strong>" : "</strong>";
            case "__":
                return isOpenTag ? "<strong>" : "</strong>";
            default:
                throw new MarkdownException(source.line, source.posInLine, "unknown symbol " + c);

        }
    }

    private String checkForStrong(String tag) throws MarkdownException {
        char nextChar = source.nextChar();
        if (("" + nextChar).equals(tag)) {
            return checkForTag(tag + tag);
        } else {
            if (stack.getLast().equals(getHtmlTag(tag, false))) {
                return stack.removeLast();
            }
            if (!Character.isWhitespace(nextChar)) {
                stack.add(getHtmlTag(tag, false));
                return getHtmlTag(tag, true);
            } else {
                return tag;
            }
        }
    }

    private String checkForDoubleTag(String tag) throws MarkdownException {
        String nextChar = "" + source.nextChar();
        if (nextChar.equals(tag)) {
            return checkForTag(tag + tag);
        } else {
            return tag;
        }
    }

    private String checkForTag(String tag) throws MarkdownException {
        if (stack.getLast().equals(getHtmlTag(tag, false))) {
            source.nextChar();
            return stack.removeLast();
        } else if (!Character.isWhitespace(source.nextChar())) {
            stack.add(getHtmlTag(tag, false));
            return getHtmlTag(tag, true);
        } else {
            return tag;
        }
    }

    private boolean test(char c) {
        return source.getChar() == c;
    }

    private String repeat(char ch, int cnt) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cnt; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    private void skipLineBreaks() throws MarkdownException {
        while (test('\n') || test('\r')) {
            source.nextChar();
        }
    }
}