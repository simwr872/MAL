package se.kth.mal.antlr4;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.stream.Stream;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

public class MalErrorListener extends BaseErrorListener {
   private File file;
   private int  errors;

   public MalErrorListener(File file) {
      this.file = file;
      this.errors = 0;
   }

   @Override
   public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
      if (recognizer instanceof Parser) {
         Token token = (Token) offendingSymbol;
         error(this.file, line, charPositionInLine, token.getText(), msg);
      }
      else if (recognizer instanceof Lexer) {
         Lexer lexer = (Lexer) recognizer;
         CharStream input = lexer.getInputStream();
         String err = input.getText(Interval.of(lexer._tokenStartCharIndex, input.index()));
         error(this.file, line, charPositionInLine, err, msg);
      }
   }

   public int printCount(String str) {
      StringBuilder s = new StringBuilder();
      for (char c : str.toCharArray()) {
         s.append(c == '\t' ? "  " : c);
      }
      System.err.print(s);
      return s.length();
   }

   public void error(File file, int line, int index, String text, String msg) {
      if (text.equals("<EOF>")) {
         System.err.printf("%s:%s:%s: error: %s\n", file.getName(), line, index, msg);
         this.errors++;
         return;
      }
      Stream<String> lines = null;
      try {
         lines = Files.lines(file.toPath());
      }
      catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      String faultyLine = lines.skip(line - 1).findFirst().get();
      String preText = faultyLine.substring(0, index);
      String postText = faultyLine.substring(index + text.length());

      System.err.printf("%s:%s:%s: error: %s\n", file.getName(), line, index, msg);
      String pre = String.join("", Collections.nCopies(printCount(preText), " "));
      String post = String.join("", Collections.nCopies(printCount(text) - 1, "~"));
      System.err.println(postText);
      System.err.println(pre + "^" + post);
      this.errors++;
   }

   public int getErrors() {
      return errors;
   }
}
