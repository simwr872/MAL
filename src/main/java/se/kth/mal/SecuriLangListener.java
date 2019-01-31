package se.kth.mal;

import org.antlr.v4.runtime.tree.TerminalNode;

import se.kth.mal.sLangParser.CategoryDeclarationContext;
import se.kth.mal.sLangParser.ExpressionChildContext;
import se.kth.mal.sLangParser.ExpressionStepContext;
import se.kth.mal.sLangParser.ImmediateContext;
import se.kth.mal.sLangParser.NormalContext;
import se.kth.mal.sLangParser.SelectContext;
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

   @Override
   public void enterNormal(NormalContext ctx) {
      // Normal step with any amount of steps, may or may not have specified
      // type.
      Step step = new Step(asset.name, attackStep.name, ctx.Identifier().getText());
      for (ExpressionStepContext esc : ctx.expressionStep()) {
         String cast = (esc.Identifier().size() > 1 ? esc.Identifier(1).getText() : "");
         Connection connection = new Connection(esc.Identifier(0).getText(), cast);
         if (step.connections.isEmpty()) {
            connection.previousAsset = asset.name;
         }
         step.connections.add(connection);
      }
      attackStep.steps.add(step);
   }

   @Override
   public void enterSelect(SelectContext ctx) {
      // Select step, may have any amount of intermediate steps. Select step may
      // also have type, followed by any amount of normal steps.
      String attack = (ctx.Identifier().size() > 1 ? ctx.Identifier(1).getText() : ctx.Identifier(0).getText());
      String cast = (ctx.Identifier().size() > 1 ? ctx.Identifier(0).getText() : "");
      Step step = new Step(asset.name, attackStep.name, attack);
      SelectConnection select = new SelectConnection();
      select.previousAsset = asset.name;
      select.cast = cast;
      for (ExpressionChildContext ecc : ctx.expressionChild()) {
         Step childStep = new Step(asset.name, attackStep.name, "");
         for (ExpressionStepContext esc : ecc.expressionStep()) {
            String _cast = (esc.Identifier().size() > 1 ? esc.Identifier(1).getText() : "");
            Connection connection = new Connection(esc.Identifier(0).getText(), _cast);
            if (step.connections.isEmpty()) {
               connection.previousAsset = asset.name;
            }
            childStep.connections.add(connection);
         }
         select.steps.add(childStep);
      }
      for (SetOperatorContext soc : ctx.setOperator()) {
         select.operators.add(soc.getText());
      }
      step.connections.add(select);

      for (ExpressionStepContext esc : ctx.expressionStep()) {
         String _cast = (esc.Identifier().size() > 1 ? esc.Identifier(1).getText() : "");
         Connection connection = new Connection(esc.Identifier(0).getText(), _cast);
         step.connections.add(connection);
      }
      attackStep.steps.add(step);
   }
}
