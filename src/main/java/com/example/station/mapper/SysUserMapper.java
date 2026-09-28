package com.example.station.mapper;

import com.example.station.entity.SysUser;
import com.example.station.vo.CourierVO;
import com.example.station.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户表 sys_user 数据访问接口
 * 用户角色（sys_user_role）关联的删除在此一并维护，保证用户与角色绑定同事务处理
 */
@Mapper
public interface SysUserMapper {

    /**
     * 按用户名查询未删除的用户
     * @param username 用户名
     * @return 用户信息，不存在返回 null
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 分页查询用户列表（配合 PageHelper，自动按相同条件生成 count；不含快递员角色）
     * @param kw 用户名/姓名/手机号关键字，可空
     * @param status 状态（1 正常 0 禁用），可空
     * @param excludeRoleCode 排除的角色编码（用户列表排除 COURIER），可空
     * @return 用户列表（含角色编码 role）
     */
    List<UserVO> selectList(@Param("kw") String kw, @Param("status") Integer status,
                            @Param("excludeRoleCode") String excludeRoleCode);

    /**
     * 分页查询快递员列表（配合 PageHelper，自动按相同条件生成 count；仅含指定角色）
     * @param kw 用户名/姓名/手机号关键字，可空
     * @param status 状态（1 正常 0 禁用），可空
     * @param roleCode 角色编码（快递员列表传 COURIER），可空
     * @return 快递员列表
     */
    List<CourierVO> selectCourierList(@Param("kw") String kw, @Param("status") Integer status,
                                      @Param("roleCode") String roleCode);

    /**
     * 按 ID 查询用户详情（未删除，含角色编码，不含 password）
     * @param id 用户ID
     * @return 用户详情，不存在返回 null
     */
    UserVO selectById(@Param("id") Long id);

    /**
     * 按手机号精确匹配未删除的用户（uk_phone 唯一至多一行，入库选择收件人联动用）
     * @param phone 手机号
     * @return 用户简要信息（不含 password），不存在返回 null
     */
    UserVO selectByPhone(@Param("phone") String phone);

    /**
     * 统计指定用户名的用户数量（注册/新增查重）
     * 不过滤已删除用户，与 uk_username 唯一键口径一致（已删除用户仍占用用户名）
     * @param username 用户名
     * @return 用户数量
     */
    Long countByUsername(@Param("username") String username);

    /**
     * 统计有效用户数量（关联校验用，非唯一键查重，过滤已删除）
     * @param id 用户ID
     * @return 匹配数量
     */
    Long countById(@Param("id") Long id);

    /**
     * 统计指定手机号的未删除用户数量（注册/新增/修改查重）
     * @param phone 手机号
     * @param excludeId 排除的用户ID（修改时排除自身），新增/注册传 null
     * @return 用户数量
     */
    Long countByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    /**
     * 新增用户（回填自增主键 id）
     * @param user 用户信息
     * @return 影响行数
     */
    int insert(SysUser user);

    /**
     * 修改用户可编辑字段（用户名/密码/状态由专用接口维护）
     * @param user 用户信息（须含 id）
     * @return 影响行数
     */
    int update(SysUser user);

    /**
     * 启用/禁用用户账号
     * @param id 用户ID
     * @param status 1 正常，0 禁用
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("updateTime") LocalDateTime updateTime);

    /**
     * 重置用户密码（管理员重置为默认密码，传入 BCrypt 密文）
     * @param id 用户ID
     * @param password 加密后的密码
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int resetPassword(@Param("id") Long id, @Param("password") String password,
                      @Param("updateTime") LocalDateTime updateTime);

    /**
     * 物理删除用户
     * （用户名/手机号有唯一键，逻辑删除会导致同名账号无法再注册，故物理删除并同步清角色绑定）
     * @param id 用户ID
     * @return 影响行数
     */
    int delete(@Param("id") Long id);

    /**
     * 查询有效用户ID列表（未删除且状态正常，系统公告广播用）
     * @return 用户ID列表
     */
    List<Long> selectActiveIds();

    /**
     * 查询指定角色的有效用户ID列表（未删除且状态正常，角色定向通知用）
     * @param roleCode 角色编码（如 ADMIN）
     * @return 用户ID列表
     */
    List<Long> selectActiveIdsByRole(@Param("roleCode") String roleCode);
}

