package se.kth.mal;

import org.antlr.v4.runtime.tree.TerminalNode;

import se.kth.mal.sLangParser.CategoryDeclarationContext;
import se.kth.mal.sLangParser.ExpressionStepContext;
import se.kth.mal.sLangParser.ImmediateContext;
import se.kth.mal.sLangParser.NormalContext;
import se.kth.mal.sLangParser.SelectContext;
import se.kth.mal.steps.Connection;
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

   // TODO: Clean up referring to attacksteps/existance reqs.

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
   // --------------
   // Reached attack steps take the form of 'a.b[c].(d & e).f'. Called chains,
   // each step separated by a dot. An entire chain will always end with an
   // attack step except for when a chain is started inside parenthesis, there
   // it will end with a set.

   @Override
   public void enterImmediate(ImmediateContext ctx) {
      Step step = new Step(asset.name, attackStep.name, ctx.Identifier().getText());
      attackStep.steps.add(step);
   }

   @Override
   public void enterNormal(NormalContext ctx) {
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
      // Chain chain = new Chain(asset.name, attackStep.name);
      // SelectLink select = new SelectLink();
      // for (ExpressionChildContext child : ctx.expressionChild()) {
      // Chain childChain = new Chain(asset.name, "");
      // for (TerminalNode step : child.Identifier()) {
      // childChain.links.add(new Link(step.getText()));
      // }
      // select.chains.add(childChain);
      // }
      // for (SetOperatorContext operator : ctx.setOperator()) {
      // select.operators.add(operator.getText());
      // }
      // chain.links.addAll(parseNormalSteps(ctx.expressionStep()));
      // if (ctx.Identifier().size() > 1) {
      // select.type = ctx.Identifier(0).getText();
      // chain.targetStep = ctx.Identifier(1).getText();
      // }
      // else {
      // chain.targetStep = ctx.Identifier(0).getText();
      // }
      // chain.links.add(select);
      // attackStep.chains.add(chain);
   }

   // --------------

}
