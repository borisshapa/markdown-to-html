package md2html;

import java.io.*;

public class Md2Html {
    public static void main(String[] args) throws MarkdownException, IOException {
        String ans = new MdParser(new FileMdSource(args[0])).parse();

        try (FileWriter writer = new FileWriter(new File(args[1]))) {
            writer.write(ans);
        }
    }
}
