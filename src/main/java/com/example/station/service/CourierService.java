package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.CourierDTO;
import com.example.station.vo.CourierVO;

import java.util.List;

/**
 * 快递员管理业务层接口：列表、新增、修改、删除（启用禁用/重置密码复用用户接口）
 */
public interface CourierService {

    /**
     * 分页查询快递员列表（管理员，仅含 COURIER 角色账号）
     * @param kw 用户名/姓名/手机号关键字，可空
     * @param status 状态（1 正常 0 禁用），可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 快递员分页列表（total / pageNum / pageSize / pages 在 map 中）
     */
    R<List<CourierVO>> list(String kw, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 新增快递员（管理员，默认正常并固定分配 COURIER 角色）
     * @param dto 新增入参
     * @return 操作结果
     */
    R<String> add(CourierDTO dto);

    /**
     * 修改快递员（管理员，用户名不可改，角色保持 COURIER）
     * @param dto 修改入参（id 必填）
     * @return 操作结果
     */
    R<String> update(CourierDTO dto);

    /**
     * 删除快递员（管理员，物理删除并同步清空角色绑定，不允许删除自己）
     * @param id 快递员ID
     * @return 操作结果
     */
    R<String> delete(Long id);

    /**
     * 分页查询快递员投递记录（操作日志：模块=快递管理且操作人=该用户，按操作时间倒序）
     * @param id 快递员ID
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页操作日志列表，附 total 总条数
     */
    R deliveryList(Long id, Integer pageNum, Integer pageSize);

    /**
     * 分页查询快递员入库记录（操作日志：模块=快递入库且操作人=该用户，按操作时间倒序）
     * @param id 快递员ID
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页操作日志列表，附 total 总条数
     */
    R storageList(Long id, Integer pageNum, Integer pageSize);

    /**
     * 分页查询快递员登记的异常件记录（异常记录：登记人=该用户，按登记时间倒序）
     * @param id 快递员ID
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页异常件列表，附 total 总条数
     */
    R exceptionList(Long id, Integer pageNum, Integer pageSize);
}
