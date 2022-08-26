package org.nebula.contrib.ngbatis.models;

// Copyright (c) 2022 All project authors. All rights reserved.
//
// This source code is licensed under Apache 2.0 License.
import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import java.util.HashMap;
import java.util.Map;

/**.
 * xml 中标签所声明的信息（方法）.
 *.
 * @author yeweicheng <br>.
 *     Now is history.
.*/
public class MapperContext {

  private static MapperContext instance;

  private MapperContext() { }

  public static MapperContext newInstance() {
    if (instance == null) {
      instance = new MapperContext();
    }
    return instance;
  }

  /** 当前应用中，在 xml 中 namespace 所声明的所有 XXXDao 及其 类模型.*/
  Map<String, ClassModel> interfaces;

  /**.
   * 标签名与实体的映射。（除了根据
   * {@link org.nebula.contrib.ngbatis.proxy.NebulaDaoBasic } 中
   * 泛型 T 所自动识别的外，还可以自行补充）.
  .*/
  final Map<String, Class<?>> tagTypeMapping = new HashMap<>();

  /** 基类所有的 nGQL模板.*/
  Map<String, String> daoBasicTpl;

  /** Nebula 连接池.*/
  NebulaPool nebulaPool;

  /** Nebula 连接配置.*/
  NebulaPoolConfig nebulaPoolConfig;

  boolean resourceRefresh = false;

  public Map<String, ClassModel> getInterfaces() {
    return interfaces;
  }

  public void setInterfaces(final Map<String, ClassModel> interfaces) {
    this.interfaces = interfaces;
  }

  public Map<String, String> getDaoBasicTpl() {
    return daoBasicTpl;
  }

  public void setDaoBasicTpl(final Map<String, String> daoBasicTpl) {
    this.daoBasicTpl = daoBasicTpl;
  }

  public NebulaPool getNebulaPool() {
    return nebulaPool;
  }

  public NebulaPoolConfig getNebulaPoolConfig() {
    return nebulaPoolConfig;
  }

  public void setNebulaPoolConfig(final NebulaPoolConfig nebulaPoolConfig) {
    this.nebulaPoolConfig = nebulaPoolConfig;
  }

  public void setNebulaPool(final NebulaPool nebulaPool) {
    this.nebulaPool = nebulaPool;
  }

  public boolean isResourceRefresh() {
    return resourceRefresh;
  }

  public void setResourceRefresh(final boolean resourceRefresh) {
    this.resourceRefresh = resourceRefresh;
  }

  public Map<String, Class<?>> getTagTypeMapping() {
    return tagTypeMapping;
  }
}
