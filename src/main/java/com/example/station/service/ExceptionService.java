package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.ExceptionDTO;
import com.example.station.dto.ExceptionHandleDTO;
import org.springframework.validation.annotation.Validated;

/**
 * 异常件业务接口
 * 登记（事务：写异常记录 + 改快递状态 + 通知管理员）→ 处理（管理员更新处理状态并恢复快递状态）
 */
@Validated
public interface ExceptionService {

    /**
     * 分页条件查询异常件列表（登记/处理人姓名 JOIN 显示，按登记时间倒序）
     * @param expressId 快递ID，可空
     * @param exceptionType 异常类型：DAMAGE/LOST/WRONG_ADDR/WRONG_INFO/REFUSED/OVERDUE/OTHER，可空
     * @param handleStatus 处理状态：PENDING/PROCESSING/RESOLVED/CLOSED/REJECTED，可空
     * @param reporterId 登记人ID，可空（快递员异常记录视图按人过滤）
     * @param keyword 关键字（快递单号/收件人模糊），可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页异常件列表，附 total 总条数
     */
    R list(Long expressId, String exceptionType, String handleStatus, Long reporterId, String keyword,
           Integer pageNum, Integer pageSize);

    /**
     * 查询异常记录详情
     * @param id 异常记录ID
     * @return 异常记录详情
     */
    R detail(Long id);

    /**
     * 登记异常件（事务）：写异常记录 + 快递状态置为异常（取件/退回后登记不改状态）+ 通知全部管理员
     * 普通用户只能登记收件关联到本人（收件用户或收/寄件电话命中）的快递
     * @param dto 异常登记入参（快递ID、异常类型、描述必填）
     * @return 登记结果
     */
    R add(@Validated ExceptionDTO dto);

    /**
     * 处理异常记录（仅管理员，事务）：更新处理状态与处理结果；
     * 终态（RESOLVED/CLOSED/REJECTED）必填处理结果、恢复快递状态（已占货位→待取件，否则待入库）并通知收件用户
     * @param dto 处理入参（异常记录ID、处理状态必填，处理结果终态必填）
     * @return 处理结果
     */
    R handle(@Validated ExceptionHandleDTO dto);
}
