package se.kth.mal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.xml.transform.ErrorListener;

import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import org.antlr.v4.runtime.tree.TerminalNode;
import se.kth.mal.antlr4.MalErrorListener;
import se.kth.mal.antlr4.MalListener;
import se.kth.mal.steps.Connection;
import se.kth.mal.steps.SelectConnection;
import se.kth.mal.steps.Step;

public class CompilerModel {

   private File                     inputFile;
   private List<Asset>              assets       = new ArrayList<>();
   private List<Association>        associations = new ArrayList<>();
   private Map<Association, String> links        = new HashMap<>();
   private int                      errors;

   private List<File> parsedFiles = new ArrayList<>();

   public CompilerModel(File inputFile) {
      this.errors = 0;
      this.inputFile = inputFile;
      parseFile(inputFile);
      if (this.errors != 0) {
         System.err.println("abort: there were parsing errors");
         System.exit(1);
      }
      update();
   }

   public void parseFile(String path) {
      parseFile(new File(this.inputFile.getParent(), path));
   }

   public void parseFile(File file) {
      if (!parsedFiles.contains(file)) {
         parsedFiles.add(file);

         CharStream input = null;
         try {
            input = CharStreams.fromPath(file.toPath());
         }
         catch (IOException e) {
            e.printStackTrace();
         }
         MalErrorListener errorListener = new MalErrorListener(file);

         MalLexer lexer = new MalLexer(input);
         lexer.removeErrorListeners();
         lexer.addErrorListener(errorListener);

         CommonTokenStream tokens = new CommonTokenStream(lexer);

         MalParser parser = new MalParser(tokens);
         parser.removeErrorListeners();
         parser.addErrorListener(errorListener);

         ParseTree tree = parser.compilationUnit();

         MalListener listener = new MalListener(this);
         ParseTreeWalker walker = new ParseTreeWalker();
         walker.walk(listener, tree);
         this.errors += errorListener.getErrors();
      }
   }

   public void parse(ParseTree leaf, JsonArrayBuilder array, Parser parser) {
      if (leaf instanceof TerminalNode) {
         JsonObjectBuilder object = Json.createObjectBuilder();
         object.add("name", leaf.getText());
         array.add(object);
      }
      else {
         String name = parser.getRuleNames()[((ParserRuleContext) leaf).getRuleIndex()];
         JsonObjectBuilder object = Json.createObjectBuilder();
         object.add("name", name);
         JsonArrayBuilder arr = Json.createArrayBuilder();
         for (int i = 0; i < leaf.getChildCount(); i++) {
            parse(leaf.getChild(i), arr, parser);
         }
         object.add("children", arr);
         array.add(object);
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
         if (a.name.equals(associationName) && ((a.leftAssetName.equals(assetName_1) && a.rightAssetName.equals(assetName_2)) || (a.rightAssetName.equals(assetName_1) && a.leftAssetName
               .equals(assetName_2)))) {
            return a;
         }
      }
      return null;
   }

   public Association getConnectedAssociation(String leftAssetName, String rightRoleName) {
      return getConnectedAssociation(leftAssetName, rightRoleName, null);
   }

   public Association getConnectedAssociation(String leftAssetName, String rightRoleName, Connection connection) {
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
         return getConnectedAssociation(leftAsset.superAssetName, rightRoleName, connection);
      }
      if (connection != null) {
         connection.debug.print();
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
         return isLeftAsset(assoc, asset.getSuperAssetName());
      }
      else {
         return false;
      }
   }

   /**
    * Traverses the ancestor hierarchy and returns which asset is the 'oldest'.
    *
    * @param asset1
    * @param asset2
    * @return
    */
   private String oldestAsset(String asset1, String asset2) {
      Asset a1 = getAsset(asset1);
      Asset a2 = getAsset(asset2);

      String asset = asset1;
      while (!asset.isEmpty()) {
         Asset a = getAsset(asset);
         if (a.getName().equals(a2.getName())) {
            return a.getName();
         }
         asset = a.getSuperAssetName();
      }

      asset = asset2;
      while (!asset.isEmpty()) {
         Asset a = getAsset(asset);
         if (a.getName().equals(a1.getName())) {
            return a.getName();
         }
         asset = a.getSuperAssetName();
      }

      return "";
   }

   /**
    * Updates all connections of a step with their associations.
    *
    * @param step Step to be updated.
    */
   void updateStep(Step step) {
      // A step is a collection of connections from one assets compromised
      // attack step to another. At this stage a step only has whatever
      // attackStep it came from and what asset it came from. Note that this
      // asset will be incorrect for set operations that are chained. This is
      // fine however since we will travel down it and update on the fly.

      for (int i = 0; i < step.connections.size(); i++) {
         // To update a connection we must first verify that it is possible,
         // then check what asset it is and update the next steps previous
         // asset.
         Connection connection = step.connections.get(i);
         if (i == 0) {
            // First step would not have a previous asset so we set this to the
            // steps asset. Usually this is just the parent step but might be an
            // updated previous value from traversing a chain with a set
            // operation.
            connection.previousAsset = step.asset;
         }
         if (!(connection instanceof SelectConnection)) {
            // Normal step
            Association association = getConnectedAssociation(connection.previousAsset, connection.field, connection);
            boolean previousAssetLeft = isLeftAsset(association, connection.previousAsset);
            connection.associationUpdate(association, previousAssetLeft);
         }
         else {
            // We are in a set operation, we can just start updating its child
            // steps while updating their assets to be the connections previous
            // asset since this would've changed during parsing.
            for (int j = 0; j < ((SelectConnection) connection).steps.size(); j++) {
               Step child = ((SelectConnection) connection).steps.get(j);
               child.asset = connection.previousAsset;
               updateStep(child);
            }
            String oldest = ((SelectConnection) connection).steps.get(0).getTargetAsset();
            for (int j = 1; j < ((SelectConnection) connection).steps.size(); j++) {
               Step child = ((SelectConnection) connection).steps.get(j);
               String _oldest = oldestAsset(oldest, child.getTargetAsset());
               if (_oldest.isEmpty()) {
                  ((SelectConnection) connection).debug.print();
                  child.debug.print();
                  throw new Error(String.format("Could not find common ancestor between '%s' and '%s'\n", oldest, child.getTargetAsset()));
               }
               oldest = _oldest;
            }
            ((SelectConnection) connection).update(oldest);
         }
         if (i + 1 < step.connections.size()) {
            // Update the next steps previous asset
            step.connections.get(i + 1).previousAsset = connection.getCastedAsset();
         }
      }
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
