package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.PickupDTO;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

/**
 * 取件（出库）业务接口
 * 取件码验证 → 状态/归属守卫；确认取件 → 事务完成出库（改状态、写取件时间、释放货位、写取件记录、发通知）
 */
@Validated
public interface PickupService {

    /**
     * 取件码验证（只读：返回快递与货位展示信息，不改变任何状态）
     * 守卫：取件码存在、快递为待取件、普通用户仅可查看收件关联到本人的快递
     * @param code 取件码
     * @return 快递展示信息
     */
    R verify(String code);

    /**
     * 确认取件出库（事务）：状态置已取件、写取件时间、释放货位并刷新货架计数、
     * 写入取件记录（uk_express_id 一单一条）、发送取件成功通知
     * @param dto 取件入参（取件码必填；取件方式工作人员可选，普通用户固定 CODE）
     * @return 取件结果
     */
    R confirm(@Validated PickupDTO dto);

    /**
     * 分页查询取件记录（管理员/快递员视图：单号/取件码/用户关键字 + 取件时间范围）
     * @param kw 单号/取件码/用户关键字，可空
     * @param startTime 取件时间起，可空
     * @param endTime 取件时间止，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 取件记录分页列表，附 total 总条数
     */
    R recordList(String kw, LocalDateTime startTime, LocalDateTime endTime,
                 Integer pageNum, Integer pageSize);
}
