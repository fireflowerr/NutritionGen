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

  private static final String DB_NAME = "nutritionGen.db";

  // Consumes the whole line on every token consumption. Makes getting numericals easier. Do not use
  // nextLine.
  private static Scanner stdn;
  static {
    stdn = new Scanner(System.in);
    String osName = System.getProperty("os.name").toLowerCase();
    if(osName.startsWith("windows")) {
      stdn.useDelimiter("\\r\\n");
    } else {
      stdn.useDelimiter("\\n");
    }
  }

  /**
  * @param args the command line arguments
  */
  public static void main(String[] args) {

    if(args.length == 0) {
      System.err.println("Expects database destination dir as arg");
      System.exit(1);
    }

    String dbLoc = args[0];
    if(!dbLoc.matches("/$")) {
      dbLoc += "/";
    }
    try(Database db = Database.createIfAbsent(dbLoc + DB_NAME)) {
      System.out.println("Database created or initialized at " + args[0]);

      String[] options = { "add", "lookup" };
      boolean again = true;
      while(again) {

        switch(getMenuSelect(options, "exit")) {
          case 0:
            System.exit(0);
          case 1: 
            addEntry(db);
            continue;
          case 2: 
            lookupEntry(db);
            continue;
          default: break;
        }

        again = getResponse("again");
      }

    } catch(Exception e) {
      System.err.println(e.getMessage());
      if(getResponse("show log")) {
        e.printStackTrace();
      }
    }

  }

  private static void lookupEntry(Database db) throws SQLException, IOException {
    
    boolean again = true;
    String[] options = Arrays.stream(Catagory.values())
        .map(x -> x.toString())
        .toArray(String[]::new);
    Catagory[] types = Catagory.values();
    while(again) {

      int selection = getMenuSelect(options, "back");
      if(selection >= 1) {
        List<NutrientProvider> entries = db.getType(types[selection - 1]);
        
        if(entries.isEmpty()) {
          System.out.println("Empty");
        } else
          expandProv(entries, db);
        } else if(selection == 0) {
          again = false;
        } else {
          again = getResponse("again");
        }
      }
  }

  private static void expandProv(List<NutrientProvider> list, Database db) throws SQLException, IOException {
    String[] options = list.stream()
        .map(x -> x.getName())
        .toArray(String[]::new);

    int selection = getMenuSelect(options, "back");  
    if(selection >= 1) {
      NutrientProvider prov = list.get(selection - 1);
      System.out.println(prov.label() + "\n");

      Catagory type = prov.getType();

      options = type == Catagory.INGREDIENT ? 
          new String[] { "remove" } :
          new String[] { "remove", "lookup ingredient value" };

      selection = getMenuSelect(options, "back");
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
    if(getResponse(msg)) {
      db.remove(prov);
    }
  }

  private static void addEntry(Database db) throws IOException, SQLException {
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
      int selection = getMenuSelect(baseAction, "end");
      if(selection >= 0) {

        switch(selection) {
          case 0 :
            again = false;
            break;
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
              if(fctry.getConstituent().isEmpty()) {
                break;
              }
              fctry.buildCompositeTable();
            }
            break;
          default : 
            again = false;
        }
      } else {
        again = getResponse("again");
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
    int v = getMenuSelect(options, "");
    return v <= 0 ? null : types[v - 1]; 
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

      int selection = getMenuSelect(options, "back");
      if(selection >= 0) {

        switch(selection) {
          case 0 :
            return table;
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

      again = getResponse("again");
    }
    return table;
  }

  private static HashMap<NutrientProvider, Pair<Catagory, Double>> 
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

        int i = getMenuSelect(options, "back");
        if(i == 0) {
          return constituent;
        }

        i--;
        if(i >= 0) {

          NutrientProvider selection = value.get(i);
          double multiplicty = validateGetDouble("Enter multiplicty", 0, Double.MAX_VALUE);
          String name = selection.getName();

          if(db.contains(type, name)) {
           constituent.put(db.get(type, name), new Pair<>(type, multiplicty));
          }
        }
      }

      again = getResponse("again");
    }

    return constituent;
  }

  private static int getMenuSelect(String[] options, String escape) {
    if(!escape.isEmpty()) {
      System.out.println("0: " + escape);
    }
    IntStream.range(0, options.length)
        .forEach(x -> System.out.println((x + 1) + ": " + options[x]));

    int in = validateGetInt("", 0, options.length); 
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

  private static boolean getResponse(String msg) {
    System.out.println(msg + " y/n");
    return stdn.next().toLowerCase().matches("^y.*");
  }

}
