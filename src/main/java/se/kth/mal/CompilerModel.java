package se.kth.mal;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class CompilerModel {

   private List<Asset>              assets       = new ArrayList<>();
   private List<Association>        associations = new ArrayList<>();
   private Map<Association, String> links        = new HashMap<>();

   public CompilerModel(String securiLangFolder, String securiLangFile) throws FileNotFoundException, IOException {

      String fileWithIncludesPath = includeIncludes(securiLangFolder, securiLangFile);

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

   public AttackStepPointer addStepPointer() {
      return new AttackStepPointer();
   }

   public Asset getAsset(String name) {
      for (Asset asset : this.assets) {
         if (asset.name.equals(name)) {
            return asset;
         }
      }
      return null;
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
      if (!leftAsset.superAssetName.equals("")) {
         Association association = getConnectedAssociation(leftAsset.superAssetName, rightRoleName);
         if (association != null) {
            return association;
         }
      }

      return null;
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

   // SecuriLangListener reads strings from the .slang file into Model
   // variables, but because the model is not yet complete, they cannot always
   // be written to the proper place (for instance, children classes may not yet
   // have been created when the first reference is encountered). Therefore,
   // Model.update() makes a second pass through the model to place all
   // information in the right place.

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
    	  for(AttackStep attackStep : asset.attackSteps) {
			  System.out.println(String.format("Iterating children inside %s$%s", asset.name, attackStep.name));
    		  for(AttackStepPointer attackStepPointer : attackStep.childPointers) {
    			  System.out.println(String.format("  Found step %s", String.join(".", attackStepPointer.roleNames)));
    			  
    			  // Each accessed attackstep will be a series of attacksteppointers.
    			  
    			  // Self referencing step will have no .attackStepPointer
    			  
    			  // Last step will have .attackStep
    			  
    			  // .asset is the asset type of the target
    			  
    			  AttackStepPointer pointer = attackStepPointer;
    			  String assetName = asset.name;
    			  int roleSize = attackStepPointer.getRoleNames().size();
    			  // Iterate only just until attackstepname
    			  for (int i = 0; i < roleSize - 1; i++) {
    				  pointer.roleName = attackStepPointer.getRoleNames().get(i);
    				  pointer.association = getConnectedAssociation(assetName, pointer.roleName);
                      assertNotNull("null", pointer.association);
                      System.out.println(String.format("    Association %s found", pointer.association.getName()));
                      if (isLeftAsset(pointer.association, assetName)) {
                         System.out.println("      left");
                         assetName = pointer.association.getRightAssetName();
                         pointer.multiplicity = pointer.association.rightMultiplicity;
                      }
                      else {
                         System.out.println("      right");
                         assetName = pointer.association.getLeftAssetName();
                         pointer.multiplicity = pointer.association.leftMultiplicity;
                      }
    				  pointer.asset = getAsset(assetName);
                      System.out.println(String.format("    Multiplicity: %s", pointer.multiplicity));
                      
                      AttackStepPointer ptr = addStepPointer();
                      pointer.attackStepPointer = ptr;
                      pointer = ptr;
    			  }
    			  String attackStepName = attackStepPointer.getRoleNames().get(roleSize - 1);
    			  // Asset is describing the connecting pointers asset, but this is here
    			  // for d3.js to render properly
    			  pointer.asset = getAsset(assetName);
    			  pointer.attackStep = pointer.asset.getAttackStep(attackStepName);

    			  AttackStepPointer parent = addStepPointer();
    			  parent.attackStep = attackStep;
    			  parent.asset = asset;
    			  if(attackStepPointer.association != null) {
    				  parent.association = attackStepPointer.association;
	    			  if(isLeftAsset(attackStepPointer.association, attackStepPointer.asset.name)) {
	    				  parent.roleName = attackStepPointer.association.getRightRoleName();
	    				  parent.multiplicity = attackStepPointer.association.rightMultiplicity;
	    			  } else {
	    				  parent.roleName = attackStepPointer.association.getLeftRoleName();
	    				  parent.multiplicity = attackStepPointer.association.leftMultiplicity;
	    			  }
    			  } else {
    				  parent.multiplicity = "1";
    			  }
    			  pointer.attackStep.parentPointers.add(parent);
    		  }
    	  }
      }
      /*
      for (Asset parentAsset : assets) {
         for (AttackStep parentAttackStep : parentAsset.attackSteps) {
            for (AttackStepPointer childPointer : parentAttackStep.childPointers) {

               System.out.println(String.format("Traversing %s$%s", parentAsset.name, childPointer.attackStepName));
               AttackStepPointer pointer = childPointer;
               String assetName = parentAsset.name;
               if (childPointer.getRoleNames().isEmpty()) {
                  childPointer.roleName = "this";
                  pointer.asset = getAsset(assetName);
                  pointer.multiplicity = "1";
               }
               for (int i = 0; i < childPointer.getRoleNames().size(); i++) {
                  pointer.roleName = childPointer.getRoleNames().get(i);
                  System.out.println(String.format("  Connecting %s -> %s", assetName, pointer.roleName));
                  pointer.association = getConnectedAssociation(assetName, pointer.roleName);
                  assertNotNull("null", pointer.association);
                  System.out.println(String.format("    Association %s found", pointer.association.getName()));
                  if (isLeftAsset(pointer.association, assetName)) {
                     System.out.println("      left");
                     assetName = pointer.association.getRightAssetName();
                     pointer.multiplicity = pointer.association.rightMultiplicity;
                  }
                  else {
                     System.out.println("      right");
                     assetName = pointer.association.getLeftAssetName();
                     pointer.multiplicity = pointer.association.leftMultiplicity;
                  }
                  pointer.asset = getAsset(assetName);
                  System.out.println(String.format("    Multiplicity: %s", pointer.multiplicity));
                  if (i != childPointer.getRoleNames().size() - 1) {
                     AttackStepPointer ptr = addStepPointer();
                     pointer.attackStepPointer = ptr;
                     pointer = ptr;
                  }
               }
               pointer.attackStepName = childPointer.attackStepName;
               pointer.attackStep = pointer.getAsset().getAttackStep(pointer.attackStepName);
               pointer.attackStep.parentPointers.add(childPointer);
               //if (pointer != childPointer) {
               //   childPointer.attackStepName = "";
               //}
            }
         }

         System.out.println("looking into mandatory and nonmandatory for " + parentAsset.name);
         Asset childAsset;
         for (Association association : getAssociations(parentAsset)) {
            childAsset = getAsset(association.getTargetAssetName(parentAsset));
            assertNotNull(String.format("The association %s [%s]<--%s--> [%s] %s from %s can't find its counterpart.", association.leftAssetName, association.leftRoleName, association.name,
                  association.rightRoleName, association.rightAssetName, parentAsset.name), childAsset);
            if (parentAsset.name.equals("MyNetwork")) {
               System.out.println("Child Asset = " + childAsset.name);
            }

            if (!parentAsset.nonMandatoryChildren.contains(childAsset)) {
               if (parentAsset.name.equals("MyNetwork")) {
                  System.out.println("target relation = " + association.getTargetRelation(parentAsset));
               }
               if (association.getTargetRelation(parentAsset).equals("-)")) {
                  System.out.println("found nonmandatory");
                  parentAsset.nonMandatoryChildren.add(childAsset);
               }
               if (association.getTargetRelation(parentAsset).equals("->")) {
                  parentAsset.mandatoryChildren.add(childAsset);
               }
            }
         }
      }*/

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
