package lamar.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lamar.database.nutrient.Catagory;
import lamar.database.nutrient.NutrientProvider;



public class Database implements AutoCloseable {

  private static final String PREFACE = "jdbc:sqlite:";
 
  private final String dbLoc;

  private Connection conn;
  private ObjectMapper mapper = new ObjectMapper();
  private Statement stmt;

  private Database(String dbLoc, Connection conn, Statement stmt) {
    this.dbLoc = dbLoc;
    this.conn = conn;
    this.stmt = stmt;
  }

  public static Database getInstance(String dbLoc) throws SQLException {
    Connection conn = DriverManager.getConnection(PREFACE + dbLoc);
    Statement stmt = conn.createStatement();
    return new Database(dbLoc, conn, stmt); 
  }

  public static Database createIfAbsent(String dbLoc) throws SQLException {
    Database db = getInstance(dbLoc);
    String create = "CREATE TABLE IF NOT EXISTS main ( type INTEGER NOT NULL, "
        + "name TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY (type, name) );";
    db.stmt.execute(create);
    return db;
  }

  public void add(NutrientProvider prov) throws SQLException, JsonProcessingException {
      String add = "INSERT INTO main ( type, name, value ) VALUES (\n" +
         "'" + prov.getType().p + "'" + ",\n" +
         "'" + prov.getName() + "'" + ",\n" + 
         "'" + mapper.writeValueAsString(prov) + "'" + ");"; 
      stmt.execute(add);
  }

  public void remove(NutrientProvider prov) throws SQLException {
      String remove = "DELETE FROM main WHERE type = " + prov.getType().p + " AND name = '" + 
          prov.getName() + "';";

      stmt.execute(remove);
  }

  public boolean contains(NutrientProvider prov) throws SQLException {
    String exists = "SELECT DISTINCT type, name FROM main  WHERE type = " + 
          prov.getType().p + " AND name = '" + prov.getName() + "';";

    ResultSet rs = stmt.executeQuery(exists);
    return rs.isClosed() ? false : true;
  }

  public NutrientProvider get(Catagory type, String name) throws SQLException, JsonMappingException, JsonProcessingException, IOException {
    String exists = "SELECT type, name, value FROM main  WHERE type = " + 
        type.p + " AND name = '" + name + "';";

    ResultSet rs = stmt.executeQuery(exists);

    if(rs.isClosed()) {
      throw new NoSuchElementException("no such entry: " + name);
    }

    NutrientProvider prov = (NutrientProvider)mapper.readValue((String)rs.getObject("value"), NutrientProvider.class);
    
    return prov;
  }

  @Override
  public void close() throws SQLException {
    conn.close();
  }

}
