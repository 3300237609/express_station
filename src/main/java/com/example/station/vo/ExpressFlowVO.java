package com.example.station.vo;

import lombok.Data;

/**
 * 快递流转记录出参（时间线节点，按时间正序返回）
 */
@Data
public class ExpressFlowVO {

    private String time;    // 节点时间（YYYY-MM-DD HH:mm:ss）
    private String event;   // 事件名称：创建快递/快递到达/入库上架/异常登记/异常处理/完成取件
    private String detail;  // 事件详情（单号、异常描述、取件方式等，可空）
}
