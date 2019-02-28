package lamar.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lamar.database.nutrient.Catagory;
import lamar.database.nutrient.NutrientProvider;



public class Database implements AutoCloseable {

  public static enum Column { TYPE, NAME };

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

  /**
 * @return the dbLoc
 */
public String getDbLoc() {
	return dbLoc;
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

  public void remove(Catagory type, String name) throws SQLException, IOException {
      for(NutrientProvider prov : getDependents(type, name)) {
        remove(prov);
      }

      String remove = "DELETE FROM main WHERE type = " + type.p + " AND name = '" + 
          name + "';";
      stmt.execute(remove);
  }

  public void remove(NutrientProvider prov) throws SQLException, IOException {
    remove(prov.getType(), prov.getName());
  }

  public List<NutrientProvider> getDependents(Catagory type, String name) throws SQLException, IOException {
    ArrayList<NutrientProvider> dependents = new ArrayList<>();
    Catagory[] types = Catagory.values();
    if(type == types[0]) {
      return dependents;
    }

    int[] range = IntStream.range(type.p + 1 - 2, types.length).toArray();
    for(int i : range) {
      dependents.addAll(getType(types[i]));
    }

    return dependents.stream()
        .filter(x -> isDependent(type, name, x))
        .collect(Collectors.toList());
  }

  public boolean isDependent(Catagory type, String name, NutrientProvider prov) {
    HashMap<String, Pair<Catagory, Double>> constituent = prov.getConstituent();

    for(String ingr : constituent.keySet()) {
      Catagory pType = constituent.get(ingr).getValue0();

      if(pType == type && ingr.equals(name)) {
        return true;
      }
    }
    return false;
  }

  public boolean contains(NutrientProvider prov) throws SQLException {
    String exists = "SELECT DISTINCT type, name FROM main  WHERE type = " + 
          prov.getType().p + " AND name = '" + prov.getName() + "';";

    ResultSet rs = stmt.executeQuery(exists);
    return rs.isClosed() ? false : true;
  }

  public boolean contains(Catagory type, String name) throws SQLException {
    String exists = "SELECT DISTINCT type, name FROM main WHERE type = " + 
         type.p + " AND name = '" + name + "';";

    ResultSet rs = stmt.executeQuery(exists);
    return rs.isClosed() ? false : true;
  }

  public NutrientProvider get(Catagory type, String name) throws SQLException, JsonMappingException, JsonProcessingException, IOException {
    String exists = "SELECT type, name, value FROM main WHERE type = " + 
        type.p + " AND name = '" + name + "';";

    ResultSet rs = stmt.executeQuery(exists);

    if(rs.isClosed()) {
      throw new NoSuchElementException("no such entry: " + name);
    }

    NutrientProvider prov = (NutrientProvider)mapper.readValue((String)rs.getObject("value"), NutrientProvider.class);
    
    return prov;
  }

  public List<String> getColumn(String id) throws SQLException {
    String get = "SELECT " + id + " FROM main";
    ResultSet rs = stmt.executeQuery(get);
    ArrayList<String> toRet = new ArrayList<>();

    while(rs.next()) {
      toRet.add(rs.getString(id));
    }
    return toRet;
  }

  public List<NutrientProvider> getType(Catagory type) throws SQLException, IOException {
    String get = "SELECT value FROM main WHERE type = " + type.p;
    ResultSet rs = stmt.executeQuery(get);
    ArrayList<NutrientProvider> ntr = new ArrayList<>();
     
    while(rs.next()) {
      ntr.add(mapper.readValue(rs.getString("value"), NutrientProvider.class));
    }
    return ntr;
  }

  @Override
  public void close() throws SQLException {
    conn.close();
  }

}
