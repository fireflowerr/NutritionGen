package lamar.database;

import java.util.HashSet;

public class ValueSet <T> extends HashSet<T> { 

  @Override
  public boolean contains(Object o) {
    for(T t : this) {
      if(t.equals(o))
        return true;
    }
    return false;
  }

  @Override
  public boolean add(T e) {
    return contains(e) ? false : super.add(e); 
  }
}
