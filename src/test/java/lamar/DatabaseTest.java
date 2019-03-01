package lamar;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import lamar.database.Database;
import lamar.database.ValueSet;
import lamar.database.nutrient.Catagory;
import lamar.database.nutrient.Nutrient;
import lamar.database.nutrient.NutrientProvider;
import lamar.database.nutrient.ProviderFactory;

/**
 * Unit test for Database.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DatabaseTest 
{
    private static final String DB_LOC = "target/nutritionGen.db";

    @BeforeClass
    public static void removeTestDb() throws IOException {
      Path p = Paths.get(DB_LOC);
      Files.deleteIfExists(p);
    }

    @Test
    public void aCreateDB() throws SQLException{
      try(Database db = Database.createIfAbsent(DB_LOC)) {
      } catch(Exception e) {
        assertTrue("db creation failed", false);
      }
    }

    @Test
    public void bAddContains() throws SQLException, JsonProcessingException {
      Database db = Database.getInstance(DB_LOC);

      ProviderFactory fctry = new ProviderFactory(); 
      NutrientProvider red = fctry.setName("red")    
          .setUnit("gram")
          .setType(Catagory.INGREDIENT)
          .setPerUnit(100)
          .setNutrient(Nutrient.ENERGY, 100)
          .build();

      fctry = new ProviderFactory();
      NutrientProvider blue = fctry.setName("blue")    
          .setUnit("gram")
          .setType(Catagory.INGREDIENT)
          .setPerUnit(100)
          .setNutrient(Nutrient.SUGAR, 100)
          .build(); 

      fctry = new ProviderFactory();
      NutrientProvider yellow = fctry.setName("yellow")    
          .setUnit("gram")
          .setType(Catagory.INGREDIENT)
          .setPerUnit(100)
          .setNutrient(Nutrient.FIBER, 100)
          .build(); 

      fctry = new ProviderFactory();
      NutrientProvider purple = fctry.setName("purple")    
          .setUnit("gram")
          .setType(Catagory.RECIPE)
          .setPerUnit(100)
          .addConstituent(blue, 1)
          .addConstituent(red, 1)
          .buildCompositeTable()
          .build(); 

      fctry = new ProviderFactory();
      NutrientProvider green = fctry.setName("green")    
          .setUnit("gram")
          .setType(Catagory.RECIPE)
          .setPerUnit(100)
          .addConstituent(blue, 1)
          .addConstituent(yellow, 1)
          .buildCompositeTable()
          .build(); 

      fctry = new ProviderFactory();
      NutrientProvider brown = fctry.setName("brown")    
          .setUnit("gram")
          .setType(Catagory.MEAL)
          .setPerUnit(100)
          .addConstituent(purple, 1)
          .addConstituent(green, 2)
          .buildCompositeTable()
          .build(); 

      db.addAll(red, blue, yellow, purple, green, brown);
      String err = "error adding ";
      assertTrue(err + "ingredient", db.contains(red));
      assertTrue(err + "ingredient", db.contains(blue));
      assertTrue(err + "recipe", db.contains(purple));
      assertTrue(err + "recipe", db.contains(green));
      assertTrue(err + "meal", db.contains(brown));
      db.close();
    }

    @Test
    public void cRemove() throws SQLException, IOException {
      Database db = Database.getInstance(DB_LOC);
      db.remove(Catagory.INGREDIENT, "yellow"); 
      
      String err = "error removing dependent "; 
      assertTrue(err + "meal", !db.contains(Catagory.MEAL, "brown"));
      assertTrue(err + "recipe", !db.contains(Catagory.RECIPE, "green"));
      assertTrue(err + "ingredient", !db.contains(Catagory.INGREDIENT, "yellow"));
      assertTrue(err + "recipe", db.contains(Catagory.RECIPE, "purple"));
      assertTrue(err + "ingredient", db.contains(Catagory.INGREDIENT, "blue"));
    }

}
