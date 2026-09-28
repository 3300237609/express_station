package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.UserDTO;
import com.example.station.dto.UserStatusDTO;
import com.example.station.service.UserService;
import com.example.station.vo.UserVO;
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
 * 用户管理控制层
 * 处理用户管理相关 HTTP 请求（管理接口仅 ADMIN 角色可用，不含快递员角色）
 * 基础路径：/user
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /** 用户业务层 */
    @Autowired
    private UserService userService;

    /**
     * 分页查询用户列表（管理员）
     * 请求方式：GET
     * 请求路径：/user/list
     * 请求参数：kw（用户名/姓名/手机号模糊，可选）、status（1 正常 0 禁用，可选）、pageNum（页码，默认1）、pageSize（每页条数，默认10）
     * 返回值：用户分页列表
     */
    @GetMapping("/list")
    public R<List<UserVO>> list(@RequestParam(required = false) String kw,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return userService.list(kw, status, pageNum, pageSize);
    }

    /**
     * 查询用户详情（管理员）
     * 请求方式：GET
     * 请求路径：/user/detail/{id}
     * 路径参数：id（用户ID）
     * 返回值：用户详情
     */
    @GetMapping("/detail/{id}")
    public R<UserVO> detail(@PathVariable Long id) {
        return userService.detail(id);
    }

    /**
     * 按手机号匹配系统用户（快递入库选择收件人联动，管理员/快递员可用）
     * 请求方式：GET
     * 请求路径：/user/matchByPhone
     * 请求参数：phone（手机号，11 位，必填）
     * 返回值：用户简要信息（id/username/realName/phone），未匹配 data 为 null（收件人可能非系统用户）
     */
    @GetMapping("/matchByPhone")
    public R<UserVO> matchByPhone(@RequestParam String phone) {
        return userService.matchByPhone(phone);
    }

    /**
     * 新增用户（管理员）
     * 请求方式：POST
     * 请求路径：/user/add
     * 请求体：UserDTO（username 用户名、password 初始密码、realName 姓名、phone 手机号、email 邮箱选填、role 角色编码选填默认 USER）
     * 返回值：操作结果
     */
    @OperationLog(module = "用户管理", operation = "新增用户", description = "用户名：#{#dto.username}，角色：#{#dto.role}")
    @PostMapping("/add")
    public R<String> add(@Valid @RequestBody UserDTO dto) {
        return userService.add(dto);
    }

    /**
     * 修改用户（管理员可改任意用户并按需变更角色；登录用户可自助修改本人资料，非管理员强制仅本人且忽略 role）
     * 请求方式：PUT
     * 请求路径：/user/update
     * 请求体：UserDTO（id 必填，username 不可改不生效，role 不传则不变更，其余字段同新增）
     * 返回值：操作结果
     */
    @OperationLog(module = "用户管理", operation = "修改用户", description = "用户ID：#{#dto.id}，姓名：#{#dto.realName}")
    @PutMapping("/update")
    public R<String> update(@Valid @RequestBody UserDTO dto) {
        return userService.update(dto);
    }

    /**
     * 删除用户（管理员，物理删除并同步清空角色绑定，不允许删除自己）
     * 请求方式：DELETE
     * 请求路径：/user/delete/{id}
     * 路径参数：id（用户ID）
     * 返回值：操作结果
     */
    @OperationLog(module = "用户管理", operation = "删除用户", description = "用户ID：#{#id}")
    @DeleteMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        return userService.delete(id);
    }

    /**
     * 启用/禁用用户（管理员，不允许禁用自己；快递员账号复用此接口）
     * 请求方式：PUT
     * 请求路径：/user/updateStatus
     * 请求体：UserStatusDTO（id 用户ID、status 1 正常 0 禁用）
     * 返回值：操作结果
     */
    @OperationLog(module = "用户管理", operation = "启停用户", description = "用户ID：#{#dto.id}，状态：#{#dto.status}")
    @PutMapping("/updateStatus")
    public R<String> updateStatus(@Valid @RequestBody UserStatusDTO dto) {
        return userService.updateStatus(dto.getId(), dto.getStatus());
    }

    /**
     * 重置用户密码（管理员，重置为默认密码；快递员账号复用此接口）
     * 请求方式：PUT
     * 请求路径：/user/resetPassword/{id}
     * 路径参数：id（用户ID）
     * 返回值：操作结果（含默认密码提示）
     */
    @OperationLog(module = "用户管理", operation = "重置密码", description = "用户ID：#{#id}")
    @PutMapping("/resetPassword/{id}")
    public R<String> resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }

    /**
     * 分页查询用户名下快递（管理员查看用户收件记录）
     * 请求方式：GET
     * 请求路径：/user/express/{id}
     * 请求参数：路径参数 id 用户ID；keyword（单号，可选）、status（可选）、pageNum（默认1）、pageSize（默认10）
     * 返回值：名下快递分页列表
     * @param id 用户ID
     * @param keyword 快递单号关键字，可空
     * @param status 快递状态，可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页快递列表，附 total 总条数
     */
    @GetMapping("/express/{id}")
    public R expressList(@PathVariable Long id,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                         @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return userService.expressList(id, keyword, status, pageNum, pageSize);
    }

    /**
     * 分页查询用户取件记录（管理员查看用户取件历史）
     * 请求方式：GET
     * 请求路径：/user/pickupRecord/{id}
     * 请求参数：路径参数 id 用户ID；keyword（单号，可选）、pageNum（默认1）、pageSize（默认10）
     * 返回值：取件记录分页列表
     * @param id 用户ID
     * @param keyword 快递单号关键字，可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页取件记录列表，附 total 总条数
     */
    @GetMapping("/pickupRecord/{id}")
    public R pickupRecordList(@PathVariable Long id,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                              @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return userService.pickupRecordList(id, keyword, pageNum, pageSize);
    }
}
