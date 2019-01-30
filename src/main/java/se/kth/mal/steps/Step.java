package se.kth.mal.steps;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A step holds information regarding a reached attack step. It contains a list
 * of connection steps to take to get from one asset and attack step to another.
 */
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

   /**
    * Gets the last connections asset. It is also this asset on which the target
    * attack step must exist.
    *
    * @return Target asset name
    */
   public String getTargetAsset() {
      if (connections.isEmpty()) {
         return asset;
      }
      return connections.get(connections.size() - 1).getCastedAsset();
   }

   /**
    * Returns previous assets casting on the first connection. If this returns
    * anything then the parent asset must be cast before we can translate the
    * connections into java code.
    *
    * @return Asset name
    */
   public String getFirstCast() {
      if (connections.isEmpty()) {
         return "";
      }
      return connections.get(0).previousCast;
   }

   /**
    * Creates a reversed step with all connections reversed.
    *
    * @param asset
    *           The new source asset, usually this steps target asset.
    * @return Reversed step
    */
   public Step reverse(String asset) {
      Step step = new Step(asset, to, from);
      for (Connection connection : connections) {
         step.connections.add(0, connection.reverse());
      }
      return step;

   }

   /**
    * Casts the asset if it was extending something else.
    *
    * @param writer
    *           Writer to print to.
    * @return An integer used to count the number of opening brackets.
    */
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

   /**
    * Prints the step and all its connections.
    *
    * @param writer
    *           Writer to print to.
    * @param format
    *           Format to print the final prefix in.
    * @param suffix
    *           Optional suffix to append the prefixes + sets.
    * @param endStep
    *           False when the reached attack step shall be appended to the
    *           final prefix. True if not.
    */
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
         writer.println("}");
      }
   }

}
