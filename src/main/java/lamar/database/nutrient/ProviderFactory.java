package lamar.database.nutrient;

import java.util.HashMap;
import java.util.Iterator;

import lamar.database.Pair;

public class ProviderFactory {

  private double[] table = new double[Nutrient.values().length];

  private String name;

  private String unit;

  private int perUnit;

  private Catagory type;

  private HashMap<NutrientProvider, Pair<Catagory, Double>> constituent = new HashMap<>();

  public String getName() {
    return name;
  }

  public ProviderFactory setName(String name) {
    this.name = name;
    return this;
  }

  public String getUnit() {
    return unit;
  }

  public ProviderFactory setUnit(String unit) {
    this.unit = unit;
    return this;
  }

  public int getPerUnit() {
    return perUnit;
  }

  public ProviderFactory setPerUnit(int perUnit) {
    this.perUnit = perUnit;
    return this;
  }

  public Catagory getType() {
    return type;
  }

  public ProviderFactory setType(Catagory type) {
    this.type = type;
    return this;
  }

  public ProviderFactory addConstituent(NutrientProvider n, double multiplicity) {
    if(n.getType().p < type.p) {
      throw new IllegalArgumentException("NutrientProvider hierarchy exception");
    }

    constituent.put(n, new Pair<>(type, multiplicity));
    return this;
  }

  public ProviderFactory removeConstituent(NutrientProvider n, double multiplicity) {
    if(n.getType().p < type.p) {
      throw new IllegalArgumentException("NutrientProvider hierarchy exception");
    }
    constituent.remove(n);
    return this;
  }

  public ProviderFactory buildCompositeTable() {
    if(constituent.isEmpty()) {
      throw new IllegalStateException("composite list is empty");
    }
   
    table = new double[Nutrient.values().length];
    int l = Nutrient.values().length;
    for(NutrientProvider k : constituent.keySet()) {
      double multiplier = constituent.get(k).getValue1(); 
      Iterator<Double> itr = k.nutrTable().values().iterator();

      for(int i = 0; i < l; i++) {
        table[i] += itr.next() * multiplier; 
      }
    }
    return this;
  }

  public double[] getTable() {
    return table;
  }

  public ProviderFactory setTable(double[] table) {
    if(table.length != Nutrient.values().length) {
      throw new IllegalArgumentException("table must have the same length as Nutrient.values()");
    }
    this.table = table;
    return this;
  }

  public ProviderFactory setNutrient(Nutrient n, double val) {
    table[n.index] = val;
    return this;
  }

  public ProviderFactory setConstituent(HashMap<NutrientProvider, Pair<Catagory, Double>> constituent) {
   this.constituent = constituent; 
   return this;
  }

  public NutrientProvider build() {
    HashMap<String, Pair<Catagory, Double>> transform = new HashMap<>(constituent.size());
    for(NutrientProvider k : constituent.keySet()) {
      transform.put(k.getName(), constituent.get(k));
    }

    if(name == null || unit == null || perUnit == 0 || type == null) {
      throw new IllegalStateException("NutrientProvider required fields " +
          "FAILED null check");
    }

    return new NutrientProvider(name, transform, unit, perUnit, type, table);
  }

  public boolean validate() {
    if(name == null || unit == null || perUnit == 0 || type == null) {
      return false;
    } else {
      return true;
    }
  }


  public String label() {
    String tString = type == null ? "null" : type.toString();
    String label = name + " " + tString + "\n";
    String header = "Nutrient, unit, value " + getMeasure() + "\n";
    StringBuilder str = new StringBuilder();
    String delim = ", ";

    for(Nutrient n : Nutrient.values()) {
      str.append(n) 
          .append(delim) 
          .append(n.unit)
          .append(delim)
          .append(table[n.index])
          .append("\n");
    }

    return label + header + str.toString();
  }

  public Pair<String, Integer> getMeasure() {
    return new Pair<>(unit, perUnit);
  }

  public HashMap<NutrientProvider, Pair<Catagory, Double>> getConstituent() {
    return constituent;
  }

}
