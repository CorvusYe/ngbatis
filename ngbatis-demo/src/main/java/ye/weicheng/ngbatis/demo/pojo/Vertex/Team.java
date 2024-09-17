package ye.weicheng.ngbatis.demo.pojo.Vertex;

// Copyright (c) 2022 All project authors. All rights reserved.
//
// This source code is licensed under Apache 2.0 License.

import org.nebula.contrib.ngbatis.annotations.base.GraphId;
import org.nebula.contrib.ngbatis.enums.IdType;
import org.nebula.contrib.ngbatis.annotations.base.Tag;
import org.nebula.contrib.ngbatis.base.GraphBaseVertex;

/**
 * Team点实体示例
 * @author xYLiuuuuuu
 * @since 2024/9/11
 */

@Tag(name = "team")
public class Team extends GraphBaseVertex {

  @GraphId(type = IdType.STRING)
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Team{" +
            "name='" + name + '\'' +
            '}';
  }
}
