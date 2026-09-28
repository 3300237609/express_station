package com.example.station.mapper;

import com.example.station.entity.ExpressCompany;
import com.example.station.vo.CompanyVO;
import com.example.station.vo.CompanyVolumeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 快递公司表 express_company 数据访问接口
 * 业务量统计（LEFT JOIN express 聚合）也在此维护，快递模块落地后可迁移
 */
@Mapper
public interface ExpressCompanyMapper {

    /**
     * 分页查询快递公司列表（配合 PageHelper，自动按相同条件生成 count）
     * @param keyword 公司名称/编码关键字，可空
     * @param status 状态，可空
     * @return 公司列表
     */
    List<CompanyVO> selectList(@Param("keyword") String keyword, @Param("status") Integer status);

    /**
     * 按 ID 查询公司详情（未删除）
     * @param id 公司ID
     * @return 公司信息，不存在返回 null
     */
    CompanyVO selectById(@Param("id") Long id);

    /**
     * 统计公司名称数量（新增/修改查重）
     * @param companyName 公司名称
     * @param excludeId 排除的公司ID（修改时排除自身），新增传 null
     * @return 数量
     */
    Long countByCompanyName(@Param("companyName") String companyName, @Param("excludeId") Long excludeId);

    /**
     * 统计公司编码数量（新增/修改查重）
     * @param companyCode 公司编码
     * @param excludeId 排除的公司ID（修改时排除自身），新增传 null
     * @return 数量
     */
    Long countByCompanyCode(@Param("companyCode") String companyCode, @Param("excludeId") Long excludeId);

    /**
     * 统计该公司名下未删除的快递数量（删除前引用检查）
     * @param companyId 公司ID
     * @return 快递数量
     */
    Long countExpressByCompanyId(@Param("companyId") Long companyId);

    /**
     * 新增快递公司（回填自增主键 id）
     * @param company 公司信息
     * @return 影响行数
     */
    int insert(ExpressCompany company);

    /**
     * 修改快递公司可编辑字段（状态由 updateStatus 专用维护）
     * @param company 公司信息（须含 id）
     * @return 影响行数
     */
    int update(ExpressCompany company);

    /**
     * 启用/停用快递公司
     * @param id 公司ID
     * @param status 1 启用，0 停用
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("updateTime") LocalDateTime updateTime);

    /**
     * 逻辑删除快递公司
     * @param id 公司ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int delete(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 查询启用中的公司列表（入库/寄件选择用）
     * @return 公司列表
     */
    List<CompanyVO> selectEnabledList();

    /**
     * 按公司聚合快递业务量（LEFT JOIN express，含 0 快递的公司）
     * @return 业务量列表（按总量倒序）
     */
    List<CompanyVolumeVO> selectVolume();
}
