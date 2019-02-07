package se.kth.mal;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.foreseeti.generator.SecuriCADCodeGenerator;

public class Master {

   public Master(String inputPath, String outputPath, String packageName, boolean foreseeti, String iconPath, String configPath) {
      File input = new File(inputPath);
      if (!input.exists() || !input.isFile()) {
         System.err.printf("Invalid input file '%s'\n", input.getAbsolutePath());
         System.exit(1);
      }

      File output = new File(outputPath);
      if (!output.exists()) {
         output.mkdirs();
      }
      if (!output.isDirectory()) {
         System.err.printf("Invalid output path '%s'\n", output.getAbsolutePath());
         System.exit(1);
      }

      if (foreseeti) {
         File icon = null;
         if (iconPath != null) {
            icon = new File(iconPath);
            if (!icon.exists() || !icon.isDirectory()) {
               System.err.printf("Invalid icon path '%s'\n", icon.getAbsolutePath());
               System.exit(1);
            }
         }
         File config = null;
         if (configPath != null) {
            config = new File(configPath);
            if (!config.exists() || !config.isFile()) {
               System.err.printf("Invalid config path '%s'\n", config.getAbsolutePath());
               System.exit(1);
            }
         }
         new SecuriCADCodeGenerator(input, output, packageName, icon, config);
      }
      else {
         new CompilerWriter(input, output, packageName);
      }
   }

   public static void main(String[] args) throws Exception {
      Options options = new Options();

      Option input = new Option("i", "input", true, "input mal file path");
      input.setRequired(true);
      options.addOption(input);

      Option output = new Option("o", "output", true, "output folder path for generated code");
      output.setRequired(true);
      options.addOption(output);

      Option tests = new Option("t", "tests", true, "output folder path for generated test code");
      tests.setRequired(false);
      options.addOption(tests);

      Option packageName = new Option("p", "package", true, "package name of generated code");
      packageName.setRequired(true);
      options.addOption(packageName);

      Option visual = new Option("v", "visual", true, "icons for visualization");
      visual.setRequired(false);
      options.addOption(visual);

      Option foreseeti = new Option("f", "foreseeti", false, "flag to use foreseeti backend");
      foreseeti.setRequired(false);
      options.addOption(foreseeti);

      Option meta = new Option("c", "config", true, "metadata config file");
      meta.setRequired(false);
      options.addOption(meta);

      CommandLineParser parser = new DefaultParser();
      HelpFormatter formatter = new HelpFormatter();
      CommandLine cmd = null;

      try {
         cmd = parser.parse(options, args);
         new Master(cmd.getOptionValue("input").trim(), cmd.getOptionValue("output").trim(), cmd.getOptionValue("package").trim(), cmd.hasOption("foreseeti"), cmd.getOptionValue("visual"),
               cmd.getOptionValue("config"));
      }
      catch (ParseException e) {
         System.err.println(e.getMessage());
         formatter.printHelp("utility-name", options);
      }
   }
}
