package ye.weicheng.ngbatis.demo.repository;

import org.springframework.data.repository.query.Param;
/**
 * nGQL片段跨mapper引用测试
 * 2023-9-7 12:25 lyw.
 */

public interface NgqlInclude4diffMapperDao {
  Integer testInclude(@Param("myInt") Integer myInt);
}
