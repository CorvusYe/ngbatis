package ye.weicheng.ngbatis.demo.pojo;

// Copyright (c) 2022 All project authors. All rights reserved.
//
// This source code is licensed under Apache 2.0 License.

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;
import ye.weicheng.ngbatis.demo.annotations.ValueType;

/**
 * <p>Person的实体类示例</p>
 * @author yeweicheng
 * @since 2022-06-10 22:10
 * <br>Now is history!
 */
@Table(name = "person")
public class Person {

  @Id
  private String name;

  private String gender;
  
  @ValueType(Double.class)
  private BigDecimal height;

  private Integer age;

  private Date birthday;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public Date getBirthday() {
    return birthday;
  }

  public void setBirthday(Date birthday) {
    this.birthday = birthday;
  }

  public BigDecimal getHeight() {
    return height;
  }

  public void setHeight(BigDecimal height) {
    this.height = height;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  @Override
  public String toString() {
    return "Person{"
      + "name='" + name
      + '\''
      + ", age="
      + age
      + '}';
  }
}
