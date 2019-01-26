package se.kth.mal.steps;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Step {
   public String           asset;
   public String           from;
   public List<Connection> connections = new ArrayList<>();
   public String           to;

   public Step(String asset, String from, String to) {
      this.asset = asset;
      this.from = from;
      this.to = to;
   }

   public String getTargetAsset() {
      if (connections.isEmpty()) {
         return asset;
      }
      return connections.get(connections.size() - 1).getCastedAsset();
   }

   public String getFirstCast() {
      if (connections.isEmpty()) {
         return "";
      }
      return connections.get(0).previousCast;
   }

   public Step reverse(String asset) {
      Step step = new Step(asset, to, from);
      for (Connection connection : connections) {
         step.connections.add(0, connection.reverse());
      }
      return step;

   }

   public int printCast(PrintWriter writer) {
      if (!getFirstCast().isEmpty()) {
         writer.printf("if (%s.this instanceof %s) {\n", asset, getFirstCast());
         return 1;
      }
      return 0;
   }

   public void print(PrintWriter writer, String format) {
      print(writer, format, "");
   }

   public void print(PrintWriter writer, String format, String suffix) {
      print(writer, format, suffix, true);
   }

   public void print(PrintWriter writer, String format, String suffix, boolean endStep) {
      int close = printCast(writer);
      String prefix = "";
      for (Connection connection : connections) {
         prefix = connection.print(writer, prefix, suffix);
         close += (connection.cast.isEmpty() ? 1 : 2);
      }
      if (endStep) {
         writer.printf(format, prefix + to);
      }
      else {
         writer.printf(format, prefix.substring(0, prefix.length() - 1));
      }
      while (close-- > 0) {
         writer.println("}//stepend");
      }
   }

}
