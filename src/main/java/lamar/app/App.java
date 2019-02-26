package lamar.app;

import java.sql.SQLException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;

import lamar.database.Database;
import lamar.database.nutrient.Catagory;
import lamar.database.nutrient.NutrientProvider;
import lamar.database.nutrient.ProviderFactory;
 
/**
 *
 * @author sqlitetutorial.net
 */
public class App {
 
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

      try(Database db = Database.createIfAbsent("/home/azae/Dev/test.db")) {
        HashMap<String, Double> test = new HashMap<>();
        test.put("bushel", 3d);
        test.put("peck", 1d);
        NutrientProvider prov = new NutrientProvider("boopion", test, "meows", 12,
            Catagory.INGREDIENT, new double[] {1, 2, 3, 4, 5, 6});

        NutrientProvider strbry = new ProviderFactory()
            .setName("strawberry")
            .setUnit("grams")
            .setPerUnit(100)
            .setType(Catagory.INGREDIENT)
            .setTable(new double[] { 100, 100, 100, 100, 100, 100 })
            .build();

        NutrientProvider cheese = new ProviderFactory()
            .setName("cheese")
            .setUnit("grams")
            .setPerUnit(100)
            .setType(Catagory.INGREDIENT)
            .setTable(new double[] { 100, 100, 100, 100, 100, 100 })
            .build();

        NutrientProvider cake = new ProviderFactory()
            .setName("cake")
            .setUnit("grams")
            .setPerUnit(100)
            .setType(Catagory.INGREDIENT)
            .setTable(new double[] { 150, 150, 150, 150, 150, 150 })
            .build();

        NutrientProvider strbryCheeseCake = new ProviderFactory()
            .setName("strawberry cheese cake")
            .setUnit("grams")
            .setPerUnit(100)
            .setType(Catagory.RECIPE)
            .addConstituent(strbry, 3)
            .addConstituent(cheese, 8)
            .addConstituent(cake, 12)
            .buildCompositeTable()
            .build();

        db.add(strbry);
        db.add(cheese);
        db.add(cake);
        db.add(strbryCheeseCake);

        db.remove(cheese);

        System.out.println("Writing strawberry cheesecake to db...");
        System.out.println(db.contains(strbry));
        System.out.println(db.contains(cheese));
        System.out.println(db.contains(cake));
        System.out.println(db.contains(strbryCheeseCake));

        System.out.println(db.get(Catagory.RECIPE, "strawberrrry cheese cake").label());

      } catch(Exception e) {
        e.printStackTrace();
      }

    }
}
