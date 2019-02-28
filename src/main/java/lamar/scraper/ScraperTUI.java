package lamar.scraper;


public class ScraperTUI {

  public static void main(String[] args) {
    try {
      //UsdaScraper.getSearchResults("milk");
      System.out.println(UsdaScraper.getIngredient(45350876).label());

    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
