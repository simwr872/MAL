package core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Attacker {

   protected Set<AttackStep> activeAttackSteps  = new HashSet<>();
   Boolean                   verbose            = false;
   public static String      defaultProfilePath = "./target/generated-sources/attackerProfile.ttc";

   public Attacker() {
      verbose = false;
   }

   public Attacker(boolean verbose) {
      this.verbose = verbose;
   }

   public void addAttackPoint(AttackStep attackPoint) {
      attackPoint.ttc = 0;
      activeAttackSteps.add(attackPoint);
   }

   public void addRandomAttackPoint(long randomSeed) {
      AttackStep attackPoint = AttackStep.randomAttackStep(randomSeed);
      System.out.println("Attack point: " + attackPoint.fullName());
      addAttackPoint(attackPoint);
   }

   private AttackStep getShortestActiveStep() {
      AttackStep shortestStep = null;
      double shortestTtc = Double.MAX_VALUE;
      for (AttackStep attackStep : activeAttackSteps) {
         if (attackStep.ttc < shortestTtc) {
            shortestTtc = attackStep.ttc;
            shortestStep = attackStep;
         }
      }
      return shortestStep;
   }

   public void reset() {
      for (AttackStep attackStep : AttackStep.allAttackSteps) {
         attackStep.ttc = Double.MAX_VALUE;
      }
   }

   private void debugPrint(String str) {
      if (verbose) {
         System.out.print(str + "\n");
      }
   }

   private Map<String, Double> readProfile(String profilePath) {
      Properties profile = new Properties();
      try {
         profile.load(new FileInputStream(profilePath));
      }
      catch (IOException e) {
         System.err.printf("Could not open profile: %s\n", profilePath);
         System.exit(1);
      }
      Map<String, Double> profileMap = new HashMap<>();
      Pattern pattern = Pattern.compile("([a-z]+)\\(*([0-9.]+)*,*([0-9.]+)*\\)*", Pattern.CASE_INSENSITIVE);
      for (String name : profile.stringPropertyNames()) {
         Matcher matcher = pattern.matcher(profile.getProperty(name));
         matcher.matches();
         switch (matcher.group(1)) {
            case "Zero":
               profileMap.put(name, AttackStep.oneSecond);
               break;
            case "Infinity":
               profileMap.put(name, AttackStep.infinity);
            case "ExponentialDistribution":
               profileMap.put(name, Double.valueOf(matcher.group(2)));
               break;
            case "GammaDistribution":
               profileMap.put(name, Double.valueOf(matcher.group(2)) * Double.valueOf(matcher.group(3)));
               break;
            case "UniformDistribution":
               profileMap.put(name, (Double.valueOf(matcher.group(3)) - Double.valueOf(matcher.group(2))) / 2);
               break;
            default:
               break;
         }
      }
      return profileMap;
   }

   public void attack() {
      System.err.println("No attacker profile selected! Trying default path...");
      attack(defaultProfilePath);
   }

   private void attack(String profilePath) {
      AttackStep.ttcHashMap = readProfile(profilePath);

      debugPrint("The model contains " + Integer.toString(Asset.allAssets.size()) + " assets and " + Integer.toString(AttackStep.allAttackSteps.size()) + " attack steps.");
      AttackStep currentAttackStep = null;
      debugPrint("AttackStep.allAttackSteps = " + AttackStep.allAttackSteps);

      for (AttackStep attackStep : AttackStep.allAttackSteps) {
         attackStep.setExpectedParents();
         debugPrint("The expected parents of " + attackStep.fullName() + " are " + attackStep.expectedParents);
      }

      for (Defense defense : Defense.allDefenses) {
         if (!defense.isEnabled()) {
            addAttackPoint(defense.disable);
         }
      }

      while (!activeAttackSteps.isEmpty()) {
         debugPrint("activeAttackSteps = " + activeAttackSteps);
         currentAttackStep = getShortestActiveStep();
         debugPrint("Updating children of " + currentAttackStep.fullName());
         currentAttackStep.updateChildren(activeAttackSteps);
         activeAttackSteps.remove(currentAttackStep);
      }
   }

}
