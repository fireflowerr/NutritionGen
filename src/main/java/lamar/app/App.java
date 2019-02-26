package lamar.app;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.IntStream;

import lamar.database.Database;
import lamar.database.nutrient.Catagory;
import lamar.database.nutrient.Nutrient;
import lamar.database.nutrient.NutrientProvider;
import lamar.database.nutrient.ProviderFactory;
 
/**
 *
 * @author sqlitetutorial.net
 */
public class App {

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

      boolean again = true;
      while(again) {
        addEntry(db);
        again = again("again");
      }

    } catch(Exception e) {
      e.printStackTrace();
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
      if(selection < 1) {
        return;
      }

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
        default : 
          break;
      }

      again = again("again");
    }
    
    db.add(fctry.build());
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

      switch(getMenuSelect(options)) {
        case 1 : 
          table[0] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
          break; 
        case 2 : 
          table[1] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
          break; 
        case 3 : 
          table[2] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
          break; 
        case 4 : 
          table[3] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
          break; 
        case 5 : 
          table[4] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
          break; 
        case 6 : 
          table[5] = validateGetDouble(instrct, 0, Double.MAX_VALUE);  
          break; 
        default :
          break;
      }

      again = again("again");
    }
    return table;
  }

  public static HashMap<NutrientProvider, Double> getConstituentIO(String msg, Database db) throws SQLException, IOException {
    System.out.println(msg);
    HashMap<NutrientProvider, Double> constituent = new HashMap<>();
    
    boolean again = true;   
    while(again) {
      Catagory type = getTypeIO("Enter type for lookup query"); 
      String name = getStringIO("Enter name for lookup query");
      double multiplicty = validateGetDouble("Enter multiplicty", 0, Double.MAX_VALUE);

      if(db.contains(type, name)) {
       constituent.put(db.get(type, name), multiplicty);
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
    System.out.println("input '" + errToken + "' is either invalid or out of range."); 
    return again("again") ? validateGetInt(msg, start, end) : -1;
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
    return again("again") ? validateGetDouble(msg, start, end) : -1;
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

    Nutrient[] n = Nutrient.values();

    return stdn.next();  
  }

}
