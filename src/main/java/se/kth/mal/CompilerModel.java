package se.kth.mal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import se.kth.mal.steps.Connection;
import se.kth.mal.steps.SelectConnection;
import se.kth.mal.steps.Step;

public class CompilerModel {

   private List<Asset>              assets        = new ArrayList<>();
   private List<Association>        associations  = new ArrayList<>();
   private Map<Association, String> links         = new HashMap<>();
   private List<String>             includedFiles = new ArrayList<>();

   public CompilerModel(String securiLangFolder, String securiLangFile) throws FileNotFoundException, IOException {

      String fileWithIncludesPath = includeIncludes(securiLangFolder, securiLangFile);

      preprocess(fileWithIncludesPath);

      InputStream is = System.in;
      if (securiLangFile != null) {
         System.out.println("Reading from " + fileWithIncludesPath);
         is = new FileInputStream(fileWithIncludesPath);
      }
      CharStream input = CharStreams.fromStream(is);

      sLangLexer lexer = new sLangLexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      sLangParser parser = new sLangParser(tokens);
      ParseTree tree = parser.compilationUnit(); // parse

      ParseTreeWalker walker = new ParseTreeWalker(); // create standard
      // walker
      sLangListener extractor = new SecuriLangListener(this);
      walker.walk(extractor, tree); // initiate walk of tree with listener
      update();
   }

   private void preprocess(String path) {
      String file = null;
      try {
         file = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      // LET, local variables. The fact that the variables are local complicates
      // things a bit. Global variables could just be easily replaced. Local
      // variables cannot be parsed by antlr since the string representation is
      // already being dealt with (it is too late). We have to match and extract
      // attack steps before antlr goes to work and perform a simple string
      // replace.

      // TODO: Not really happy with this
      Pattern stepPattern = Pattern.compile("[^-][-+]>([^\\}|&]+)", Pattern.CASE_INSENSITIVE);
      Pattern varPattern = Pattern.compile("let\\s+([a-z0-9_]+)\\s*=\\s*([^,]+),", Pattern.CASE_INSENSITIVE);

      String master = "";

      Matcher step = stepPattern.matcher(file);
      while (step.find()) {
         master += file.substring(0, step.start(1));
         String append = file.substring(step.end(1), file.length());
         String text = step.group(1);

         Matcher var = varPattern.matcher(text);

         while (var.find()) {
            String name = var.group(1);
            String content = var.group(2);
            System.out.printf("var '%s' content '%s'\n", name, content);
            text = text.substring(0, var.start()) + text.substring(var.end(), text.length());
            // Escape both pattern and replacement. Make sure we only find
            // occurances that are not contained words.
            text = text.replaceAll("([^a-z0-9_])" + Pattern.quote(name) + "([^a-z0-9_])", "$1" + Matcher.quoteReplacement(content) + "$2");
            var = varPattern.matcher(text);
         }
         master += text;
         file = append;
         step = stepPattern.matcher(file);
      }
      try (PrintWriter out = new PrintWriter(path)) {
         out.println(master + file);
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
      }
   }

   public List<Asset> getAssets() {
      return assets;
   }

   public List<Association> getAssociations() {
      return associations;
   }

   public Map<Association, String> getLinks() {
      return links;
   }

   public String includeIncludes(String securiLangFolder, String securiLangFile) {
      String filePath = securiLangFolder + "/" + securiLangFile;

      String outPath = filePath + "inc";
      try {
         FileWriter fileWriter = new FileWriter(outPath);
         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
         appendFileToBufferedWriter(securiLangFolder, securiLangFile, bufferedWriter);
         bufferedWriter.close();
      }
      catch (FileNotFoundException ex) {
         System.out.println("Unable to open file '" + filePath + "'");
      }
      catch (IOException ex) {
         System.out.println("Error reading file '" + filePath + "'");
      }
      return outPath;
   }

   public void appendFileToBufferedWriter(String securiLangFolder, String securiLangFile, BufferedWriter bufferedWriter) throws java.io.FileNotFoundException, java.io.IOException {
      String line = null;
      String filePath = securiLangFolder + "/" + securiLangFile;
      if (includedFiles.contains(filePath)) {
         return;
      }
      includedFiles.add(filePath);

      FileReader fileReader = new FileReader(filePath);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      while ((line = bufferedReader.readLine()) != null) {
         if (line.contains("include") && !line.contains("//")) {
            if (line.split(" ")[0].equals("include")) {
               String includeFileName = line.split(" ")[1];
               appendFileToBufferedWriter(securiLangFolder, includeFileName, bufferedWriter);
            }
         }
         else {
            bufferedWriter.write(line + "\n");
         }
      }
      bufferedReader.close();
   }

   public Asset addAsset(String name, String superAssetName, boolean abstractAsset) {
      Asset a = new Asset(name, this, abstractAsset);
      if (superAssetName != "") {
         a.superAssetName = superAssetName;
      }
      assets.add(a);
      return a;
   }

   public Association addAssociation(String leftAssetName, String leftRoleName, String leftMultiplicity, String leftRelation, String name, String rightRelation, String rightMultiplicity,
         String rightRoleName, String rightAssetName) {
      Association a = new Association(leftAssetName, leftRoleName, leftMultiplicity, leftRelation, name, rightRelation, rightMultiplicity, rightRoleName, rightAssetName);
      associations.add(a);
      if (!links.containsKey(a)) {
         links.put(a, String.format("%s_%s", a.leftRoleName, a.rightRoleName));
      }
      return a;
   }

   public Asset getAsset(String name) {
      for (Asset asset : this.assets) {
         if (asset.name.equals(name)) {
            return asset;
         }
      }
      throw new Error(String.format("No asset named '%s'", name));
   }

   public List<Association> getAssociations(Asset asset) {
      List<Association> assetAssociations = new ArrayList<>();
      for (Association a : associations) {
         if (a.leftAssetName.equals(asset.name)) {
            assetAssociations.add(a);
         }
         if (a.rightAssetName.equals(asset.name)) {
            assetAssociations
                  .add(new Association(a.rightAssetName, a.rightRoleName, a.rightMultiplicity, a.rightRelation, a.name, a.leftRelation, a.leftMultiplicity, a.leftRoleName, a.leftAssetName));
         }
      }
      return assetAssociations;
   }

   public Association getAssociation(String assetName_1, String associationName, String assetName_2) {
      for (Association a : associations) {
         if (a.name.equals(associationName)
               && ((a.leftAssetName.equals(assetName_1) && a.rightAssetName.equals(assetName_2)) || (a.rightAssetName.equals(assetName_1) && a.leftAssetName.equals(assetName_2)))) {
            return a;
         }
      }
      return null;
   }

   public Association getConnectedAssociation(String leftAssetName, String rightRoleName) {
      for (Association association : this.associations) {
         if (association.leftAssetName.equals(leftAssetName) && association.rightRoleName.equals(rightRoleName)) {
            return association;
         }
         if (association.rightAssetName.equals(leftAssetName) && association.leftRoleName.equals(rightRoleName)) {
            return association;
         }
      }
      // Does the association exist for the super asset of leftAsset?
      Asset leftAsset = getAsset(leftAssetName);
      if (!leftAsset.superAssetName.isEmpty()) {
         System.out.println(String.format("No association from asset '%s' to field [%s]", leftAssetName, rightRoleName));
         System.out.println(String.format("  Checking extended parent '%s' to field [%s]", leftAsset.superAssetName, rightRoleName));
         return getConnectedAssociation(leftAsset.superAssetName, rightRoleName);
      }

      throw new Error(String.format("No association from asset '%s' to field [%s]", leftAssetName, rightRoleName));
   }

   public String getConnectedAssetName(String leftAssetName, String rightRoleName) {
      if (rightRoleName.equals("this")) {
         return leftAssetName;
      }
      Association association = getConnectedAssociation(leftAssetName, rightRoleName);
      if (association.rightRoleName.equals(rightRoleName)) {
         return association.rightAssetName;
      }
      if (association.leftRoleName.equals(rightRoleName)) {
         return association.leftAssetName;
      }
      return null;
   }

   private boolean isLeftAsset(Association assoc, String name) {
      if (assoc.leftAssetName.equals(name)) {
         return true;
      }
      else if (assoc.rightAssetName.equals(name)) {
         return false;
      }
      // Original asset name might be the last child and the association may be
      // set way up the parent tree. We must iterate upwards until we can't.
      Asset asset = getAsset(name);
      if (!asset.getSuperAssetName().equals("")) {
         System.out.println("Climbing parents, " + asset.getSuperAssetName());
         return isLeftAsset(assoc, asset.getSuperAssetName());
      }
      else {
         return false;
      }
   }

   /**
    * Updates all connections of a step with their associations.
    *
    * @param step
    *           Step to be updated.
    */
   void updateStep(Step step) {
      System.out.println("Updating step, " + step);
      for (Connection connection : step.connections) {
         if (connection instanceof SelectConnection) {
            for (Step childStep : ((SelectConnection) connection).steps) {
               updateStep(childStep);
            }
            String targetAsset = ((SelectConnection) connection).steps.get(0).getTargetAsset();
            for (int i = 1; i < ((SelectConnection) connection).steps.size(); i++) {
               String target = ((SelectConnection) connection).steps.get(i).getTargetAsset();
               if (!target.equals(targetAsset)) {
                  throw new Error(String.format("Different set type on index %d; %s =/= %s", i, targetAsset, target));
               }
            }
            ((SelectConnection) connection).update();
         }
         else {
            System.out.println("prev: " + connection.previousAsset + ", to field " + connection.field);
            Association association = getConnectedAssociation(connection.previousAsset, connection.field);
            boolean previousAssetLeft = isLeftAsset(association, connection.previousAsset);
            connection.associationUpdate(association, previousAssetLeft);
         }
         int nextIndex = step.connections.indexOf(connection) + 1;
         if (nextIndex < step.connections.size()) {
            step.connections.get(nextIndex).previousAsset = connection.getCastedAsset();
         }
      }
      System.out.println("Done updating step, " + step.from + ", " + step.to);
   }

   public void update() {
      for (Asset asset : assets) {
         asset.inheritAttackSteps();
         // Store whether assets have specializations or not.
         if (!asset.superAssetName.equals("")) {
            Asset sAsset = getAsset(asset.superAssetName);
            sAsset.hasSpecializations = true;
         }
      }

      for (Asset asset : assets) {
         for (AttackStep attackStep : asset.attackSteps) {
            System.out.println(String.format("Iterating children inside %s$%s", asset.name, attackStep.name));
            for (Step step : attackStep.steps) {
               updateStep(step);
               Step parent = step.reverse(step.getTargetAsset());
               getAsset(step.getTargetAsset()).getAttackStep(step.to).parentSteps.add(parent);
            }
         }
      }
   }

   class Distribution {
      String       distributionName;
      List<String> parameters = new ArrayList<>();

      public Distribution(String distributionName, List<String> parameters) {
         this.distributionName = distributionName;
         this.parameters = parameters;
      }
   }

   public static String capitalize(String line) {
      return Character.toUpperCase(line.charAt(0)) + line.substring(1);
   }

   public static String decapitalize(String line) {
      return Character.toLowerCase(line.charAt(0)) + line.substring(1);
   }
}
