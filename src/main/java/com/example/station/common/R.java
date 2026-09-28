package com.example.station.common;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一响应对象
 * 所有接口统一返回 { code, msg, data, map } 结构（文件流导出接口除外）
 */
@Data
public class R<T> {

    /** 状态码：1 成功，0 失败 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 业务数据（增删改场景直接放提示文案） */
    private T data;

    /** 动态扩展字段（分页 total / pageNum / pageSize / pages 等） */
    private Map<String, Object> map = new HashMap<>();

    /**
     * 成功（无业务数据）
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> R<T> success() {
        R<T> r = new R<>();
        r.setCode(1);
        r.setMsg("操作成功！");
        return r;
    }

    /**
     * 成功并携带数据；增删改场景 data 直接放提示文案，如 R.success("修改成功！")
     * @param data 业务数据或提示文案
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> R<T> success(T data) {
        R<T> r = new R<>();
        r.setCode(1);
        r.setMsg("操作成功！");
        r.setData(data);
        return r;
    }

    /**
     * 失败
     * @param msg 错误文案（中文 + 全角感叹号，尽量带原因）
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> R<T> error(String msg) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMsg(msg);
        return r;
    }

    /**
     * 动态扩展字段（分页信息等）
     * @param key 键
     * @param value 值
     * @return 当前响应对象，支持链式调用
     */
    public R<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }
}
