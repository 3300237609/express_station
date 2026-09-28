package com.example.station.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 快递入库入参（继承快递基础字段；expressId 用于绑定已录入快递，不传则入库时直接创建）
 * 入库成功后：生成/沿用取件码、绑定货位、状态置为待取件 WAITING_PICKUP、记录入库时间
 * 注意：Service 手工校验（expressId 为空时才校验快递基础字段），故 Service 入口不加 @Validated
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InboundDTO extends ExpressDTO {

    private Long expressId;     // 已录入快递ID（选填：绑定已存在且状态为待入库的快递）

    private Long locationId;    // 货位ID（必填，来自智能推荐后工作人员确认）
}
