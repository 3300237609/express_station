package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.InboundDTO;

/**
 * 快递入库业务接口（事务：绑定/创建快递 + 占用货位 + 刷新货架计数 + 通知收件用户）
 */
public interface InboundService {

    /**
     * 快递入库（expressId 为空时创建快递并入库；不为空时绑定已录入快递）
     * 守卫：货位/货架可用且有容量、快递须为待入库且未占货位；入库后状态置为待取件并记录入库时间
     * @param dto 入库入参（locationId 必填，expressId 选填）
     * @return 入库结果（返回入库后的快递详情，含取件码与货位）
     */
    R add(InboundDTO dto);
}
