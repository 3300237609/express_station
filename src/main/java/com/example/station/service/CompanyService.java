package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.CompanyDTO;
import com.example.station.vo.CompanyVO;
import com.example.station.vo.CompanyVolumeVO;

import java.util.List;

/**
 * 快递公司业务层接口：列表、详情、新增、修改、删除、启用停用、业务量
 */
public interface CompanyService {

    /**
     * 分页查询快递公司列表（管理员）
     * @param keyword 公司名称/编码关键字，可空
     * @param status 状态（1 启用 0 停用），可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 公司分页列表（total / pageNum / pageSize / pages 在 map 中）
     */
    R<List<CompanyVO>> list(String keyword, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 查询快递公司详情（管理员）
     * @param id 公司ID
     * @return 公司详情
     */
    R<CompanyVO> detail(Long id);

    /**
     * 新增快递公司（管理员，默认启用）
     * @param dto 新增入参
     * @return 操作结果
     */
    R<String> add(CompanyDTO dto);

    /**
     * 修改快递公司（管理员，状态走专用接口）
     * @param dto 修改入参（id 必填）
     * @return 操作结果
     */
    R<String> update(CompanyDTO dto);

    /**
     * 删除快递公司（管理员，存在快递记录时不允许删除）
     * @param id 公司ID
     * @return 操作结果
     */
    R<String> delete(Long id);

    /**
     * 启用/停用快递公司（管理员）
     * @param id 公司ID
     * @param status 1 启用，0 停用
     * @return 操作结果
     */
    R<String> updateStatus(Long id, Integer status);

    /**
     * 查询启用中的公司列表（入库/寄件选择公司用，登录即可）
     * @return 启用中的公司列表
     */
    R<List<CompanyVO>> enabledList();

    /**
     * 查询各快递公司业务量（管理员）
     * @return 业务量统计列表（按总量倒序）
     */
    R<List<CompanyVolumeVO>> volume();
}
