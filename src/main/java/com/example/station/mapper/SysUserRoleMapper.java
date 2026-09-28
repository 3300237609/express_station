package com.example.station.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联表 sys_user_role 数据访问接口
 */
@Mapper
public interface SysUserRoleMapper {

    /**
     * 查询指定用户的角色编码列表
     * @param userId 用户ID
     * @return 角色编码列表（如 ADMIN、USER），无角色返回空列表
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 新增用户角色关联
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 影响行数
     */
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 删除指定用户的全部角色关联（删除用户、变更角色时调用）
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);
}
