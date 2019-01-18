package se.kth.mal;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import se.kth.mal.sLangParser.AmbiguousNameContext;

public class SecuriLangListener extends sLangBaseListener {

   String           path;
   List<AttackStep> containerSteps      = new ArrayList<>();
   CompilerModel    model;
   Asset            currentAsset;
   String           currentCategoryName = "NoCategoryName";

   public SecuriLangListener(CompilerModel model) {
      this.model = model;
   }

   @Override
   public void enterAssetDeclaration(sLangParser.AssetDeclarationContext ctx) {
      boolean abstractAsset = ctx.getText().startsWith("abstract");
      System.out.println("asset = " + ctx.Identifier(0).getText());
      if (ctx.Identifier().size() == 1) {
         currentAsset = model.addAsset(ctx.Identifier(0).getText(), "", abstractAsset);
      }
      else {
         currentAsset = model.addAsset(ctx.Identifier(0).getText(), ctx.Identifier(1).getText(), abstractAsset);
      }
      if (ctx.description() != null) {
         currentAsset.setInfo(ctx.description().StringLiteral().getText().replaceAll("\"", ""));
      }
      if (ctx.rationale() != null) {
         currentAsset.setRationale(ctx.rationale().StringLiteral().getText().replaceAll("\"", ""));
      }
      ParserRuleContext parent = ctx.getParent();
      if (parent != null && parent instanceof sLangParser.CategoryDeclarationContext) {
         sLangParser.CategoryDeclarationContext categoryCtx = (sLangParser.CategoryDeclarationContext) parent;
         currentAsset.category = categoryCtx.Identifier().getText();
      }
      else {
         currentAsset.category = currentCategoryName;
      }
   }

   @Override
   public void enterAssociationDeclaration(sLangParser.AssociationDeclarationContext ctx) {
      model.addAssociation(ctx.Identifier(0).getText(), ctx.Identifier(1).getText(), ctx.multiplicity(0).getText(), ctx.leftRelation().getText(), ctx.Identifier(2).getText(),
            ctx.rightRelation().getText(), ctx.multiplicity(1).getText(), ctx.Identifier(3).getText(), ctx.Identifier(4).getText());
   }

   @Override
   public void enterAttackStepDeclaration(sLangParser.AttackStepDeclarationContext ctx) {
      AttackStep attackStep;
      attackStep = currentAsset.addAttackStep(true, ctx.attackStepType().getText(), ctx.Identifier().getText());
      if (!(ctx.children() == null)) {
         List<AttackStepPointer> childPointers = getChildPointers(ctx.children());
         attackStep.childPointers = childPointers;
      }
      if (containerSteps.size() > 0) {
         attackStep.containerStep = containerSteps.get(containerSteps.size() - 1);
      }
      containerSteps.add(attackStep);

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
      AttackStep attackStep;
      attackStep = currentAsset.addAttackStep(true, ctx.existenceStepType().getText(), ctx.Identifier().getText());
      if (!(ctx.existenceRequirements() == null)) {
         for (TerminalNode existenceRequirement : ctx.existenceRequirements().Identifier()) {
            attackStep.existenceRequirementRoles.add(existenceRequirement.getText());
         }
      }
      if (!(ctx.children() == null)) {
         List<AttackStepPointer> childPointers = getChildPointers(ctx.children());
         attackStep.childPointers = childPointers;
      }
      if (containerSteps.size() > 0) {
         attackStep.containerStep = containerSteps.get(containerSteps.size() - 1);
      }
      containerSteps.add(attackStep);

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
   public void exitAttackStepDeclaration(sLangParser.AttackStepDeclarationContext ctx) {
      containerSteps.remove(containerSteps.size() - 1);
   }

   protected List<AttackStepPointer> getChildPointers(sLangParser.ChildrenContext ctx) {
      List<AttackStepPointer> childPointers = new ArrayList<>();
      for (sLangParser.ExpressionNameContext enc : ctx.expressionName()) {
         AttackStepPointer pointer = currentAsset.addStepPointer();
         pointer.roleName = enc.Identifier().getText();

         // Traversing backwards
         AmbiguousNameContext anc = enc.ambiguousName();
         while (anc != null) {
            AttackStepPointer ptr = currentAsset.addStepPointer();
            ptr.attackStepPointer = pointer;
            pointer = ptr;
            pointer.roleName = anc.Identifier().getText();
            anc = anc.ambiguousName();
         }

         childPointers.add(pointer);
      }
      return childPointers;
   }
}
