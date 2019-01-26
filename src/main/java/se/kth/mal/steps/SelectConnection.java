package se.kth.mal.steps;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;

public class SelectConnection extends Connection {
   public List<Step>   steps     = new ArrayList<>();
   public List<String> operators = new ArrayList<>();

   public void update() {
      asset = steps.get(0).getTargetAsset();
      multiplicity = "*";
      previousMultiplicity = "*";
   }

   @Override
   public Connection reverse() {

      SelectConnection connection = new SelectConnection();
      connection.previousAsset = this.asset;
      connection.previousCast = this.cast;
      connection.previousField = this.field;
      connection.previousMultiplicity = this.multiplicity;
      connection.asset = this.previousAsset;
      connection.cast = this.previousCast;
      connection.field = this.previousField;
      connection.multiplicity = this.previousMultiplicity;

      for (Step step : steps) {
         connection.steps.add(step.reverse(step.getTargetAsset()));
      }
      connection.operators = operators;
      return connection;
   }

   @Override
   public String print(PrintWriter writer, String prefix, String setSuffix) {
      List<String> sets = new ArrayList<>();
      for (Step step : steps) {
         String set = RandomStringUtils.randomAlphabetic(5);
         writer.printf("Set<%s> %s = new HashSet<>(); // select child\n", asset, set);
         step.print(writer, set + ".add(%s); // adding to select set\n", setSuffix, false);
         sets.add(set);
      }

      for (int i = 1; i < sets.size(); i++) {
         String operator = operators.get(i - 1);
         if (operator.equals("\\/")) {
            writer.printf("%s.addAll(%s); // union\n", sets.get(0), sets.get(i));
         }
         else if (operator.equals("/\\")) {
            writer.printf("%s.retainAll(%s); // intersection\n", sets.get(0), sets.get(i));
         }
      }

      writer.printf("for (%s %s : %s) { // iterating final select set\n", asset, decapitalize(asset), sets.get(0));
      if (!cast.isEmpty()) {
         writer.printf("if (%s instanceof %s) { // select cast\n", decapitalize(asset), cast);
         prefix = String.format("((%s) %s).", cast, decapitalize(asset));
      }
      else {
         prefix = decapitalize(asset) + ".";
      }
      return prefix;
   }
}
