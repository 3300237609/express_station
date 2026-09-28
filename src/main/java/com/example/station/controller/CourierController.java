package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.CourierDTO;
import com.example.station.service.CourierService;
import com.example.station.vo.CourierVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 快递员管理控制层
 * 处理快递员管理相关 HTTP 请求（管理接口仅 ADMIN 角色可用，启用禁用/重置密码复用 /user 接口）
 * 基础路径：/courier
 */
@RestController
@RequestMapping("/courier")
public class CourierController {

    /** 快递员业务层 */
    @Autowired
    private CourierService courierService;

    /**
     * 分页查询快递员列表（管理员）
     * 请求方式：GET
     * 请求路径：/courier/list
     * 请求参数：kw（用户名/姓名/手机号模糊，可选）、status（1 正常 0 禁用，可选）、pageNum（页码，默认1）、pageSize（每页条数，默认10）
     * 返回值：快递员分页列表
     */
    @GetMapping("/list")
    public R<List<CourierVO>> list(@RequestParam(required = false) String kw,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return courierService.list(kw, status, pageNum, pageSize);
    }

    /**
     * 新增快递员（管理员，固定分配 COURIER 角色）
     * 请求方式：POST
     * 请求路径：/courier/add
     * 请求体：CourierDTO（username 用户名、password 初始密码、realName 姓名、phone 手机号、email 邮箱选填）
     * 返回值：操作结果
     */
    @OperationLog(module = "快递员管理", operation = "新增快递员", description = "用户名：#{#dto.username}，姓名：#{#dto.realName}")
    @PostMapping("/add")
    public R<String> add(@Valid @RequestBody CourierDTO dto) {
        return courierService.add(dto);
    }

    /**
     * 修改快递员（管理员）
     * 请求方式：PUT
     * 请求路径：/courier/update
     * 请求体：CourierDTO（id 必填，username 不可改不生效，其余字段同新增）
     * 返回值：操作结果
     */
    @OperationLog(module = "快递员管理", operation = "修改快递员", description = "快递员ID：#{#dto.id}，姓名：#{#dto.realName}")
    @PutMapping("/update")
    public R<String> update(@Valid @RequestBody CourierDTO dto) {
        return courierService.update(dto);
    }

    /**
     * 删除快递员（管理员，物理删除并同步清空角色绑定，不允许删除自己）
     * 请求方式：DELETE
     * 请求路径：/courier/delete/{id}
     * 路径参数：id（快递员ID）
     * 返回值：操作结果
     */
    @OperationLog(module = "快递员管理", operation = "删除快递员", description = "快递员ID：#{#id}")
    @DeleteMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        return courierService.delete(id);
    }

    /**
     * 分页查询快递员投递记录（操作日志：模块=快递管理且操作人=该用户）
     * 请求方式：GET
     * 请求路径：/courier/deliveryList/{id}
     * 请求参数：路径参数 id 快递员ID；pageNum（默认1）、pageSize（默认10）
     * 返回值：投递操作日志分页列表
     * @param id 快递员ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页操作日志列表，附 total 总条数
     */
    @GetMapping("/deliveryList/{id}")
    public R deliveryList(@PathVariable Long id,
                          @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                          @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return courierService.deliveryList(id, pageNum, pageSize);
    }

    /**
     * 分页查询快递员入库记录（操作日志：模块=快递入库且操作人=该用户）
     * 请求方式：GET
     * 请求路径：/courier/storageList/{id}
     * 请求参数：路径参数 id 快递员ID；pageNum（默认1）、pageSize（默认10）
     * 返回值：入库操作日志分页列表
     * @param id 快递员ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页操作日志列表，附 total 总条数
     */
    @GetMapping("/storageList/{id}")
    public R storageList(@PathVariable Long id,
                         @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                         @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return courierService.storageList(id, pageNum, pageSize);
    }

    /**
     * 分页查询快递员登记的异常件记录（异常记录：登记人=该用户）
     * 请求方式：GET
     * 请求路径：/courier/exceptionList/{id}
     * 请求参数：路径参数 id 快递员ID；pageNum（默认1）、pageSize（默认10）
     * 返回值：异常件分页列表
     * @param id 快递员ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页异常件列表，附 total 总条数
     */
    @GetMapping("/exceptionList/{id}")
    public R exceptionList(@PathVariable Long id,
                           @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                           @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return courierService.exceptionList(id, pageNum, pageSize);
    }
}
