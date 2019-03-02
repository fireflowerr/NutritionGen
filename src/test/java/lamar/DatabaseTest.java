package lamar;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import lamar.database.Database;
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
    private static final NutrientProvider RED = new ProviderFactory() 
          .setName("red")    
          .setUnit("gram")
          .setType(Catagory.INGREDIENT)
          .setPerUnit(100)
          .setNutrient(Nutrient.ENERGY, 100)
          .build();

    private static final NutrientProvider BLUE = new ProviderFactory()
          .setName("blue")    
          .setUnit("gram")
          .setType(Catagory.INGREDIENT)
          .setPerUnit(100)
          .setNutrient(Nutrient.SUGAR, 100)
          .build(); 

    private static final NutrientProvider YELLOW = new ProviderFactory()
          .setName("yellow")    
          .setUnit("gram")
          .setType(Catagory.INGREDIENT)
          .setPerUnit(100)
          .setNutrient(Nutrient.FIBER, 100)
          .build(); 

    private static final NutrientProvider GREEN = new ProviderFactory()
          .setName("green")    
          .setUnit("gram")
          .setType(Catagory.RECIPE)
          .setPerUnit(100)
          .addConstituent(BLUE, 1)
          .addConstituent(YELLOW, 1)
          .buildCompositeTable()
          .build(); 

    private static final NutrientProvider PURPLE = new ProviderFactory()
          .setName("purple")    
          .setUnit("gram")
          .setType(Catagory.RECIPE)
          .setPerUnit(100)
          .addConstituent(BLUE, 1)
          .addConstituent(RED, 1)
          .buildCompositeTable()
          .build(); 

    private static final NutrientProvider BROWN = new ProviderFactory()
          .setName("brown")    
          .setUnit("gram")
          .setType(Catagory.MEAL)
          .setPerUnit(100)
          .addConstituent(PURPLE, 1)
          .addConstituent(GREEN, 2)
          .addConstituent(YELLOW, .03)
          .buildCompositeTable()
          .build(); 


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

      db.addAll(RED, BLUE, YELLOW, PURPLE, GREEN, BROWN);
      String err = "error adding ";
      assertTrue(err + "ingredient", db.contains(RED));
      assertTrue(err + "ingredient", db.contains(BLUE));
      assertTrue(err + "recipe", db.contains(PURPLE));
      assertTrue(err + "recipe", db.contains(GREEN));
      assertTrue(err + "meal", db.contains(BROWN));
      db.close();
    }

    @Test
    public void cRemove() throws SQLException, IOException {
      Database db = Database.getInstance(DB_LOC);
      db.remove(Catagory.INGREDIENT, "yellow"); 
      
      String err = "error removing dependent "; 
      assertTrue(err + "meal", !db.contains(BROWN));
      assertTrue(err + "recipe", !db.contains(GREEN));
      assertTrue(err + "ingredient", !db.contains(YELLOW));
      assertTrue(err + "recipe", db.contains(PURPLE));
      assertTrue(err + "ingredient", db.contains(BLUE));

      db.close();
    }

    @Test
    public void dGet() throws SQLException, IOException {
      Database db = Database.getInstance(DB_LOC);

      NutrientProvider red = db.get(RED.getType(), RED.getName());
      NutrientProvider blue = db.get(BLUE.getType(), BLUE.getName());
      NutrientProvider purple = db.get(PURPLE.getType(), PURPLE.getName());

      String err = "db GET error";

      assertTrue(err + "on RECIPE", PURPLE.equals(purple));
      assertTrue(err + "on INGREDIENT", red.equals(red));
      assertTrue(err + "on INGREDIENT", blue.equals(blue));
      
      boolean thrown = false;
      try {
        db.get(RED.getType(), PURPLE.getName());
      } catch(SQLException e) {
        thrown = true;
      }

      assertTrue("Failed to throw exception on invalid GET operation", thrown);
      db.close();
    }

    @Test
    public void eGetCol() throws SQLException {
      Database db = Database.getInstance(DB_LOC);
      List<String> dbNames = db.getColumn("name");

      String err = "Error constructing name column from DB";
      assertTrue(err, dbNames.remove(PURPLE.getName()));
      assertTrue(err, dbNames.remove(BLUE.getName()));
      assertTrue(err, dbNames.remove(RED.getName()));
      assertTrue(err, dbNames.isEmpty());
      db.close();
    }

    @Test
    public void fGetDbLoc() throws SQLException {
      Database db = Database.getInstance(DB_LOC);
      assertTrue("error in db field or getter dbLoc", DB_LOC.equals(db.getDbLoc()));
    }

}
