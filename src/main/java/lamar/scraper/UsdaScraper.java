package lamar.scraper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lamar.database.Pair;
import lamar.database.nutrient.Catagory;
import lamar.database.nutrient.Nutrient;
import lamar.database.nutrient.ProviderFactory;

public class UsdaScraper {

  private static String SEARCH_PREFACE = "https://ndb.nal.usda.gov/ndb/search/list?S&qlookup=";
  private static String ENTRY_PREFACE = "https://ndb.nal.usda.gov/ndb/foods/show/";

  private UsdaScraper() {}

  public static HashMap<Pair<String, String>, Integer> getSearchResults(String term) throws IOException {
    Document doc = Jsoup.connect(SEARCH_PREFACE + term).get();
    Elements table = doc.select("table > tbody > tr");
    HashMap<Pair<String, String>, Integer> results = new HashMap<>();
    
    for(Element row : table) {
      
      Elements col = row.select("td");
      
      Pair<String, String> nameAndMnfctr = new Pair<>(col.get(2).text(), String.format("%3.20s", col.get(3).text())); 
      results.put(nameAndMnfctr, Integer.valueOf(col.get(1).text()));
    }

    return results;
  }

  public static ProviderFactory getIngredient(int id) throws IOException { 
    Document doc = Jsoup.connect(ENTRY_PREFACE + id).get();
    ProviderFactory fctry = new ProviderFactory();
    fctry.setType(Catagory.INGREDIENT);

    Pair<String, Integer> measure = getUnitsPer(doc);
    fctry.setUnit(measure.getValue0())
        .setPerUnit(measure.getValue1());
      
    setTable(doc, fctry);

    return fctry;
  }

  private static void setTable(Document doc, ProviderFactory fctry) throws IOException {
    int colNum = getUnitCol(doc);
    Elements table = doc.select("table#nutdata > tbody > tr");

    Iterator<Element> itr = table.iterator();
    while(itr.hasNext()) {
      Element elem = itr.next();
      Elements col = elem.select("td");

      String ntrStr = fmtWebNutrient(col.get(1).text());
      if(!ntrStr.isEmpty()) {
        Nutrient ntr = nutrientFromWebFmt(ntrStr); 
        double val = Double.valueOf(col.get(colNum).text());
        fctry.setNutrient(ntr, val);
      }
    }
  }

  private static Pair<String, Integer> getUnitsPer (Document doc) throws IOException {
    Elements header = doc.select("table.display").select("tr").first().select("th");
    Optional<String> measureOpt = header.stream()
        .map(x -> x.text())
        .filter(x -> x.contains("per"))
        .findFirst();

    String measure = measureOpt.orElseThrow(() -> new IOException("no per unit listing found in table header"));
    measure = measure.trim();
    String[] tokens = measure.split("\\s+|" + ((char)160));

    Pair<String, Integer> toRet = new Pair<>(tokens[3], Integer.valueOf(tokens[2]));
    return toRet;
  }

  private static int getUnitCol(Document doc) throws IOException {
    Elements header = doc.select("table.display").select("tr").first().select("th");
    int sz = header.size();

    int i = 0;
    Iterator<Element> itr = header.iterator();
    while(itr.hasNext()) {
      
      String col = itr.next().text();
      if(col.contains("per")) {
        break;
      } 
      i++;
    }

    if(i >= sz) {
      throw new IOException("unable to find measure column in table header");
    }
    return i;
  }

  private static Nutrient nutrientFromWebFmt(String n) {
    switch(n) {
      case "energy": 
        return Nutrient.ENERGY;
      case "protein":
        return Nutrient.PROTEIN;
      case "lipid":
        return Nutrient.LIPID;
      case "carbohydrate":
        return Nutrient.CARBS;
      case "fiber":
        return Nutrient.FIBER;
      case "sugars":
        return Nutrient.SUGAR;
      default:
        throw new IllegalStateException("non existant existant enum or defective switch");
    }
  }

  private static String fmtWebNutrient(String n) {
    n = n.toLowerCase();
    if(n.contains("energy"))
      return "energy";
    else if(n.contains("protein"))
      return "protein";
    else if(n.contains("lipid"))
      return "lipid";
    else if(n.contains("carbohydrate"))
      return "carbohydrate";
    else if(n.contains("fiber"))
      return "fiber";
    else if(n.contains("sugar"))
      return "sugars";
    else return "";

  }
}
