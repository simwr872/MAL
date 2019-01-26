package se.kth.mal.steps;

import java.io.PrintWriter;

import se.kth.mal.Association;

public class Connection {
   public String previousAsset        = "";
   public String previousCast         = "";
   public String previousField        = "";
   public String previousMultiplicity = "";
   public String asset                = "";
   public String cast                 = "";
   public String field                = "";
   public String multiplicity         = "";

   public Connection() {
   }

   public Connection(String field) {
      this.field = field;
   }

   public Connection(String field, String cast) {
      this.field = field;
      this.cast = cast;
   }

   public Connection reverse() {
      Connection connection = new Connection();
      connection.previousAsset = this.asset;
      connection.previousCast = this.cast;
      connection.previousField = this.field;
      connection.previousMultiplicity = this.multiplicity;
      connection.asset = this.previousAsset;
      connection.cast = this.previousCast;
      connection.field = this.previousField;
      connection.multiplicity = this.previousMultiplicity;
      return connection;
   }

   public void associationUpdate(Association association, boolean previousAssetLeft) {
      this.previousField = (previousAssetLeft ? association.getLeftRoleName() : association.getRightRoleName());
      this.previousMultiplicity = (previousAssetLeft ? association.getLeftMultiplicity() : association.getRightMultiplicity());
      this.asset = (!previousAssetLeft ? association.getLeftAssetName() : association.getRightAssetName());
      this.multiplicity = (!previousAssetLeft ? association.getLeftMultiplicity() : association.getRightMultiplicity());
      String previous = (previousAssetLeft ? association.getLeftAssetName() : association.getRightAssetName());
      if (!previous.equals(previousAsset)) {
         previousCast = previousAsset;
         previousAsset = previous;
         // previousCast = previous;
      }
   }

   public String getCastedAsset() {
      return (cast.isEmpty() ? asset : cast);
   }

   public boolean isSet() {
      return (multiplicity.equals("*") || multiplicity.equals("1-*"));
   }

   public String decapitalize(final String line) {
      return Character.toLowerCase(line.charAt(0)) + line.substring(1);
   }

   public String print(PrintWriter writer, String prefix, String suffix) {
      if (isSet()) {
         writer.printf("for (%s %s : %s) {\n", asset, decapitalize(asset), prefix + field + suffix);
         if (!cast.isEmpty()) {
            writer.printf("if (%s instanceof %s) {\n", decapitalize(asset), cast);
            prefix = String.format("((%s) %s).", cast, decapitalize(asset));
         }
         else {
            prefix = decapitalize(asset) + ".";
         }
      }
      else {
         writer.printf("if (%s != null) {\n", prefix + field);
         if (!cast.isEmpty()) {
            writer.printf("if (%s instanceof %s) {\n", prefix + field + suffix, cast);
            prefix = String.format("((%s) %s).", cast, prefix + field + suffix);
         }
         else {
            prefix = prefix + field + suffix + ".";
         }
      }

      return prefix;
   }

}
