package lamar.database.nutrient;

import java.util.HashMap;
import lamar.database.Pair;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.ANY
)
public final class NutrientProvider {

  private final double[] table;

  private final String name;

  private final String unit;

  private final int per;

  private final Catagory type;

  // name -> multiplicity
  private final HashMap<String, Pair<Catagory, Double>> constituent;

  @JsonCreator
  public NutrientProvider(@JsonProperty("name") String name,
        @JsonProperty("constituent") HashMap<String, Pair<Catagory, Double>> constituent, 
        @JsonProperty("unit") String unit, @JsonProperty("per") int per,
        @JsonProperty("type") Catagory type, @JsonProperty("table") double[] table) {

    if(table.length != Nutrient.values().length) {
      throw new IllegalArgumentException("table must have the same length as Nutrient.values()");
    }
    this.table = table;
    this.name = name;
    this.unit = unit;
    this.per = per;
    this.type = type;
    this.constituent = constituent;
  }

  public String getName() {
    return name;
  }
  
  @JsonIgnore
  public Pair<String, Integer> getMeasure() {
    return new Pair<>(unit, per);
  }

  public Catagory getType() {
    return type;
  }

  public HashMap<Nutrient, Double> nutrTable()  {
    Nutrient[] v = Nutrient.values();
    int l = v.length;
    HashMap<Nutrient, Double> toRet = new HashMap<>(l);
    for(int i = 0; i < l; i++) {
      toRet.put(v[i], table[i]);
    }
    return toRet;
  }

  public String label() {
    String label = name + " " + type.toString() + "\n";
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

    if(!type.equals(Catagory.INGREDIENT)) {

     str.append("INGREDIENTS\n");
     for(String ingr : constituent.keySet()) {
        double m = constituent.get(ingr).getValue1(); 
              str.append(ingr)
              .append(": ")
              .append(m)
              .append("\n");
      }
    }

    return label + header + str.toString();
  }


  public double getRow(Nutrient id) {
    return table[id.index];
  }

  public HashMap<String, Pair<Catagory, Double>> getConstituent() {
    @SuppressWarnings("unchecked")
    HashMap<String, Pair<Catagory, Double>> toRet = (HashMap<String, Pair<Catagory, Double>>)constituent.clone();
    return toRet;
  }

  // USED FOR JSON SERIALIZATION

  @SuppressWarnings("unused")
  private double[] getTable() {
    return table;
  }

  @SuppressWarnings("unused")
  private String getUnit() {
    return unit;
  }

  @SuppressWarnings("unused")
  private int getPer() {
    return per;
  }

}
