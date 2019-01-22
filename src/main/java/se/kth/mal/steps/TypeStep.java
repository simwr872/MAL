package se.kth.mal.steps;

public class TypeStep extends Step {
   public String name;
   public String cls;
   public String type;

   public TypeStep(String name, String type) {
      this.name = name;
      this.type = type;
   }
}
