package se.kth.mal;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import se.kth.mal.steps.FinalStep;
import se.kth.mal.steps.NormalStep;
import se.kth.mal.steps.SelectStep;
import se.kth.mal.steps.Step;
import se.kth.mal.steps.TypeStep;

public class SecuriLangListener extends sLangBaseListener {

   String            path;
   List<AttackStep>  containerSteps      = new ArrayList<>();
   CompilerModel     model;
   Asset             currentAsset;
   // --
   AttackStep        currentAttackStep;
   Step              first;
   Step              current;
   int               dots                = 0;
   Stack<Step>       savedFirst          = new Stack<>();
   Stack<Integer>    savedDots           = new Stack<>();
   Stack<SelectStep> savedSteps          = new Stack<>();
   // --
   String            currentCategoryName = "NoCategoryName";

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

   // TODO: Clean up referring to attacksteps/existance reqs.

   @Override
   public void enterAttackStepDeclaration(sLangParser.AttackStepDeclarationContext ctx) {
      System.out.println("+ATTACKSTEP.");
      AttackStep attackStep;
      attackStep = currentAsset.addAttackStep(true, ctx.attackStepType().getText(), ctx.Identifier().getText());
      currentAttackStep = attackStep;
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

   // -----

   @Override
   public void enterDot(sLangParser.DotContext ctx) {
      // Increment our current dot counter.
      dots++;
   }

   @Override
   public void exitDot(sLangParser.DotContext ctx) {
      dots--;
      if (dots == 0) {
         // We have traversed a full chain.
         if (savedSteps.empty()) {
            // We are NOT inside a select step. We have to convert current to a
            // final step and add the first step to reached steps.
            current.prev.next = new FinalStep(((NormalStep) current).name);
            currentAttackStep.steps.add(first);
         }
         else {
            // We are inside a select step. Fill whatever slot is not taken.
            if (savedSteps.lastElement().left != null) {
               savedSteps.lastElement().right = first;
            }
            else {
               savedSteps.lastElement().left = first;
            }
         }
         // Reset first pointer to allow for new chain.
         first = null;
      }
   }

   @Override
   public void enterNormal(sLangParser.NormalContext ctx) {
      NormalStep step = new NormalStep(ctx.getText());
      if (first != null) {
         current.next = step;
         step.prev = current;
      }
      else {
         first = step;
      }
      current = step;
   }

   @Override
   public void enterType(sLangParser.TypeContext ctx) {
      TypeStep step = new TypeStep(ctx.Identifier().getText(), ctx.expressionType().getText());
      if (first != null) {
         current.next = step;
         step.prev = current;
      }
      else {
         first = step;
      }
      current = step;
   }

   public void enterSelect(char type) {
      SelectStep step = new SelectStep(type);
      if (first != null) {
         current.next = step;
         step.prev = current;
      }
      else {
         first = step;
      }
      // We save the select step, current dots and first pointer. These may be
      // used post select. We reset first and dots to allow for new chains.
      savedSteps.push(step);
      savedFirst.push(first);
      savedDots.push(dots);
      first = null;
      dots = 0;
   }

   public void exitSelect() {
      // Restore the values we saved.
      first = savedFirst.pop();
      dots = savedDots.pop();
      current = savedSteps.pop();

      if (!savedSteps.empty()) {
         // In the event we have nested selects, exitDot will NOT handle filling
         // the previous selects slots. We do it here instead and reset first
         // for new chains.
         if (savedSteps.lastElement().left != null) {
            savedSteps.lastElement().right = first;
         }
         else {
            savedSteps.lastElement().left = first;
         }
         first = null;
      }
   }

   @Override
   public void enterOr(sLangParser.OrContext ctx) {
      enterSelect('|');
   }

   @Override
   public void exitOr(sLangParser.OrContext ctx) {
      exitSelect();
   }

   @Override
   public void enterAnd(sLangParser.AndContext ctx) {
      enterSelect('&');
   }

   @Override
   public void exitAnd(sLangParser.AndContext ctx) {
      exitSelect();
   }

   @Override
   public void enterChildren(sLangParser.ChildrenContext ctx) {
      System.out.println(String.format("CHILDREN: %s", ctx.getText()));
   }

   @Override
   public void exitChildren(sLangParser.ChildrenContext ctx) {
      System.out.println("ext_CHILDREN");
   }

   // ---
   protected List<AttackStepPointer> getChildPointers(sLangParser.ChildrenContext ctx) {
      List<AttackStepPointer> childPointers = new ArrayList<>();
      for (sLangParser.ExpressionContext exp : ctx.expression()) {
         // Decorate .g4 file with # asdf and override enterBlaBlacontext use
         // Current asset/pointer etc
      }

      // for (sLangParser.ExpressionNameContext enc : ctx.expressionName()) {
      // AttackStepPointer pointer = currentAsset.addStepPointer();
      // pointer.roleName = enc.Identifier().getText();
      //
      // // Traversing backwards
      // AmbiguousNameContext anc = enc.ambiguousName();
      // while (anc != null) {
      // AttackStepPointer ptr = currentAsset.addStepPointer();
      // ptr.attackStepPointer = pointer;
      // pointer = ptr;
      // pointer.roleName = anc.Identifier().getText();
      // if (anc.type() != null) {
      // pointer.type = anc.type().Identifier().getText();
      // }
      // anc = anc.ambiguousName();
      // }
      //
      // childPointers.add(pointer);
      // }
      return childPointers;
   }
}
