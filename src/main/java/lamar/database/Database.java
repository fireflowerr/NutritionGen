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

  /**
   * @param dbLoc The path to the DB
   * @return      DB instance
   */
  public static Database getInstance(String dbLoc) throws SQLException {
    Connection conn = DriverManager.getConnection(PREFACE + dbLoc);
    Statement stmt = conn.createStatement();
    return new Database(dbLoc, conn, stmt); 
  }

  /**
   * @param dbLoc The path to the DB
   * @return      A concreate instance of this DB
   */
  public static Database createIfAbsent(String dbLoc) throws SQLException {
    Database db = getInstance(dbLoc);
    String create = "CREATE TABLE IF NOT EXISTS main ( type INTEGER NOT NULL, "
        + "name TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY (type, name) );";
    db.stmt.execute(create);
    return db;
  }

  /**
   * @param prov  The NutrientProvider to add to DB 
   */
  public void add(NutrientProvider prov) throws SQLException, JsonProcessingException {
      String add = "INSERT INTO main ( type, name, value ) VALUES (\n" +
         "'" + prov.getType().p + "'" + ",\n" +
         "'" + prov.getName() + "'" + ",\n" + 
         "'" + mapper.writeValueAsString(prov) + "'" + ");"; 
      stmt.execute(add);
  }

  /**
   *  @param  provs NutrientProviders to add to DB
   */
  public void addAll(NutrientProvider... provs) throws SQLException, JsonProcessingException {
    for(NutrientProvider ntr : provs) {
      add(ntr);
    }
  }

  /**
   * Removes a specified NutrientProvider from this DB.
   *  @param  type  The type of the NutrientProvider 
   *  @param  name  The name of the NutrientProvider
   */
  public void remove(Catagory type, String name) throws SQLException, IOException {
      for(NutrientProvider prov : getDependents(type, name)) {
        remove(prov);
      }

      String remove = "DELETE FROM main WHERE type = " + type.p + " AND name = '" + 
          name + "';";
      stmt.execute(remove);
  }

  /**
   * @param prov to remove
   */
  public void remove(NutrientProvider prov) throws SQLException, IOException {
    remove(prov.getType(), prov.getName());
  }

  /**
   * Returns a list of dependent NutrientProviders
   * @param type  The type of the NutrientProvider
   * @param name  The name of the NutrientProvider
   * @return The set of NutrientProviders that depend on the specified NutrientProvider
   */
  public ValueSet<NutrientProvider> getDependents(Catagory type, String name) throws IOException, SQLException {
    ValueSet<NutrientProvider> canidates = new ValueSet<>();
    Catagory[] types = Catagory.values();

    if(type.p == 0) { 
      return canidates;
    } 

    int[] range = IntStream.range(0, type.p).toArray();
    for(int i : range) {
      canidates.addAll(getType(types[i]));
    }
   
   return getDependents(type, name, new ValueSet<NutrientProvider>(), canidates); 
  }

  private ValueSet<NutrientProvider> getDependents(Catagory type, String name,
      ValueSet<NutrientProvider> oldDependents, ValueSet<NutrientProvider> canidates) throws 
      SQLException, IOException {

    ValueSet<NutrientProvider> dependents = canidates.stream()
        .filter(x -> isDependent(type, name, x))
        .filter(x -> !oldDependents.contains(x))
        .collect(Collectors.toCollection(ValueSet::new));
    
    if(dependents.isEmpty()) {
      return oldDependents;
    }

    oldDependents.addAll(dependents);
    for(NutrientProvider dep : dependents) {
      getDependents(dep.getType(), dep.getName(), oldDependents, canidates);
    }
    return oldDependents;
  }

  private boolean isDependent(Catagory type, String name, NutrientProvider prov) {
    HashMap<String, Pair<Catagory, Double>> constituent = prov.getConstituent();

    for(String ingr : constituent.keySet()) {
      Catagory pType = constituent.get(ingr).getValue0();

      if(pType == type && ingr.equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return true if the provided NutrientProvider is contained in this DB
   */
  public boolean contains(NutrientProvider prov) throws SQLException {
    return contains(prov.getType(), prov.getName());
  }

  /**
   * @return true if the provided NutrientProvider is contained in this DB
   */
  public boolean contains(Catagory type, String name) throws SQLException {
    String exists = "SELECT DISTINCT type, name FROM main WHERE type = " + 
         type.p + " AND name = '" + name + "';";

    ResultSet rs = stmt.executeQuery(exists);
    return rs.isClosed() ? false : true;
  }

  /**
   * @param type  The type of the NutrientProvider
   * @param name  The name of the NutrientProvider
   * @return  NutrientProvider matching the input criteria
   */
  public NutrientProvider get(Catagory type, String name) throws SQLException, JsonMappingException, JsonProcessingException, IOException {
    String exists = "SELECT type, name, value FROM main WHERE type = " + 
        type.p + " AND name = '" + name + "';";

    ResultSet rs = stmt.executeQuery(exists);

    if(rs.isClosed()) {
      throw new SQLException("no such entry: " + name);
    }

    NutrientProvider prov = (NutrientProvider)mapper.readValue((String)rs.getObject("value"), NutrientProvider.class);
    
    return prov;
  }

  /**
   *  Fetch DB column by ID
   *  @param id Column id
   *  @return String list of column entries
   */
  public List<String> getColumn(String id) throws SQLException {
    String get = "SELECT " + id + " FROM main";
    ResultSet rs = stmt.executeQuery(get);
    ArrayList<String> toRet = new ArrayList<>();

    while(rs.next()) {
      toRet.add(rs.getString(id));
    }
    return toRet;
  }

  /**
   * Get all NutrientProviders of a certain Catagory
   * @param type  The category for lookup
   * @return      List of matching NutrientProvider    
   */
  public List<NutrientProvider> getType(Catagory type) throws SQLException, IOException {
    String get = "SELECT value FROM main WHERE type = " + type.p;
    ResultSet rs = stmt.executeQuery(get);
    ArrayList<NutrientProvider> ntr = new ArrayList<>();
     
    while(rs.next()) {
      ntr.add(mapper.readValue(rs.getString("value"), NutrientProvider.class));
    }
    return ntr;
  }

  /**
   * Close access to this DB resource
   */
  @Override
  public void close() throws SQLException {
    conn.close();
  }

}
