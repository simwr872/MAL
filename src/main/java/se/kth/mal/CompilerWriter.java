package se.kth.mal;

import static org.junit.Assert.assertTrue;

//Iff any attack step (internal or external) points to an inherited attack step in an asset:
//Create a specialization of that attack step, in the specialized asset.
//Override the setExpectedParents() method, including the pointing attack step as parent in the specialization.
//(In the generalized attack step, the pointing attack step should not be included).
//Don't instantiate the attack step in the generalized class but instead in the constructor.

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import se.kth.mal.steps.Step;

// After changing sLang.g4, in bash, in
// /Users/pontus/Documents/Pontus\ Program\ Files/Eclipse/securiLangDSL2d3js/src, run
// "antlr4 sLang.g4"

// The JavaWriter produces executable Java code for testing purposes.
public class CompilerWriter {

   PrintWriter writer;
   public CompilerModel model;

   private String package2path(String packageName) throws IllegalArgumentException {
      if (!packageName.matches("\\w+(\\.\\w+)*$")) {
         throw (new IllegalArgumentException("'" + packageName + "' is not a valid package name"));
      }

      return packageName.replace('.', File.separatorChar);
   }

   public CompilerWriter(File input, File output, String packageName) {
      this.model = new CompilerModel(input);
      String name = input.getName().replaceFirst("\\.[^.]+$", ""); // strip .mal
      writeD3(output, name);
      writeJava(output, packageName, package2path(packageName));
      writeTtc(output);
   }

   private void writeTtc(File output) {
      File file = new File(output, "attackerProfile.ttc");
      try {
         PrintWriter out = new PrintWriter(file, "UTF-8");
         for(Asset asset : this.model.getAssets()) {
            for(AttackStep attackStep : asset.attackSteps) {
               out.printf("%s.%s = %s\n", asset.name, attackStep.name, attackStep.getDistribution());
            }
         }
         out.close();
      }
      catch (FileNotFoundException | UnsupportedEncodingException e) {
         e.printStackTrace();
      }
   }

   private String capitalize(final String line) {
      return Character.toUpperCase(line.charAt(0)) + line.substring(1);
   }

   private String decapitalize(final String line) {
      return Character.toLowerCase(line.charAt(0)) + line.substring(1);
   }

   public String d3() {
      JsonObjectBuilder json = Json.createObjectBuilder();
      JsonArrayBuilder assets = Json.createArrayBuilder();
      for (Asset asset : model.getAssets()) {
         JsonObjectBuilder jsonAsset = Json.createObjectBuilder();
         jsonAsset.add("name", asset.name);
         if (!asset.attackSteps.isEmpty()) {
            JsonArrayBuilder attackSteps = Json.createArrayBuilder();

            for (AttackStep attackStep : asset.attackSteps) {
               JsonObjectBuilder jsonAttackStep = Json.createObjectBuilder();
               jsonAttackStep.add("name", attackStep.name);
               if (attackStep.attackStepType.equals("#") || attackStep.attackStepType.equals("3") || attackStep.attackStepType.equals("E")) {
                  jsonAttackStep.add("type", "defense");
               }
               else if (attackStep.attackStepType.equals("&")) {
                  jsonAttackStep.add("type", "and");
               }
               else {
                  jsonAttackStep.add("type", "or");
               }

               AttackStep superAttackStep = attackStep.getSuper();
               if (!attackStep.steps.isEmpty() || superAttackStep != null) {
                  JsonArrayBuilder targets = Json.createArrayBuilder();
                  for (Step step : attackStep.steps) {
                     JsonObjectBuilder jsonStep = Json.createObjectBuilder();
                     jsonStep.add("name", step.to);
                     jsonStep.add("entity_name", step.getTargetAsset());
                     jsonStep.add("size", 4000);
                     targets.add(jsonStep);
                  }
                  if (superAttackStep != null) {
                     JsonObjectBuilder jsonStep = Json.createObjectBuilder();
                     jsonStep.add("name", superAttackStep.name);
                     jsonStep.add("entity_name", superAttackStep.asset.name);
                     jsonStep.add("size", 4000);
                     targets.add(jsonStep);
                  }
                  jsonAttackStep.add("targets", targets);
               }
               attackSteps.add(jsonAttackStep);
            }
            jsonAsset.add("children", attackSteps);
         }
         assets.add(jsonAsset);
      }
      json.add("children", assets);

      return json.build().toString();
   }

   public void writeD3(File output, String name) {

      String outputFile = output.getAbsolutePath() + File.separator + name + ".html";

      InputStream is = this.getClass().getClassLoader().getResourceAsStream("visualization.html");
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
      content = content.replace("{{NAME}}", name).replace("{{JSON}}", d3());

      try {
         PrintWriter out = new PrintWriter(outputFile, "UTF-8");
         out.print(content);
         out.close();
      }
      catch (FileNotFoundException | UnsupportedEncodingException e) {
         e.printStackTrace();
      }
   }

   private void writeCore(File output) {
      output.mkdirs();

      ZipInputStream zin = new ZipInputStream(this.getClass().getResourceAsStream("/core.zip"));
      ZipEntry entry;
      try {
         while ((entry = zin.getNextEntry()) != null) {
            byte[] buf = new byte[1024];
            FileOutputStream fos = new FileOutputStream(new File(output, entry.getName()));
            int i = -1;
            while ((i = zin.read(buf)) != -1) {
               fos.write(buf, 0, i);
            }
            fos.close();
         }
      }
      catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   private void writeJava(File output, String packageName, String packagePath) {
      writeCore(new File(output, "core"));

      File out = new File(output, packagePath);
      out.mkdirs();

      for (Asset asset : model.getAssets()) {
         File file = new File(out, asset.name + ".java");
         try {
            writer = new PrintWriter(file.getAbsolutePath(), "UTF-8");
            printPackage(packageName);
            printImports();
            printAssetClassHeaders(asset);
            printAssociations(asset);
            printStepAssignments(asset);
            printConstructor(asset);
            printStepDefinitions(asset);
            printConnectionHelpers(asset);
            printGetAssociatedAssetClassName(asset);
            printGetAssociatedAssets(asset);
            printGetAllAssociatedAssets(asset);
            writer.println("}");
            writer.close();

         }
         catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException");
            e.printStackTrace();
         }
         catch (UnsupportedEncodingException e) {
            System.out.println("UnsupportedEncodingException");
            e.printStackTrace();
         }
      }
   }

   void printPackage(String packageName) {
      writer.println("package " + packageName + ";\n");
   }

   void printImports() {
      String imports = "import java.util.ArrayList;\nimport java.util.HashSet;\nimport java.util.List;\nimport java.util.Set;\nimport static org.junit.Assert.assertTrue;\n\nimport core.Asset;\nimport core.AttackStep;\nimport core.AttackStepMax;\nimport core.AttackStepMin;\nimport core.Defense;";
      writer.println(imports);
   }

   void printAssetClassHeaders(Asset asset) {
      writer.print("public class " + asset.name);
      if (asset.superAssetName != "") {
         writer.print(" extends " + asset.superAssetName);
      }
      else {
         writer.print(" extends Asset");
      }
      writer.println(" {");
   }

   void printAssociations(Asset asset) {
      for (Association association : model.getAssociations(asset)) {
         if (association.rightMultiplicity.equals("*") || association.rightMultiplicity.equals("1-*")) {
            writer.println("   public java.util.Set<" + association.rightAssetName + "> " + association.rightRoleName + " = new HashSet<>();");
         }
         else {
            if (association.rightMultiplicity.equals("1") || association.rightMultiplicity.equals("0-1")) {
               writer.println("   public " + association.rightAssetName + " " + association.rightRoleName + ";");
            }
         }
      }
      writer.println("");
   }

   void printStepAssignments(Asset asset) {
      for (AttackStep attackStep : asset.attackSteps) {
         if (attackStep.superAttackStepName.equals("")) {
            writer.print("   public " + capitalize(attackStep.name) + " " + attackStep.name + ";\n");
         }
      }
      writer.println("");
   }

   void printConstructor(Asset asset) {
      String constructorString = "";
      constructorString = sprintConstructorWithDefenseAttributes(asset, false, constructorString);
      constructorString = sprintConstructorWithDefenseAttributes(asset, true, constructorString);
      constructorString = sprintConstructorWithoutDefenseAttributes(asset, false, constructorString);
      constructorString = sprintConstructorWithoutDefenseAttributes(asset, true, constructorString);
      writer.println(constructorString);
   }

   protected String sprintConstructorWithoutDefenseAttributes(Asset asset, boolean hasName, String constructorString) {
      if (!asset.defensesExcludingExistenceRequirements().isEmpty()) {
         constructorString += "   public " + capitalize(asset.name) + "(";
         if (hasName) {
            constructorString += "String name";
         }
         constructorString += ") {\n";
         constructorString += "      this(";
         if (hasName) {
            constructorString += "name, ";
         }

         for (AttackStep defense : asset.defensesExcludingExistenceRequirements()) {
            constructorString += "false, ";
         }
         constructorString = constructorString.substring(0, constructorString.length() - 2);
         constructorString += ");\n";
         constructorString += "      assetClassName = \"" + asset.name + "\";\n   }\n\n";
      }
      return constructorString;
   }

   protected String sprintConstructorWithDefenseAttributes(Asset asset, boolean hasName, String constructorString) {
      constructorString += "   public " + capitalize(asset.name) + "(";
      if (hasName) {
         constructorString += "String name";
         if (!asset.defensesExcludingExistenceRequirements().isEmpty()) {
            constructorString += ", ";
         }
      }
      constructorString = sprintDefenseAttributes(asset, constructorString);
      constructorString += ") {\n";
      constructorString = sprintSuperCall(asset, hasName, constructorString);
      constructorString = sprintStepCreation(asset, constructorString);
      constructorString += "      assetClassName = \"" + asset.name + "\";\n   }\n\n";
      return constructorString;
   }

   protected String sprintSuperCall(Asset asset, boolean hasName, String constructorString) {
      constructorString += "      super(";
      Asset superAsset = null;
      if (!asset.superAssetName.equals("")) {
         superAsset = model.getAsset(asset.superAssetName);
      }
      if (hasName) {
         constructorString += "name";
         if (!asset.superAssetName.equals("")) {
            if (!superAsset.defensesExcludingExistenceRequirements().isEmpty()) {
               constructorString += ", ";
            }
         }
      }
      if (!asset.superAssetName.equals("")) {
         for (AttackStep defense : superAsset.defensesExcludingExistenceRequirements()) {
            constructorString += defense.name + "State, ";
         }
         if (!superAsset.defensesExcludingExistenceRequirements().isEmpty()) {
            constructorString = constructorString.substring(0, constructorString.length() - 2);
         }
      }

      constructorString += ");\n";
      return constructorString;
   }

   protected String sprintDefenseAttributes(Asset asset, String constructorString) {
      String attributesString = "";
      if (!asset.defensesExcludingExistenceRequirements().isEmpty()) {
         for (AttackStep defense : asset.defensesExcludingExistenceRequirements()) {
            attributesString += "Boolean " + defense.name + "State, ";
         }
         if (attributesString.length() > 0) {
            attributesString = attributesString.substring(0, attributesString.length() - 2);
         }
      }
      return constructorString + attributesString;
   }

   protected String sprintStepCreation(Asset asset, String constructorString) {
      for (AttackStep defense : asset.defenses()) {
         constructorString += "      if (" + defense.name + " != null) {\n";
         constructorString += "         AttackStep.allAttackSteps.remove(" + defense.name + ".disable);\n";
         constructorString += "      }\n";
         constructorString += "      Defense.allDefenses.remove(" + defense.name + ");\n";
         constructorString += "      " + defense.name + " = new " + capitalize(defense.name) + "(this.name";
         if (!defense.hasExistenceRequirements()) {
            constructorString += ", " + defense.name + "State";
         }
         constructorString += ");\n";
      }
      for (AttackStep attackStep : asset.attackSteps) {
         if (!asset.defenses().contains(attackStep)) {
            constructorString += "      AttackStep.allAttackSteps.remove(" + attackStep.name + ");\n";
            constructorString += "      " + attackStep.name + " = new " + capitalize(attackStep.name) + "(this.name);\n";
         }
      }
      return constructorString;
   }

   void printStepDefinitions(Asset asset) {
      for (AttackStep attackStep : asset.attackSteps) {
         if (attackStep.attackStepType.equals("#") || attackStep.attackStepType.equals("E") || attackStep.attackStepType.equals("3")) {
            printDefenseDefinition(attackStep);
         }
         else {
            printAttackStepDefinition(attackStep);
         }
      }
   }

   void printDefenseDefinition(AttackStep attackStep) {
      printDefenseSignature(attackStep);
      printDefenseConstructor(attackStep);
      printExistenceRequirements(attackStep);
      // A new Disable is created for each specialization, and then only the
      // most specialized is used. Not so pretty.
      printDisableDeclaration(attackStep);
      writer.println("}\n");
   }

   protected void printDisableDeclaration(AttackStep attackStep) {
      writer.print("   public class Disable extends ");
      if (!attackStep.superAttackStepName.equals("")) {
         writer.print(attackStep.superAttackStepName + ".Disable");
      }
      else {
         writer.print("AttackStepMin");
      }
      writer.println(" {");
      // writer.println(" String defenseName = \"" + attackStep.name + "\";");
      writer.println("         public Disable(String name) {");
      writer.println("            super(name);");
      writer.println("         }\n");
      writer.println("         @Override");
      writer.println("         public String fullName() {");
      writer.println("            return \"" + attackStep.fullDefaultName() + "\";");
      writer.println("         }");
      printUpdateChildren(attackStep);
      writer.println("   }");
   }

   protected void printDefenseConstructor(AttackStep attackStep) {
      writer.print("   public " + capitalize(attackStep.name) + "(String name");
      if (!attackStep.hasExistenceRequirements()) {
         writer.print(", Boolean enabled");
      }
      writer.println(") {");
      writer.print("      super(name");
      if (!attackStep.superAttackStepName.equals("") && !attackStep.hasExistenceRequirements()) {
         writer.print(", enabled");
      }
      writer.println(");");
      if (attackStep.superAttackStepName.equals("")) {

         if (!attackStep.hasExistenceRequirements()) {
            writer.println("      defaultValue = enabled;");
         }
      }
      writer.println("      disable = new Disable(name);");
      writer.println("   }\n");
   }

   protected void printDefenseSignature(AttackStep attackStep) {
      writer.print("   public class " + capitalize(attackStep.name) + " extends ");
      if (!attackStep.superAttackStepName.equals("")) {
         writer.println(attackStep.superAttackStepName + " {");
      }
      else {
         writer.println("Defense {");
      }
   }

   void printExistenceRequirements(AttackStep attackStep) {
      if (!attackStep.existenceRequirementRoles.isEmpty()) {
         writer.println("   @Override");
         writer.println("   public boolean isEnabled() {");
         // The below should be the role name, not the asset name.
         // Furthermore, it should check for empty set rather than == null for
         // multiplicity associations
         Association association = model.getConnectedAssociation(attackStep.asset.name, attackStep.existenceRequirementRoles.get(0));
         assertTrue("Did not find the association from the asset " + attackStep.asset.name + " to the role " + attackStep.existenceRequirementRoles.get(0), association != null);
         String multiplicity = association.targetMultiplicityIncludingInheritance(attackStep.asset);
         if (multiplicity.equals("1") || multiplicity.equals("0-1")) {
            if (attackStep.attackStepType.equals("E")) {
               writer.println("      return " + attackStep.existenceRequirementRoles.get(0) + " == null;");
            }
            if (attackStep.attackStepType.equals("3")) {
               writer.println("      return " + attackStep.existenceRequirementRoles.get(0) + " != null;");
            }
         }
         else {
            if (attackStep.attackStepType.equals("E")) {
               writer.println("      return " + attackStep.existenceRequirementRoles.get(0) + ".isEmpty();");
            }
            if (attackStep.attackStepType.equals("3")) {
               writer.println("      return !" + attackStep.existenceRequirementRoles.get(0) + ".isEmpty();");
            }
         }
         writer.println("   }");
      }

   }

   void printAttackStepDefinition(AttackStep attackStep) {
      writer.print("   public class " + capitalize(attackStep.name) + " extends ");
      String attackStepTypeString = "";
      if (!attackStep.superAttackStepName.equals("")) {
         attackStepTypeString = attackStep.superAttackStepName;
      }
      else {
         if (attackStep.attackStepType.equals("&")) {
            attackStepTypeString = "AttackStepMax";
         }
         if (attackStep.attackStepType.equals("|")) {
            attackStepTypeString = "AttackStepMin";
         }
         if (attackStep.attackStepType.equals("t")) {
            attackStepTypeString = "CPT_AttackStep";
         }
      }
      assert (!attackStepTypeString.equals(""));
      writer.println(attackStepTypeString + " {");
      writer.println("   public " + capitalize(attackStep.name) + "(String name) {");
      writer.println("      super(name);");
      writer.println("      assetClassName = \"" + attackStep.asset.name + "\";");
      writer.println("   }");

      printSetExpectedParents(attackStep);
      printUpdateChildren(attackStep);
      printLocalTtc(attackStep);

      writer.println("   }\n");
   }

   void printUpdateChildren(AttackStep attackStep) {
      if (!attackStep.steps.isEmpty()) {
         writer.println("@Override");
         writer.println("public void updateChildren(java.util.Set<AttackStep> activeAttackSteps) {");
         if (attackStep.isExtension) {
            writer.println("super.updateChildren(activeAttackSteps);");
         }
         for (Step step : attackStep.steps) {
            step.print(writer, "%s.updateTtc(this, ttc, activeAttackSteps);\n");
         }
         writer.println("}");
      }
   }

   void printSetExpectedParents(AttackStep attackStep) {
      if (!attackStep.parentSteps.isEmpty()) {
         writer.println("@Override");
         writer.println("public void setExpectedParents() {");
         if (!attackStep.getSuperAttackStepName().isEmpty()) {
            writer.println("super.setExpectedParents();");
         }
         if (!attackStep.getExistenceRequirementRoles().isEmpty()) {
            writer.println(String.format("if (%s != null) {", attackStep.getExistenceRequirementRoles().get(0)));
         }
         for (Step step : attackStep.parentSteps) {
            if (model.getAsset(step.getTargetAsset()).getAttackStep(step.to).isDefense()) {
               step.print(writer, "addExpectedParent(%s.disable);\n");
            }
            else {
               step.print(writer, "addExpectedParent(%s);\n");
            }
         }
         if (!attackStep.getExistenceRequirementRoles().isEmpty()) {
            writer.println("}");
         }
         writer.println("}");
      }
   }

   void printLocalTtc(AttackStep attackStep) {
      if (attackStep.asset.superAssetName.equals("") || !attackStep.ttcFunction.equals("Default")) {
         writer.println("      @Override");
         writer.println("      public double localTtc() {");
         writer.println("         return ttcHashMap.get(\"" + attackStep.asset.name + "." + attackStep.name + "\");");
         // if (attackStep.ttcFunction.equals("ExponentialDistribution")) {
         // writer.println(" return " +
         // attackStep.ttcParameters.get(0).toString() + ";");
         // }
         // else {
         // if (attackStep.ttcFunction.equals("GammaDistribution")) {
         // writer.println(" return " +
         // Float.toString((attackStep.ttcParameters.get(0) *
         // attackStep.ttcParameters.get(1))) + ";");
         // }
         // else {
         // if (attackStep.ttcFunction.equals("BernoulliDistribution")) {
         // writer.println(" if (" + attackStep.ttcParameters.get(0).toString()
         // + " > 0.5 ) {");
         // writer.println(" return oneSecond;\n}");
         // writer.println(" else {");
         // writer.println(" return infinity;\n}");
         // }
         // else {
         // assert false : "Unknown distribution for attack step " +
         // attackStep.asset.name + "." + attackStep.name + ".";
         // }
         // }
         // }
         writer.println("      }\n");
      }
   }

   void printConnectionHelpers(Asset asset) {
      for (Association association : asset.getAssociations()) {
         String targetAssetName = association.getTargetAssetName(asset);
         String targetRoleName = association.getTargetRoleName(asset);
         String sourceRoleName = association.getSourceRoleName(asset);
         writer.println("      public void add" + capitalize(targetRoleName) + "(" + targetAssetName + " " + targetRoleName + ") {");
         if (association.targetMultiplicity(asset).equals("0-1") || association.targetMultiplicity(asset).equals("1")) {
            writer.println("         this." + targetRoleName + " = " + targetRoleName + ";");
         }
         else {
            writer.println("         this." + targetRoleName + ".add(" + targetRoleName + ");");
         }
         if (association.sourceMultiplicity(asset).equals("0-1") || association.sourceMultiplicity(asset).equals("1")) {
            writer.println("         " + targetRoleName + "." + sourceRoleName + " = this;");
         }
         else {
            writer.println("         " + targetRoleName + "." + sourceRoleName + ".add(this);");
         }
         writer.println("      }\n");
      }
   }

   void printGetAssociatedAssetClassName(Asset asset) {
      writer.println("   @Override");
      writer.println("   public String getAssociatedAssetClassName(String roleName) {");
      for (Association association : asset.getAssociations()) {
         writer.println("      if (roleName.equals(\"" + association.getTargetRoleName(asset) + "\")) {");
         if (association.targetMultiplicity(asset).equals("*") || association.targetMultiplicity(asset).equals("1-*")) {
            writer.println("         for (Object o: " + association.getTargetRoleName(asset) + ") {");
            writer.println("            return o.getClass().getName();");
            writer.println("         }");
         }
         else {
            writer.println("         return " + association.getTargetRoleName(asset) + ".getClass().getName();");
         }
         writer.println("      }");
      }
      writer.println("      return null;");
      writer.println("   }");
   }

   void printGetAssociatedAssets(Asset asset) {
      writer.println("   @Override");
      writer.println("   public java.util.Set<Asset> getAssociatedAssets(String roleName) {");
      writer.println("      java.util.Set<Asset> assets = new java.util.HashSet<>();");
      for (Association association : asset.getAssociationsIncludingInherited()) {
         writer.println(
               "      if (roleName.equals(\"" + association.getTargetRoleNameIncludingInheritance(asset) + "\")  && " + association.getTargetRoleNameIncludingInheritance(asset) + " != null) {");
         if (association.targetMultiplicityIncludingInheritance(asset).equals("*") || association.targetMultiplicityIncludingInheritance(asset).equals("1-*")) {
            writer.println("         assets.addAll(" + association.getTargetRoleNameIncludingInheritance(asset) + ");");
         }
         else {
            writer.println("         assets.add(" + association.getTargetRoleNameIncludingInheritance(asset) + ");");
         }
         writer.println("         return assets;");
         writer.println("      }");
      }
      writer.println("      assertTrue(\"The asset \" + this.toString() + \" does not feature the role name \" + roleName + \".\", false);");
      writer.println("      return null;");
      writer.println("   }");
   }

   void printGetAllAssociatedAssets(Asset asset) {
      writer.println("   @Override");
      writer.println("   public java.util.Set<Asset> getAllAssociatedAssets() {");
      writer.println("      java.util.Set<Asset> assets = new java.util.HashSet<>();");
      for (Association association : asset.getAssociationsIncludingInherited()) {
         if (association.targetMultiplicityIncludingInheritance(asset).equals("*") || association.targetMultiplicityIncludingInheritance(asset).equals("1-*")) {
            writer.println("      assets.addAll(" + association.getTargetRoleNameIncludingInheritance(asset) + ");");
         }
         else {
            writer.println("      if (" + association.getTargetRoleNameIncludingInheritance(asset) + " != null) {");
            writer.println("         assets.add(" + association.getTargetRoleNameIncludingInheritance(asset) + ");");
            writer.println("      }");
         }
      }
      writer.println("      return assets;");
      writer.println("   }");
   }

}
