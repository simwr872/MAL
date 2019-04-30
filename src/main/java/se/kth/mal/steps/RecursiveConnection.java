package se.kth.mal.steps;

import java.io.PrintWriter;

import org.apache.commons.lang.RandomStringUtils;

public class RecursiveConnection extends Connection {

   public RecursiveConnection(String field) {
      super(field);
   }

   @Override public Connection reverse() {
      Connection connection = new RecursiveConnection("");
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

   @Override
   public String print(PrintWriter writer, String prefix, String suffix) {
      // Print occurs inside a function so we have to simulate something
      // recursive
      String set = decapitalize(asset) + "_" + RandomStringUtils.randomAlphabetic(5);
      String pool = decapitalize(asset) + "_" + RandomStringUtils.randomAlphabetic(5);
      String item = decapitalize(asset) + "_" + RandomStringUtils.randomAlphabetic(5);
      writer.printf("java.util.Set<%s> %s = new java.util.HashSet<>();\n", asset, set);
      writer.printf("java.util.List<%s> %s = new java.util.ArrayList<>();\n", asset, pool);
      // Add ourselves to the returned set and current pool
      if(!prefix.isEmpty()) {
         writer.printf("%s.add(%s);\n", set, prefix.substring(0, prefix.length() - 1));
         writer.printf("%s.add(%s);\n", pool, prefix.substring(0, prefix.length() - 1));
      } else {
         writer.printf("%s.add(%s.this);\n", set, asset);
         writer.printf("%s.add(%s.this);\n", pool, asset);
      }

      writer.printf("%s %s;\n", asset, item);
      writer.printf("while(!%s.isEmpty()) {\n", pool);
      writer.printf("%s = %s.remove(0);\n", item, pool);
      if (isSet()) {
         writer.printf("%s.addAll(%s.%s);\n", set, item, field + suffix);
         writer.printf("%s.addAll(%s.%s);\n", pool, item, field + suffix);
      }
      else {
         writer.printf("%s.add(%s.%s);\n", set, item, field + suffix);
         writer.printf("%s.add(%s.%s);\n", pool, item, field + suffix);
      }
      writer.println("}");

      // Iterate the constructed set
      String iterator = decapitalize(asset) + "_" + RandomStringUtils.randomAlphabetic(5);
      writer.printf("for (%s %s : %s) {\n", asset, iterator, set);
      prefix = iterator + ".";
      return prefix;
   }

   @Override
   public String illustrate() {
      return field + "+";
   }
}
