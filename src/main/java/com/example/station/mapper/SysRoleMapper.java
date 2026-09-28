package com.example.station.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色表 sys_role 数据访问接口
 */
@Mapper
public interface SysRoleMapper {

    /**
     * 按角色编码查询角色ID
     * @param code 角色编码（ADMIN / COURIER / USER）
     * @return 角色ID，不存在返回 null
     */
    Long selectIdByCode(@Param("code") String code);
}
