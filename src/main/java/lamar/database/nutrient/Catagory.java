package lamar.database.nutrient;

public enum Catagory { 
    MEAL(0), RECIPE(1), INGREDIENT(2);
    public final int p;
    
    private Catagory(int p) {
      this.p = p;
    }
}

