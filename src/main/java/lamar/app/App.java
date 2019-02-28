package lamar.app;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

import lamar.database.Database;
import lamar.database.Pair;
import lamar.database.nutrient.Catagory;
import lamar.database.nutrient.Nutrient;
import lamar.database.nutrient.NutrientProvider;
import lamar.database.nutrient.ProviderFactory;
 
/**
 *
 * @author sqlitetutorial.net
 */
public class App {

  // Consumes the whole line on every token consumption. Makes getting numericals easier. Do not use
  // nextLine.
  private static Scanner stdn;
  static {
    stdn = new Scanner(System.in);
    stdn.useDelimiter("\\n");
  }

  /**
  * @param args the command line arguments
  */
  public static void main(String[] args) {

    if(args.length == 0) {
      System.err.println("Expects database destination dir as arg");
      System.exit(1);
    }

    try(Database db = Database.createIfAbsent(args[0])) {
      System.out.println("Database created or initialized at " + args[0]);

      String[] options = { "add", "lookup" };
      boolean again = true;
      while(again) {

        switch(getMenuSelect(options)) {
          case 1: 
            addEntry(db);
            continue;
          case 2: 
            lookupEntry(db);
            continue;
          default: break;
        }

        again = again("again");
      }

    } catch(Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }

  }

  public static void lookupEntry(Database db) throws SQLException, IOException {
    
    boolean again = true;
    String[] options = Arrays.stream(Catagory.values())
        .map(x -> x.toString())
        .toArray(String[]::new);
    Catagory[] types = Catagory.values();
    while(again) {

      int selection = getMenuSelect(options);
      if(selection >= 1) {
        List<NutrientProvider> entries = db.getType(types[selection - 1]);
        
        if(entries.isEmpty()) {
          System.out.println("Empty");
        } else
          expandProv(entries, db);
        }

        again = again("again");
      }
  }

  private static void expandProv(List<NutrientProvider> list, Database db) throws SQLException, IOException {
    String[] options = list.stream()
        .map(x -> x.getName())
        .toArray(String[]::new);

    int selection = getMenuSelect(options);  
    if(selection >= 1) {
      NutrientProvider prov = list.get(selection - 1);
      System.out.println(prov.label() + "\n");

      Catagory type = prov.getType();

      options = type == Catagory.INGREDIENT ? 
          new String[] { "remove" } :
          new String[] { "remove", "lookup ingredient value" };

      selection = getMenuSelect(options);
      switch(selection) {
        case 1: 
          removeEntry(prov, db); 
          break;
        case 2: 
          HashMap<String, Pair<Catagory, Double>> constituent = prov.getConstituent(); 
          List<NutrientProvider> newList = new ArrayList<>(constituent.size());
          
          for(String ingr : constituent.keySet()) {
            Pair<Catagory, Double> p = constituent.get(ingr);
            newList.add(db.get(p.getValue0(), ingr));
          }
          expandProv(newList, db);
          break;
      }
    }
  }

  private static void removeEntry(NutrientProvider prov, Database db) throws SQLException, IOException {
    String msg = "Warning: This operation will delete all entries which are dependent on this one."
        + "\ncontinue";
    if(again(msg)) {
      db.remove(prov);
    }
  }

  public static void addEntry(Database db) throws IOException, SQLException {
    ProviderFactory fctry = new ProviderFactory();
    boolean again = true;

    String[] baseAction = new String[] { "name", "unit", "per", "type" };
    int typeVal = -1;

    while(again) {

      Catagory type = fctry.getType();
      if(type != null && type.p != typeVal) {
        baseAction = type.p == Catagory.INGREDIENT.p ?  
            new String[] { "name", "unit", "per", "type", "values"} :
            new String[] { "name", "unit", "per", "type", "ingredients"};

      }

      System.out.println(fctry.label());
      int selection = getMenuSelect(baseAction);
      if(selection >= 1) {

        switch(selection) {
          case 1 :
            fctry.setName(getStringIO("Enter provider name"));
            break;
          case 2 :
            fctry.setUnit(getStringIO("Enter unit of measure"));
            break;
          case 3 :
            fctry.setPerUnit(validateGetInt("Enter multiplicity of unit measure", 0, Integer.MAX_VALUE));
            break;
          case 4 : 
            fctry.setType(getTypeIO("Select provider type"));
            break;
          case 5 : 
            if(type.p == Catagory.INGREDIENT.p) {
              fctry.setTable(getIngredientIO("Fill in ingredient table"));
            } else {
              fctry.setConstituent(getConstituentIO("Enter as: type, name", db));
              fctry.buildCompositeTable();
            }
            break;
          default : 
            again = false;
        }
      } else {
        again = false;
      }

    }
    
    if(fctry.validate()) {
      System.out.println("Entry added successfully");
      db.add(fctry.build());
    } else {
      System.out.println("Entry NOT added successfully");
    }
    
  }

  private static String getStringIO(String msg) {
    System.out.println(msg);
    return stdn.next();
  }

  private static Catagory getTypeIO(String msg) {
    System.out.println(msg);

    Catagory[] types = Catagory.values();
    String[] options =  Arrays.stream(types)
        .map(x -> x.toString())
        .toArray(String[]::new);
    int v = getMenuSelect(options);
    return v == -1 ? null : types[v - 1]; 
  }

  private static double[] getIngredientIO(String msg) {
    System.out.println(msg);

    Nutrient[] ntr = Nutrient.values();
    double[] table = new double[ntr.length];
    String[] options = Arrays.stream(ntr)
        .map(x -> x.toString())
        .toArray(String[]::new);

    boolean again = true;
    String instrct = "Enter value";
    while(again) {

      int selection = getMenuSelect(options);
      if(selection >= 1) {

        switch(selection) {
          case 1 : 
            table[0] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
            continue; 
          case 2 : 
            table[1] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
            continue; 
          case 3 : 
            table[2] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
            continue; 
          case 4 : 
            table[3] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
            continue; 
          case 5 : 
            table[4] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
            continue; 
          case 6 : 
            table[5] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
            continue; 
          default :
            break;
        }
      }

      again = again("again");
    }
    return table;
  }

  public static HashMap<NutrientProvider, Pair<Catagory, Double>> 
      getConstituentIO(String msg, Database db) throws SQLException, IOException {

    System.out.println(msg);
    HashMap<NutrientProvider, Pair<Catagory, Double>> constituent = new HashMap<>();
    
    boolean again = true;   
    while(again) {

      Catagory type = getTypeIO("Select type for lookup query"); 
      if(type != null) {
        List<NutrientProvider> value = db.getType(type);
        String[] options = value.stream()
            .map(x -> x.getName())
            .toArray(String[]::new);

        int i = getMenuSelect(options) - 1;
        if(i >= 0) {

          NutrientProvider selection = value.get(i);
          double multiplicty = validateGetDouble("Enter multiplicty", 0, Double.MAX_VALUE);
          String name = selection.getName();

          if(db.contains(type, name)) {
           constituent.put(db.get(type, name), new Pair<>(type, multiplicty));
          }
        }
      }

      again = again("again");
    }

    return constituent;
  }

  private static int getMenuSelect(String[] options) {
    IntStream.range(0, options.length)
        .forEach(x -> System.out.println((x + 1) + ": " + options[x]));

    int in = validateGetInt("", 1, options.length); 
    return in;
  }

  private static int validateGetInt(String msg, int start, int end) {
    System.out.println(msg);

    int in = 0;
    if(stdn.hasNextInt()) {

      in = stdn.nextInt();
      if(in >= start && in <= end) {
        return in;
      }
    }

    String errToken = in == 0 ? stdn.next() : Integer.toString(in);
    if(!errToken.equals("cancel")) {
      System.out.println("input '" + errToken + "' is either invalid or out of range."); 
    }
    return -1;
  }

  private static double validateGetDouble(String msg, double start, double end) {
    System.out.println(msg);
    
    double in = 0;
    if(stdn.hasNextDouble()) {

      in = stdn.nextDouble();
      if(in >= start && in < end) {
        return in;
      }
    }

    String errToken = in == 0 ? stdn.next() : Double.toString(in);
    System.out.println("input '" + errToken + "' is either invalid or out of range."); 
    return -1;
  }

  private static boolean again(String msg) {
    System.out.println(msg + " y/n");
    return stdn.next().toLowerCase().matches("^y.*");
  }

  public static String getProv() {
    ProviderFactory fctry = new ProviderFactory();
    System.out.println("Enter NutrientProvider name");
    fctry.setName(stdn.next());

    Catagory vals[] = Catagory.values();
    System.out.println("Enter NutrientProvider type");
    IntStream.range(0, Catagory.values().length)
        .mapToObj(x -> (1 + x) + vals[x].toString())
        .forEach(System.out::println);
    int selection = Integer.valueOf(stdn.next());
    fctry.setType(vals[selection - 1]); 

    System.out.println("Enter unit of measure");
    fctry.setUnit(stdn.next());

    System.out.println("Enter unit multiplicity");
    fctry.setPerUnit(Integer.valueOf(stdn.next()));

    return stdn.next();  
  }

}
