package org.nebula.contrib.ngbatis.handler;

// Copyright (c) 2022 All project authors. All rights reserved.
//
// This source code is licensed under Apache 2.0 License.
import static org.nebula.contrib.ngbatis.utils.ReflectUtil.isBasicType;

import com.vesoft.nebula.client.graph.data.ResultSet;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.nebula.contrib.ngbatis.ResultHandler;
import org.nebula.contrib.ngbatis.exception.QueryException;
import org.nebula.contrib.ngbatis.exception.ResultHandleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**.
 * 结果集处理器的基类，主要作用有：.
 *.
 * <ul>.
 *   <li>用于做基本的类型校验.
 *   <li>调用不同数据类型下的结果集处理器.
 * </ul>.
 *.
 * @author yeweicheng <br>.
 *     Now is history.
.*/
public abstract class AbstractResultHandler<T, Z>
    implements ResultHandler<T, Z> {

  private Logger log = LoggerFactory.getLogger(AbstractResultHandler.class);

  protected boolean isReturnTypeInterface(final Class returnType) {
    return returnType.isInterface();
  }

  /**.
   * 进入按不同数据类型处理前的基本处理。包括 期望得到原始结果集、期望单行却返回多行、期望单列却返回多列.
   *.
   * @param returnType 接口的返回值类型.
   * @param result 实际查得的 Nebula结果集.
   * @param resultType xml 中声明的返回值类型。通常用于集合泛型.
   * @return ORM 之后的 java对象结果.
  .*/
  public T handle(
      final Class returnType, final ResultSet result,
      final Class resultType) {
    if (returnType == ResultSet.class) {
      return (T) result;
    }
    if (!result.isSucceeded()) {
      throw new QueryException(result.getErrorMessage());
    }

    T newResult = (T) newInstance(returnType, resultType);

    if (!(newResult instanceof Collection) && result.rowsSize() > 1) {
      throw new ResultHandleException("返回值要求只有一个值，但却出现了一行以上记录。");
    }

    List<String> columnNames = result.getColumnNames();
    if (isBasicType(returnType) && columnNames.size() > 1) {
      throw new ResultHandleException("接口返回类型为基本类型，结果集却存在多个列。" + columnNames);
    }

    try {
      return handle(newResult, result, resultType);
    } catch (Exception e) {
      throw new ResultHandleException(e);
    }
  }

  /**.
   * 根据返回值类型创建的 返回值容器。用于放置转换后的值.
   *.
   * @param returnType 接口的返回值类型.
   * @param resultType xml 中声明的返回值类型。通常用于集合泛型.
   * @return 与预期返回值相符的空属性对象。.
  .*/
  protected T newInstance(final Class<T> returnType, final Class resultType) {
    if (isReturnTypeInterface(returnType)) {
      return this.defaultInstance(returnType);
    } else {
      try {
        Object t = resultType.newInstance();
        return (T) t;
      } catch (InstantiationException | IllegalAccessException e) {
        log.error("泛型第二个参数" + resultType.getName() + "的类型，不支持实例化。");
        return null;
      }
    }
  }

  /**.
   * 为不同集合类型创建 0元素对象.
   *.
   * @param returnType
   * @return
  .*/
  protected T defaultInstance(final Class returnType) {
    Object o =
        returnType == List.class
            ? new ArrayList()
            : returnType == Set.class
                ? new HashSet()
                : returnType == Queue.class
                    ? new ConcurrentLinkedQueue()
                    : returnType == Map.class ? new HashMap<>() : null;
    return (T) o;
  }

  /** 不同数据类型的结果处理类创建时，需要将自身所处理的类型注册到全局变量中..*/
  public AbstractResultHandler() {
    Type[] typeParameters =
        ((ParameterizedType)
          this.getClass().getGenericSuperclass()).getActualTypeArguments();
    if (typeParameters != null && typeParameters.length == 2) {
      try {
        addHandler(
            Class.forName(typeParameters[0].getTypeName()),
            Class.forName(typeParameters[1].getTypeName()));
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  /**.
   * 注册类型处理类.
   *.
   * @param returnType 接口的返回值类型.
   * @param resultType xml 中声明的返回值类型。通常用于集合泛型.
  .*/
  void addHandler(final Class returnType, final Class resultType) {
    ResultHandler bean = this;
    DIFF_TYPE_RESULT_HANDLER.put(returnType, resultType, bean);
    HANDLERS.putIfAbsent(returnType, bean);
  }

  protected String getLastName(final String name) {
    String[] split = name.split("\\.");
    if (split.length == 1) {
      return split[0];
    } else {
      return split[split.length - 1];
    }
  }

  protected String getLastLabel(final String[] labels) {
    if (labels != null) {
      int length = labels.length;
      if (length > 0) {
        return labels[length - 1];
      }
    }
    return null;
  }
}
