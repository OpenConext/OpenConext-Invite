package access.lifecycle;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@EqualsAndHashCode
@ToString
public class LifeCycleResult {

  private String status = "OK";
  private String name = "OpenConext-access-server";
  private List<Attribute> data = new ArrayList<>();

  public void setData(List<Attribute> data) {
    this.data = data;
  }
}
