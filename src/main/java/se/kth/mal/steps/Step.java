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
      return new Step(asset, this.to, this.from);
   }

   public int printCast(PrintWriter writer) {
      if (!getFirstCast().isEmpty()) {
         writer.printf("if (%s.this instanceof %s) {\n", asset, getFirstCast());
         return 1;
      }
      return 0;
   }

   public void print() {
      System.out.format("Asset [%s]$[%s] to [%s]$[%s]\n", asset, from, getTargetAsset(), to);
   }

}
