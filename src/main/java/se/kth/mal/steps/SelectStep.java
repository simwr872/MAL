package se.kth.mal.steps;

public class SelectStep extends Step {
   public Step left;
   public Step right;
   public char type;

   public SelectStep(char type) {
      this.type = type;
   }
}
