package se.kth.mal;

import java.util.ArrayList;
import java.util.List;

public class AttackStepPointer {
   String            roleName       = "";
   String            attackStepName = "";
   AttackStep        attackStep;
   Association       association;
   String            multiplicity;
   Asset             asset;
   List<String>      roleNames      = new ArrayList<>();
   AttackStepPointer attackStepPointer;

   public Asset getAsset() {
      return asset;
   }

   public AttackStepPointer getAttackStepPointer() {
      return attackStepPointer;
   }

   public List<String> getRoleNames() {
      return roleNames;
   }

   public String getRoleName() {
      return roleName;
   }

   public String getAttackStepName() {
      return attackStepName;
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
}
