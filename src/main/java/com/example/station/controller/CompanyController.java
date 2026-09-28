package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.CompanyDTO;
import com.example.station.service.CompanyService;
import com.example.station.vo.CompanyVO;
import com.example.station.vo.CompanyVolumeVO;
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
 * 快递公司控制层
 * 处理快递公司管理相关 HTTP 请求（管理接口仅 ADMIN 角色可用）
 * 基础路径：/company
 */
@RestController
@RequestMapping("/company")
public class CompanyController {

    /** 快递公司业务层 */
    @Autowired
    private CompanyService companyService;

    /**
     * 分页查询快递公司列表（管理员）
     * 请求方式：GET
     * 请求路径：/company/list
     * 请求参数：keyword（公司名称/编码模糊，可选）、status（1 启用 0 停用，可选）、pageNum（页码，默认1）、pageSize（每页条数，默认10）
     * 返回值：快递公司分页列表
     */
    @GetMapping("/list")
    public R<List<CompanyVO>> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return companyService.list(keyword, status, pageNum, pageSize);
    }

    /**
     * 查询快递公司详情（管理员）
     * 请求方式：GET
     * 请求路径：/company/detail/{id}
     * 路径参数：id（公司ID）
     * 返回值：快递公司详情
     */
    @GetMapping("/detail/{id}")
    public R<CompanyVO> detail(@PathVariable Long id) {
        return companyService.detail(id);
    }

    /**
     * 新增快递公司（管理员）
     * 请求方式：POST
     * 请求路径：/company/add
     * 请求体：CompanyDTO（companyName 公司名称、companyCode 公司编码、contactName 联系人、contactPhone 联系电话，后两项选填）
     * 返回值：操作结果
     */
    @OperationLog(module = "快递公司", operation = "新增公司", description = "公司：#{#dto.companyName}")
    @PostMapping("/add")
    public R<String> add(@Valid @RequestBody CompanyDTO dto) {
        return companyService.add(dto);
    }

    /**
     * 修改快递公司（管理员）
     * 请求方式：PUT
     * 请求路径：/company/update
     * 请求体：CompanyDTO（id 必填，其余字段同新增）
     * 返回值：操作结果
     */
    @OperationLog(module = "快递公司", operation = "修改公司", description = "公司ID：#{#dto.id}，公司：#{#dto.companyName}")
    @PutMapping("/update")
    public R<String> update(@Valid @RequestBody CompanyDTO dto) {
        return companyService.update(dto);
    }

    /**
     * 删除快递公司（管理员，名下存在快递记录时不允许删除）
     * 请求方式：DELETE
     * 请求路径：/company/delete/{id}
     * 路径参数：id（公司ID）
     * 返回值：操作结果
     */
    @OperationLog(module = "快递公司", operation = "删除公司", description = "公司ID：#{#id}")
    @DeleteMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        return companyService.delete(id);
    }

    /**
     * 启用/停用快递公司（管理员）
     * 请求方式：PUT
     * 请求路径：/company/updateStatus/{id}
     * 路径参数：id（公司ID）
     * 请求参数：status（1 启用，0 停用）
     * 返回值：操作结果
     */
    @OperationLog(module = "快递公司", operation = "启停公司", description = "公司ID：#{#id}，状态：#{#status}")
    @PutMapping("/updateStatus/{id}")
    public R<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return companyService.updateStatus(id, status);
    }

    /**
     * 查询启用中的快递公司（入库/寄件选择公司用，登录即可访问）
     * 请求方式：GET
     * 请求路径：/company/enabled
     * 返回值：启用中的公司列表
     */
    @GetMapping("/enabled")
    public R<List<CompanyVO>> enabledList() {
        return companyService.enabledList();
    }

    /**
     * 查询各快递公司业务量（管理员）
     * 请求方式：GET
     * 请求路径：/company/volume
     * 返回值：公司业务量统计列表（按总量倒序）
     */
    @GetMapping("/volume")
    public R<List<CompanyVolumeVO>> volume() {
        return companyService.volume();
    }
}
