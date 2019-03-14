package se.kth.mal.steps;

import java.io.PrintWriter;

import org.apache.commons.lang.RandomStringUtils;

public class RecursiveConnection extends Connection {

   public RecursiveConnection(String field) {
      super(field);
   }

   @Override
   public String print(PrintWriter writer, String prefix, String suffix) {
      // Print occurs inside a function so we have to simulate something
      // recursive
      String set = decapitalize(asset) + "_" + RandomStringUtils.randomAlphabetic(5);
      String pool = decapitalize(asset) + "_" + RandomStringUtils.randomAlphabetic(5);
      String item = decapitalize(asset) + "_" + RandomStringUtils.randomAlphabetic(5);
      writer.printf("Set<%s> %s = new HashSet<>();\n", asset, set);
      writer.printf("List<%s> %s = new ArrayList<>();\n", asset, pool);
      // Add ourselves to the returned set and current pool
      writer.printf("%s.add(%s);\n", set, prefix.substring(0, prefix.length() - 1));
      writer.printf("%s.add(%s);\n", pool, prefix.substring(0, prefix.length() - 1));

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
}
