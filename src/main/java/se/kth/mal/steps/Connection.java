package se.kth.mal.steps;

import java.io.PrintWriter;

import org.apache.commons.lang.RandomStringUtils;

import se.kth.mal.Association;

/**
 * A connection contains information how to move from one link in the chain to
 * another when traversing a reached attack step. For example in the reached
 * attack step compromise of an asset Network, a chain could be
 * 'routers.dataflows.compromise'. The chain has two links, routers and
 * dataflows, finished with an attack step compromise. 2 connections would be
 * made to traverse back and forth in the links.
 */
public class Connection {
   public String    previousAsset        = ""; // Previous asset according to
                                               // the
                                               // association
   public String    previousCast         = ""; // What the previous asset must
                                               // be
                                               // cast as to be valid. Used
                                               // whenever an asset extends some
                                               // other asset, but the
                                               // association
                                               // is made with the parent asset.
   public String    previousField        = "";
   public String    previousMultiplicity = "";
   public String    asset                = ""; // Asset according to association
   public String    cast                 = ""; // What the asset must be cast
                                               // to,
                                               // used at the users discretion
                                               // using typeof operator.
   public String    field                = "";
   public String    multiplicity         = "";

   public DebugInfo debug;

   public Connection() {
   }

   public Connection(String field) {
      this.field = field;
   }

   public Connection(String field, String cast) {
      this.field = field;
      this.cast = cast;
   }

   /**
    * Creates a new but reversed connection. When a connection is updated
    * completely it may be reversed to represent the other direction.
    *
    * @return Reversed connection
    */
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

   /**
    * Creating a connection only requires the field name. A chain of connections
    * must thereafter be updated by traversing them and finding corresponding
    * associations. The first connection must have a previous asset to begin
    * with, this method then updates the missing fields and prepares it for the
    * next connection to be updated.
    *
    * @param association
    *           Association between previous asset and current field.
    * @param previousAssetLeft
    *           True if previous asset was on left side of association.
    */
   public void associationUpdate(Association association, boolean previousAssetLeft) {
      this.previousField = (previousAssetLeft ? association.getLeftRoleName() : association.getRightRoleName());
      this.previousMultiplicity = (previousAssetLeft ? association.getLeftMultiplicity() : association.getRightMultiplicity());
      this.asset = (!previousAssetLeft ? association.getLeftAssetName() : association.getRightAssetName());
      this.multiplicity = (!previousAssetLeft ? association.getLeftMultiplicity() : association.getRightMultiplicity());
      String previous = (previousAssetLeft ? association.getLeftAssetName() : association.getRightAssetName());
      if (!previous.equals(previousAsset)) {
         previousCast = previousAsset;
         previousAsset = previous;
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

   /**
    * Prints this connection as java code.
    *
    * @param writer
    *           The writer to write to.
    * @param prefix
    *           Function may be called after another connection and will have an
    *           iterator or previous value to respect.
    * @param suffix
    *           The foreseeti backend use an additional abstraction of sets and
    *           must call a function to obtain the actual set. Therefore we
    *           support a suffix to be used.
    * @return New prefix.
    */
   public String print(PrintWriter writer, String prefix, String suffix) {
      if (isSet()) {
         String iterator = decapitalize(asset) + "_" + RandomStringUtils.randomAlphabetic(5);
         writer.printf("for (%s %s : %s) {\n", asset, iterator, prefix + field + suffix);
         if (!cast.isEmpty()) {
            writer.printf("if (%s instanceof %s) {\n", iterator, cast);
            prefix = String.format("((%s) %s).", cast, iterator);
         }
         else {
            prefix = iterator + ".";
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

   public String illustrate() {
      return field;
   }
}
