package se.kth.mal;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

import se.kth.mal.sLangParser.CategoryDeclarationContext;
import se.kth.mal.sLangParser.ChildExtensionContext;
import se.kth.mal.sLangParser.ExpressionStepContext;
import se.kth.mal.sLangParser.ImmediateContext;
import se.kth.mal.sLangParser.NormalContext;
import se.kth.mal.sLangParser.PreExpressionStepContext;
import se.kth.mal.sLangParser.SelectContext;
import se.kth.mal.sLangParser.SetChildContext;
import se.kth.mal.sLangParser.SetOperationContext;
import se.kth.mal.sLangParser.SetOperatorContext;
import se.kth.mal.steps.Connection;
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

   @Override
   public void enterImmediate(ImmediateContext ctx) {
      // Immediate step of form '-> compromise'
      Step step = new Step(asset.name, attackStep.name, ctx.Identifier().getText());
      attackStep.steps.add(step);
   }

   public void parseExpressionStep(List<TerminalNode> identifiers, Step step) {
      String cast = (identifiers.size() > 1 ? identifiers.get(1).getText() : "");
      Connection connection = new Connection(identifiers.get(0).getText(), cast);
      if (step.connections.isEmpty()) {
         connection.previousAsset = asset.name;
      }
      step.connections.add(connection);
   }

   public void parseExpressionStep(ExpressionStepContext ctx, Step step) {
      parseExpressionStep(ctx.Identifier(), step);
   }

   @Override
   public void enterNormal(NormalContext ctx) {
      // Normal step with any amount of steps, may or may not have specified
      // type.
      Step step = new Step(asset.name, attackStep.name, ctx.Identifier().getText());
      for (ExpressionStepContext esc : ctx.expressionStep()) {
         parseExpressionStep(esc, step);
      }
      attackStep.steps.add(step);
   }

   public void parseSetOperation(List<PreExpressionStepContext> pre, SetOperationContext ctx, Step step) {
      // (alpha /\ bravo /\ (charlie[golf] \/ delta)[hotel])[echo].foxtrot
      // We make sure to always bring whatever was infront of our setoperation,
      // finally it is prepended before any normal expression steps are
      // evaluated
      pre.addAll(ctx.preExpressionStep());
      SelectConnection select = new SelectConnection();
      select.previousAsset = asset.name;
      select.cast = (ctx.Identifier() != null ? ctx.Identifier().getText() : "");

      for (SetChildContext scc : ctx.setChild()) {
         Step childStep = new Step(asset.name, attackStep.name, "");
         if (scc.setOperation() != null) {
            // parse again recusive
            parseSetOperation(pre, scc.setOperation(), childStep);
         }
         else {
            // normal
            for (PreExpressionStepContext pesc : pre) {
               parseExpressionStep(pesc.Identifier(), childStep);
            }
            for (ExpressionStepContext esc : scc.expressionStep()) {
               parseExpressionStep(esc, childStep);
            }
         }
         select.steps.add(childStep);
      }
      for (SetOperatorContext soc : ctx.setOperator()) {
         select.operators.add(soc.getText());
      }
      step.connections.add(select);
      for (ExpressionStepContext esc : ctx.expressionStep()) {
         parseExpressionStep(esc, step);
      }
   }

   @Override
   public void enterSelect(SelectContext ctx) {
      // Select step, may have any amount of intermediate steps. Select step may
      // also have type, followed by any amount of normal steps.
      String attack = ctx.Identifier().getText();
      Step step = new Step(asset.name, attackStep.name, attack);
      parseSetOperation(new ArrayList<PreExpressionStepContext>(), ctx.setOperation(), step);
      attackStep.steps.add(step);
   }
}
