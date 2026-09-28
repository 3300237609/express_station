package com.example.station.mapper;

import com.example.station.entity.SysConfig;
import com.example.station.vo.ConfigVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置表 SysConfig 数据访问接口（表无逻辑删除列，config_key 全局唯一）
 */
@Mapper
public interface SysConfigMapper {

    /**
     * 查询全部配置（按配置键升序）
     * @return 配置列表
     */
    List<ConfigVO> selectList();

    /**
     * 主键查询配置
     * @param id 配置ID
     * @return 配置信息，不存在返回 null
     */
    ConfigVO selectById(@Param("id") Long id);

    /**
     * 按配置键查询配置值（业务逻辑读取用，含停用配置由调用方判断）
     * @param configKey 配置键
     * @return 配置值，不存在返回 null
     */
    String selectValueByKey(@Param("configKey") String configKey);

    /**
     * 按配置键查重（不过滤状态，与 uk_config_key 全局唯一键口径一致）
     * @param configKey 配置键
     * @param excludeId 排除的ID（新增传 null，修改传自身ID）
     * @return 匹配数量
     */
    int countByKey(@Param("configKey") String configKey, @Param("excludeId") Long excludeId);

    /**
     * 新增配置（回填主键）
     * @param config 配置实体
     * @return 影响行数
     */
    int insert(SysConfig config);

    /**
     * 修改配置（配置键/配置值/说明/状态全量更新）
     * @param config 配置实体（须含 id）
     * @return 影响行数
     */
    int update(SysConfig config);

    /**
     * 物理删除配置（表无逻辑删除列）
     * @param id 配置ID
     * @return 影响行数
     */
    int delete(@Param("id") Long id);
}
