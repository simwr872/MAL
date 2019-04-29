package se.kth.mal.antlr4;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import se.kth.mal.Asset;
import se.kth.mal.AttackStep;
import se.kth.mal.CompilerModel;
import se.kth.mal.MalBaseListener;
import se.kth.mal.MalParser.AssetContext;
import se.kth.mal.MalParser.AssociationContext;
import se.kth.mal.MalParser.AttackstepContext;
import se.kth.mal.MalParser.CategoryContext;
import se.kth.mal.MalParser.ExprContext;
import se.kth.mal.MalParser.IncludeContext;
import se.kth.mal.MalParser.OperatorContext;
import se.kth.mal.MalParser.StatementContext;
import se.kth.mal.steps.Connection;
import se.kth.mal.steps.DebugInfo;
import se.kth.mal.steps.RecursiveConnection;
import se.kth.mal.steps.SelectConnection;
import se.kth.mal.steps.Step;

public class MalListener extends MalBaseListener {
   private CompilerModel            model;
   private String                   category;
   private Asset                    asset;
   private AttackStep               attackStep;
   private Map<String, ExprContext> variables;

   public MalListener(CompilerModel model) {
      this.model = model;
      this.category = "";
      this.variables = new HashMap<>();
   }

   @Override
   public void enterInclude(IncludeContext ctx) {
      this.model.parseFile(ctx.getText().replace(ctx.Include().getText(), ""));
   }

   @Override
   public void enterCategory(CategoryContext ctx) {
      this.category = ctx.Identifier().getText();
   }

   @Override
   public void exitCategory(CategoryContext ctx) {
      this.category = "";
   }

   public DebugInfo debug(ParserRuleContext ctx) {
      return new DebugInfo(ctx.getText(), ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine());
   }

   @Override
   public void enterAssociation(AssociationContext ctx) {
      String leftAsset = ctx.Identifier(0).getText();
      String leftField = ctx.type(0).Identifier().getText();
      String leftMult = ctx.multiplicity(0).getText();
      String name = ctx.Identifier(1).getText();
      String rightField = ctx.type(1).Identifier().getText();
      String rightMult = ctx.multiplicity(1).getText();
      String rightAsset = ctx.Identifier(2).getText();
      this.model.addAssociation(leftAsset, leftField, leftMult, "<--", name, "-->", rightMult, rightField, rightAsset);
   }

   @Override
   public void enterAsset(AssetContext ctx) {
      String name = ctx.Identifier(0).getText();
      boolean isAbstract = ctx.assetType().getText().equals("abstractAsset");
      String superAsset = ctx.Identifier().size() > 1 ? ctx.Identifier(1).getText() : "";
      this.asset = this.model.addAsset(name, superAsset, isAbstract);
      this.asset.category = this.category;
   }

   @Override
   public void enterAttackstep(AttackstepContext ctx) {
      this.variables.clear();
      String name = ctx.Identifier().getText();
      String type = ctx.attackstepType().getText();
      this.attackStep = this.asset.addAttackStep(true, type, name);
      this.attackStep.isExtension = ctx.reachedType() != null && ctx.reachedType().getText().equals("+>");

      if (ctx.ttc() != null) {
         String dist = ctx.ttc().Identifier().getText();
         this.attackStep.ttcFunction = dist;
         if (ctx.ttc().Parameters() != null) {
            String parameters = ctx.ttc().Parameters().getText();
            parameters = parameters.substring(1, parameters.length() - 1);
            String params[] = parameters.split(",");
            for (String param : params) {
               this.attackStep.ttcParameters.add(Float.parseFloat(param));
            }
         }

      }

      if (ctx.existence() != null) {
         for (TerminalNode id : ctx.existence().Identifier()) {
            attackStep.existenceRequirementRoles.add(id.getText());
         }
      }
   }

   @Override
   public void enterStatement(StatementContext ctx) {
      if (ctx.Identifier() != null) {
         String name = ctx.Identifier().getText();
         ExprContext expr = ctx.expr();
         this.variables.put(name, expr);
      }
      else {
         Step step = new Step(this.asset.name, this.attackStep.name, "");
         parseExpr(ctx.expr(), step, "");
         step.popStep();
         this.attackStep.steps.add(step);
      }
   }

   private void parseExpr(ExprContext ctx, Step step, String type) {
      // We have to parse expressions ourself since types may carry
      if (ctx.Identifier() != null) {
         // Normal, type is not overwritten. As an example consider
         // 'alpha.(bravo.charlie[Charlie])[SuperCharlie]'. Resulting type would
         // be SuperCharlie.
         String name = ctx.Identifier().getText();
         if (ctx.type() != null && type.isEmpty()) {
            type = ctx.type().Identifier().getText();
         }
         boolean transitive = ctx.Transitive() != null;
         if (this.variables.containsKey(name)) {
            parseExpr(this.variables.get(name), step, type);
         }
         else if (transitive) {
            step.connections.add(new RecursiveConnection(name));
            step.connections.get(step.connections.size() - 1).debug = debug(ctx);
         }
         else {
            step.connections.add(new Connection(name, type));
            step.connections.get(step.connections.size() - 1).debug = debug(ctx);
         }
      }
      else if (ctx.expr().size() == 1) {
         // Wrapped parenthesis, simply continue and carry type
         if (ctx.type() != null && type.isEmpty()) {
            type = ctx.type().Identifier().getText();
         }
         parseExpr(ctx.expr(0), step, type);
      }
      else if (ctx.operator().size() != 0) {
         // Set
         SelectConnection con = new SelectConnection();
         con.cast = type;
         for (OperatorContext op : ctx.operator()) {
            con.operators.add(op.getText());
         }
         for (ExprContext expr : ctx.expr()) {
            Step childStep = new Step(this.asset.name, this.attackStep.name, "");
            parseExpr(expr, childStep, "");
            con.steps.add(childStep);
         }
         step.connections.add(con);
         step.connections.get(step.connections.size() - 1).debug = debug(ctx);
      }
      else {
         // Chained. Type carries only for the left expression. As an example,
         // consider 'alpha.(bravo.charlie)[SuperCharlie]'.
         parseExpr(ctx.expr(0), step, "");
         parseExpr(ctx.expr(1), step, type);
      }
   }

}
