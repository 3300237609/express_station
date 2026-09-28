package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.ExpressDTO;
import com.example.station.dto.UpdateLocationDTO;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * 快递信息管理业务接口
 */
@Validated
public interface ExpressService {

    /**
     * 条件分页查询快递列表（单号/取件码/收件人/电话/公司/状态/到达时间范围）
     * @param keyword 快递单号关键字，可空
     * @param pickupCode 取件码（模糊），可空
     * @param receiverName 收件人（模糊），可空
     * @param receiverPhone 收件人电话（模糊），可空
     * @param companyId 快递公司ID，可空
     * @param status 快递状态，可空
     * @param arrivalTimeStart 到达时间起（含当日），可空
     * @param arrivalTimeEnd 到达时间止（含当日），可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页快递列表，附 total 总条数
     */
    R list(String keyword, String pickupCode, String receiverName, String receiverPhone,
           Long companyId, String status, LocalDate arrivalTimeStart, LocalDate arrivalTimeEnd,
           Integer pageNum, Integer pageSize);

    /**
     * 查询快递详情（含公司名与货位编号）
     * @param id 快递ID
     * @return 快递详情
     */
    R detail(Long id);

    /**
     * 查询快递流转记录（创建/到达/入库/异常/取件时间线，按时间正序）
     * @param id 快递ID
     * @return 流转节点列表
     */
    R flow(Long id);

    /**
     * 新增快递（系统自动生成 6 位取件码，初始状态待入库 WAITING）
     * @param dto 快递入参
     * @return 新增结果
     */
    R add(@Validated ExpressDTO dto);

    /**
     * 修改快递基础信息（不改取件码、状态、货位与业务时间戳）
     * @param dto 快递入参（id 必填）
     * @return 修改结果
     */
    R update(@Validated ExpressDTO dto);

    /**
     * 删除快递（占用货位/有取件记录/有异常登记时禁止删除）
     * @param id 快递ID
     * @return 删除结果
     */
    R delete(Long id);

    /**
     * 修改快递状态（管理员修正：枚举校验；仍关联货位时禁止置为已取件/已退回）
     * @param id 快递ID
     * @param status 目标状态
     * @return 状态修改结果
     */
    R updateStatus(Long id, String status);

    /**
     * 调整快递货位（管理员：已上架快递换位；占用目标货位、释放原货位、更新关联并通知收件用户）
     * 守卫：已出库/未上架/同货位拒绝，目标货位须存在且未停用、所属货架正常、容量未满
     * @param dto 调整入参（expressId、locationId 必填）
     * @return 调整结果
     */
    R updateLocation(@Validated UpdateLocationDTO dto);

    /**
     * 校验快递基础信息（快递公司存在、收件用户存在、尺寸枚举合法），供新增/修改/入库复用
     * @param dto 快递入参
     * @return 错误文案，全部通过返回 null
     */
    String validateExpressInfo(ExpressDTO dto);

    /**
     * 生成 6 位数字取件码并查重（最多尝试 50 次，已取件的码永久占号不复用）
     * @return 未被占用的取件码，全部冲突返回 null
     */
    String generatePickupCode();

    /**
     * 分页查询我的快递（登录即可，分视角：快递员=本人经手入库的快递；管理员/普通用户=收件用户或收件电话命中）
     * @param keyword 快递单号关键字，可空
     * @param status 快递状态，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页快递列表，附 total 总条数
     */
    R myList(String keyword, String status, Integer pageNum, Integer pageSize);
}
