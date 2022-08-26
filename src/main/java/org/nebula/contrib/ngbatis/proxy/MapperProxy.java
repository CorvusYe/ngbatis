package org.nebula.contrib.ngbatis.proxy;

// Copyright (c) 2022 All project authors. All rights reserved.
//
// This source code is licensed under Apache 2.0 License.
import static org.nebula.contrib.ngbatis.models.ClassModel.PROXY_SUFFIX;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.net.Session;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import org.nebula.contrib.ngbatis.ArgNameFormatter;
import org.nebula.contrib.ngbatis.Env;
import org.nebula.contrib.ngbatis.ResultResolver;
import org.nebula.contrib.ngbatis.annotations.UseKeyArgReplace;
import org.nebula.contrib.ngbatis.config.ParseCfgProps;
import org.nebula.contrib.ngbatis.exception.QueryException;
import org.nebula.contrib.ngbatis.models.ClassModel;
import org.nebula.contrib.ngbatis.models.MapperContext;
import org.nebula.contrib.ngbatis.models.MethodModel;
import org.nebula.contrib.ngbatis.session.LocalSession;
import org.nebula.contrib.ngbatis.utils.Page;
import org.nebula.contrib.ngbatis.utils.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**.
 * 被动态代理类所调用。用于实际的数据库访问并调用结果集处理方法.
 *.
 * @author yeweicheng <br>.
 *     Now is history.
.*/
public class MapperProxy {

  private static Logger log = LoggerFactory.getLogger(MapperProxy.class);
  @Autowired private ParseCfgProps props;

  public static Env env;

  private ClassModel classModel;

  private Map<String, MethodModel> methodCache = new HashMap<>();

  public MapperProxy(final ClassModel classModel) {
    this.classModel = classModel;
    methods(classModel);
  }

  private void methods(final ClassModel classModel) {
    methodCache.clear();
    Map<String, MethodModel> methods = classModel.getMethods();
    methodCache.putAll(methods);
  }

  /**.
   * <strong>框架中极其重要的方法，被动态代理类所执行。.
   * 是动态代理的入口方法{@link MapperProxyClassGenerator#method}</strong>.
   * 提供给代理类所调用.
   *.
   * @param className 访问数据库的接口.
   * @param methodName 执行数据库操作的方法名.
   * @param args 执行数据库操作的参数.
   * @return 结果对象映射的 java 对象.
  .*/
  public static Object invoke(
    final String className, final String methodName, final Object... args) {
      MapperContext mapperContext = env.getMapperContext();
      String proxyClassName = className + PROXY_SUFFIX;
      ClassModel classModel = mapperContext.getInterfaces().get(proxyClassName);
      Method method = null;
      if (mapperContext.isResourceRefresh()) {
        try {
          Map<String, ClassModel> classModelMap =
            classModel.getResourceLoader().parseClassModel(
              classModel.getResource());
          classModel = classModelMap.get(proxyClassName);
          method = classModel.getMethod(methodName).getMethod();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        method = classModel.getMethod(methodName).getMethod();
      }
      return pageSupport(classModel, method, args);
    }

  /**.
   * 自动对该分页的接口进行分页操作<br>.
   * 该分页：接口参数中有 {@link Page Page} 对象.
   *
   * @param classModel 应用初始化后，数据访问接口对应的类模型.
   * @param method 执行数据库操作的方法.
   * @param args 执行数据库操作的参数.
   * @return 结果对象映射的 java 对象.
  .*/
  private static Object pageSupport(
      final ClassModel classModel, final Method method, final Object[] args) {
    int pageParamIndex = ReflectUtil.containsType(method, Page.class);

    MapperProxy mapperProxy = new MapperProxy(classModel);
    if (pageParamIndex < 0) {
      return mapperProxy.invoke(classModel, method, args);
    }

    String countMethodName = method.getName() + "$Count";
    String pageMethodName = method.getName() + "$Page";

    Long count =
        (Long) mapperProxy.invoke(classModel, classModel.getMethods().get(
          countMethodName), args);
    List rows =
        (List) mapperProxy.invoke(classModel, classModel.getMethods().get(
          pageMethodName), args);

    Page page = (Page) args[pageParamIndex];
    page.setTotal(count);
    page.setRows(rows);
    return rows;
  }

  public static Object invoke(
    final MethodModel methodModel, final  Object... args) {
      return invoke(null, methodModel, args);
    }

  /**
   * 提供给基类所调用，完整描述整个 orm 流程的核心方法.
   *
   * <ol>
   *   <li>获取方法具体信息，主要包括返回值类型与查询脚本(nGQL)
   *   <li>对上一步获取到的 nGQL 中，参数占位符替换成实际参数值
   *   <li>执行数据库访问
   *   <li>按返回值类型获取对应结果集处理器
   *   <li>完成数据库数据类型向 javaa 对象类型的转化
   * </ol>
   *
   * @param classModel mapper 接口类型，存放 mapper 标签的属性
   * @param methodModel 接口方法模型，存放了 dao接口的详细信息（nGQL模板、返回值类型等）
   * @param args 执行 nGQL 的参数
   * @return 结果值
   */
  public static Object invoke(
    final ClassModel classModel, final MethodModel methodModel,
    final Object... args) {
      Method method = methodModel.getMethod();
      ResultSet query = null;
      // 参数格式转换
      long step0 = System.currentTimeMillis();
      Map<String, Object> argMap = env.getArgsResolver().resolve(
        methodModel, args);
      Map<String, Object> paramWithSchema =
          new LinkedHashMap<String, Object>(argMap) {
            {
              put("ng_cm", classModel);
              put("ng_mm", methodModel);
              put("ng_args", args);
            }
          };
      // beetl 渲染模板
      String textTpl = methodModel.getText();
      String nGQL = env.getTextResolver().resolve(textTpl, paramWithSchema);
      Map<String, Object> params = null;
      if (method != null && method.isAnnotationPresent(
          UseKeyArgReplace.class)) {
        ArgNameFormatter.CqlAndArgs format = env.getArgNameFormatter().format(
          nGQL, argMap);
        nGQL = format.getCql();
        params = format.getArgs();
      } else {
        params = argMap;
      }

      long step1 = System.currentTimeMillis();
      query = executeWithParameter(classModel, methodModel, nGQL, params);

      long step2 = System.currentTimeMillis();
      if (!query.isSucceeded()) {
        throw new QueryException("数据查询失败：" + query.getErrorMessage());
      }

      if (methodModel.getResultType() == ResultSet.class) {
        return query;
      }

      ResultResolver resultResolver = env.getResultResolver();
      Object resolve = resultResolver.resolve(methodModel, query);
      long step3 = System.currentTimeMillis();

      log.debug(
          "nGQL construction in {}ms, query in {}ms, result handle in {}ms ",
          step1 - step0,
          step2 - step1,
          step3 - step2);
      return resolve;
    }

  public Object invoke(final Method method, final Object... args) {
    return invoke(null, method, args);
  }

  public Object invoke(
    final ClassModel classModel, final Method method, final Object... args) {
      MethodModel methodModel = methodCache.get(method.getName());
      methodModel.setMethod(method);

      return invoke(classModel, methodModel, args);
    }

  /**.
   * 通过 nebula-graph 客户端执行数据库访问。被 invoke 所调用，间接为动态代理类服务.
   *.
   * @param nGQL 待执行的查询脚本（模板）.
   * @param params 待执行脚本的参数所需的参数.
   * @return nebula-graph 的未被 orm 操作的原始结果集.
  .*/
  public static ResultSet executeWithParameter(
    final ClassModel cm, final MethodModel mm, final String nGQL,
    final Map<String, Object> params) {
      LocalSession localSession = null;
      Session session = null;
      ResultSet result = null;
      String proxyClass = null;
      String proxyMethod = null;
      try {
        if (log.isDebugEnabled()) {
          StackTraceElement stackTraceElement = Thread.currentThread(
            ).getStackTrace()[6];
          proxyClass = stackTraceElement.getClassName();
          proxyMethod = stackTraceElement.getMethodName();
        }

        localSession = env.getDispatcher().poll();
        nGQL = qlWithSpace(localSession, nGQL, getSpace(cm, mm));
        session = localSession.getSession();
        result = session.executeWithParameter(nGQL, params);
        if (result.isSucceeded()) {
          return result;
        } else {
          throw new QueryException(" 数据查询失败" + result.getErrorMessage());
        }
      } catch (Exception e) {
        throw new QueryException("数据查询失败：" + e.getMessage());
      } finally {
        log.debug(
          "\n\t- proxyMethod:{}#{}\n\t- nGQL{}\n\t- params:{}\n\t- result:{}",
          proxyClass,
          proxyMethod,
          nGQL,
          params,
          result);
        if (localSession != null) {
          env.getDispatcher().offer(localSession);
        }
      }
    }

  private static String qlWithSpace(
    final LocalSession localSession, final String nGQL,
    final String currentSpace) {
      String trimedQueryString = nGQL.trim();
      String sessionSpace = localSession.getCurrentSpace();
      if (Objects.equals(sessionSpace, currentSpace)) {
        return String.format("\n\t\t%s", trimedQueryString);
      }
      localSession.setCurrentSpace(currentSpace);
      return String.format(
        "USE %s;\n\t\t%s", currentSpace, trimedQueryString);
    }

  public static String getSpace(
    final ClassModel cm, final MethodModel mm) {
      return mm != null && mm.getSpace() != null
        ? mm.getSpace()
        : cm != null && cm.getSpace() != null ? cm.getSpace() : env.getSpace();
    }

  public static Logger getLog() {
    return log;
  }

  public static void setLog(final Logger log) {
    MapperProxy.log = log;
  }

  public ParseCfgProps getProps() {
    return props;
  }

  public void setProps(final ParseCfgProps props) {
    this.props = props;
  }

  public ClassModel getClassModel() {
    return classModel;
  }

  public void setClassModel(final ClassModel classModel) {
    this.classModel = classModel;
  }

  public Map<String, MethodModel> getMethodCache() {
    return methodCache;
  }

  public void setMethodCache(final Map<String, MethodModel> methodCache) {
    this.methodCache = methodCache;
  }
}
