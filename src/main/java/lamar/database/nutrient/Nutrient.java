package lamar.database.nutrient;


public enum Nutrient { 
  ENERGY(0, "kcal"), PROTEIN(1, "g"), LIPID(2, "g"), CARBS(3, "g"), FIBER(4, "g"), SUGAR(5, "g");

  public final int index;
  public final String unit;

  private Nutrient(int index, String unit) {
    this.index = index;
    this.unit = unit;
  }
}  
