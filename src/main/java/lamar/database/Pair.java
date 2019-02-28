package lamar.database;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.ANY
)
public class Pair <T1, T2> {
 private final T1 value0; 
 private final T2 value1;

  @JsonCreator
  public Pair(@JsonProperty("value0") T1 value0, @JsonProperty("value1") T2 value1) {
    this.value0 = value0;
    this.value1 = value1;
  }

  public T1 getValue0() {
    return value0;
  }

  public T2 getValue1() {
    return value1;
  }

  @Override
  public String toString() {
    return "(" + value0 + ", " + value1 + ")";
  }

  @Override
  public boolean equals(Object o) {
    if(!(o.getClass() == Pair.class)) {
      return false;
    }

    @SuppressWarnings("rawtypes")
    Pair remote = (Pair)o;

    return value0.equals(remote.getValue0()) && value1.equals(remote.getValue1()); 
  }
}
