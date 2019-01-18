package se.kth.mal;

public class AttackStepPointer {
   String            roleName = "";
   AttackStep        attackStep;
   Association       association;
   String            multiplicity;
   String            type     = "";
   Asset             asset;
   AttackStepPointer attackStepPointer;

   public Asset getAsset() {
      return asset;
   }

   public AttackStepPointer getAttackStepPointer() {
      return attackStepPointer;
   }

   public String getRoleName() {
      return roleName;
   }

   public String getMultiplicity() {
      return multiplicity;
   }

   public AttackStep getAttackStep() {
      return attackStep;
   }

   public Association getAssociation() {
      return association;
   }

   public String getType() {
      return type;
   }
}
