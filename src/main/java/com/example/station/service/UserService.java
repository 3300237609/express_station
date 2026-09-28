package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.UserDTO;
import com.example.station.vo.UserVO;

import java.util.List;

/**
 * 用户管理业务层接口：列表、详情、新增、修改、删除、启用禁用、重置密码
 */
public interface UserService {

    /**
     * 分页查询用户列表（管理员，不含快递员角色）
     * @param kw 用户名/姓名/手机号关键字，可空
     * @param status 状态（1 正常 0 禁用），可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 用户分页列表（total / pageNum / pageSize / pages 在 map 中）
     */
    R<List<UserVO>> list(String kw, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 查询用户详情（管理员）
     * @param id 用户ID
     * @return 用户详情
     */
    R<UserVO> detail(Long id);

    /**
     * 新增用户（管理员，默认正常并分配角色，未传角色默认 USER）
     * @param dto 新增入参
     * @return 操作结果
     */
    R<String> add(UserDTO dto);

    /**
     * 修改用户（管理员，用户名不可改，密码/状态走专用接口）
     * @param dto 修改入参（id 必填，未传角色则不变更角色）
     * @return 操作结果
     */
    R<String> update(UserDTO dto);

    /**
     * 删除用户（管理员，物理删除并同步清空角色绑定，不允许删除自己）
     * @param id 用户ID
     * @return 操作结果
     */
    R<String> delete(Long id);

    /**
     * 启用/禁用用户（管理员，不允许禁用自己）
     * @param id 用户ID
     * @param status 1 正常，0 禁用
     * @return 操作结果
     */
    R<String> updateStatus(Long id, Integer status);

    /**
     * 重置用户密码（管理员，重置为默认密码）
     * @param id 用户ID
     * @return 操作结果（含默认密码提示）
     */
    R<String> resetPassword(Long id);

    /**
     * 分页查询用户名下快递（收件用户或收件手机号命中，供管理员查看用户收件记录）
     * @param id 用户ID
     * @param keyword 快递单号关键字，可空
     * @param status 快递状态，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页快递列表，附 total 总条数
     */
    R expressList(Long id, String keyword, String status, Integer pageNum, Integer pageSize);

    /**
     * 分页查询用户取件记录（按取件人过滤，JOIN 快递单号与操作人，按取件时间倒序）
     * @param id 用户ID
     * @param keyword 快递单号关键字，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页取件记录列表，附 total 总条数
     */
    R pickupRecordList(Long id, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 按手机号匹配系统用户（快递入库选择收件人联动，管理员/快递员可用）
     * @param phone 手机号（11 位精确匹配，自动裁剪前后空格）
     * @return 用户简要信息（id/username/realName/phone），未匹配 data 为 null（收件人可能非系统用户，属正常分支）
     */
    R<UserVO> matchByPhone(String phone);
}
