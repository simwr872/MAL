package se.kth.mal;

import java.util.Collections;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import se.kth.mal.sLangParser.CategoryDeclarationContext;
import se.kth.mal.sLangParser.ChildExtensionContext;
import se.kth.mal.sLangParser.ExpressionContext;
import se.kth.mal.sLangParser.ExpressionStepContext;
import se.kth.mal.sLangParser.NormalStepContext;
import se.kth.mal.sLangParser.SetStepContext;
import se.kth.mal.steps.Connection;
import se.kth.mal.steps.DebugInfo;
import se.kth.mal.steps.RecursiveConnection;
import se.kth.mal.steps.SelectConnection;
import se.kth.mal.steps.Step;

public class SecuriLangListener extends sLangBaseListener {
   CompilerModel model;
   String        category;
   Asset         asset;
   AttackStep    attackStep;

   public SecuriLangListener(CompilerModel model) {
      this.model = model;
   }

   @Override
   public void enterCategoryDeclaration(CategoryDeclarationContext ctx) {
      category = ctx.Identifier().getText();
   }

   @Override
   public void exitCategoryDeclaration(CategoryDeclarationContext ctx) {
      category = "";
   }

   @Override
   public void enterAssetDeclaration(sLangParser.AssetDeclarationContext ctx) {
      System.out.println("asset = " + ctx.Identifier(0).getText());
      boolean abstractAsset = ctx.getText().startsWith("abstract");
      if (ctx.Identifier().size() == 1) {
         asset = model.addAsset(ctx.Identifier(0).getText(), "", abstractAsset);
      }
      else {
         asset = model.addAsset(ctx.Identifier(0).getText(), ctx.Identifier(1).getText(), abstractAsset);
      }
      if (ctx.description() != null) {
         asset.setInfo(ctx.description().StringLiteral().getText().replaceAll("\"", ""));
      }
      if (ctx.rationale() != null) {
         asset.setRationale(ctx.rationale().StringLiteral().getText().replaceAll("\"", ""));
      }
      asset.category = category;
   }

   @Override
   public void enterAssociationDeclaration(sLangParser.AssociationDeclarationContext ctx) {
      model.addAssociation(ctx.Identifier(0).getText(), ctx.Identifier(1).getText(), ctx.multiplicity(0).getText(), ctx.leftRelation().getText(), ctx.Identifier(2).getText(),
            ctx.rightRelation().getText(), ctx.multiplicity(1).getText(), ctx.Identifier(3).getText(), ctx.Identifier(4).getText());
   }

   @Override
   public void enterChildExtension(ChildExtensionContext ctx) {
      attackStep.isExtension = true;
   }

   @Override
   public void enterAttackStepDeclaration(sLangParser.AttackStepDeclarationContext ctx) {
      attackStep = asset.addAttackStep(true, ctx.attackStepType().getText(), ctx.Identifier().getText());

      if (ctx.ttc() != null) {
         attackStep.ttcFunction = ctx.ttc().Identifier().getText();
         if (ctx.ttc().formalParameters() != null) {
            int nParams = ctx.ttc().formalParameters().DecimalFloatingPointLiteral().size();
            for (int i = 0; i < nParams; i++) {
               attackStep.ttcParameters.add(Float.parseFloat(ctx.ttc().formalParameters().DecimalFloatingPointLiteral(i).getText()));
            }
         }
      }

      if (ctx.description() != null) {
         attackStep.description = ctx.description().StringLiteral().getText().replaceAll("\"", "");
      }
      if (ctx.rationale() != null) {
         attackStep.setRationale(ctx.rationale().StringLiteral().getText().replaceAll("\"", ""));
      }
   }

   @Override
   public void enterExistenceStepDeclaration(sLangParser.ExistenceStepDeclarationContext ctx) {
      attackStep = asset.addAttackStep(true, ctx.existenceStepType().getText(), ctx.Identifier().getText());
      if (!(ctx.existenceRequirements() == null)) {
         for (TerminalNode existenceRequirement : ctx.existenceRequirements().Identifier()) {
            attackStep.existenceRequirementRoles.add(existenceRequirement.getText());
         }
      }

      if (ctx.ttc() != null) {
         attackStep.ttcFunction = ctx.ttc().Identifier().getText();
         if (ctx.ttc().formalParameters() != null) {
            int nParams = ctx.ttc().formalParameters().DecimalFloatingPointLiteral().size();
            for (int i = 0; i < nParams; i++) {
               attackStep.ttcParameters.add(Float.parseFloat(ctx.ttc().formalParameters().DecimalFloatingPointLiteral(i).getText()));
            }
         }
      }

      if (ctx.description() != null) {
         attackStep.description = ctx.description().StringLiteral().getText();
      }
   }

   int level = 0;

   public void inPrint(String... args) {
      String format = args[0];
      args[0] = String.join(" ", Collections.nCopies(level, "  "));
      System.out.printf("%s" + format + "\n", args);
   }

   public DebugInfo debug(ParserRuleContext ctx) {
      return new DebugInfo(ctx.getText(), ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine());
   }

   public Connection parseNormal(NormalStepContext ctx) {
      inPrint("Step is of type normal");
      String field = ctx.Identifier(0).getText();
      String cast = "";
      boolean transitive = false;
      if (ctx.Identifier().size() == 2) {
         cast = ctx.Identifier(1).getText();
         inPrint("Step is typed '%s'", cast);
      }
      if (ctx.transitiveSign() != null) {
         transitive = true;
         inPrint("Step is transitive");
      }
      if (transitive) {
         return new RecursiveConnection(field);
      }
      else {
         return new Connection(field, cast);
      }
   }

   public Connection parseSet(SetStepContext ctx) {
      inPrint("Step is of type set");
      String cast = "";
      if (ctx.Identifier() != null) {
         cast = ctx.Identifier().getText();
         inPrint("Step is typed '%s'", cast);
      }
      SelectConnection con = new SelectConnection();
      for (int i = 0; i < ctx.expressionSteps().size(); i++) {
         Step childStep = new Step(asset.name, attackStep.name, "");
         childStep.debug = debug(ctx.expressionSteps(i));
         for (ExpressionStepContext step : ctx.expressionSteps(i).expressionStep()) {
            childStep.connections.add(parseStep(step));
         }
         con.steps.add(childStep);
         if (i < ctx.setOperator().size()) {
            inPrint("Joined with operator '%s'", ctx.setOperator(i).getText());
            con.operators.add(ctx.setOperator(i).getText());
         }
      }
      return con;
   }

   public Connection parseStep(ExpressionStepContext step) {
      inPrint("Parsing step '%s'", step.getText());
      Connection con;
      level++;
      if (step.normalStep() != null) {
         con = parseNormal(step.normalStep());
      }
      else {
         con = parseSet(step.setStep());
      }
      level--;
      con.debug = debug(step);
      return con;
   }

   @Override
   public void enterExpression(ExpressionContext ctx) {
      String reachedStep = ctx.Identifier().getText();
      Step currentStep = new Step(asset.name, attackStep.name, reachedStep);
      inPrint("New path from '%s$%s' through '%s' reaching '%s'", asset.name, attackStep.name, ctx.expressionSteps().getText(), reachedStep);
      level++;
      for (ExpressionStepContext step : ctx.expressionSteps().expressionStep()) {
         currentStep.connections.add(parseStep(step));
      }
      level--;
      currentStep.debug = debug(ctx);
      attackStep.steps.add(currentStep);
   }
}
