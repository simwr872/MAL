package se.kth.mal.steps;

public class DebugInfo {
   public int    startLine;
   public int    startChar;
   public int    endLine;
   public int    endChar;
   public String raw;

   public DebugInfo(String raw, int startLine, int startChar, int endLine, int endChar) {
      this.raw = raw;
      this.startLine = startLine;
      this.startChar = startChar;
      this.endLine = endLine;
      this.endChar = endChar;
   }

   public void print() {
      System.err.printf("Raw string '%s'\n", raw);
      System.err.printf("  Start: Ln %s, Col %s\n", startLine, startChar);
      System.err.printf("  End: Ln %s, Col %s\n", endLine, endChar);
   }
}
