package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.dto.CompanyDTO;
import com.example.station.entity.ExpressCompany;
import com.example.station.mapper.ExpressCompanyMapper;
import com.example.station.service.CommonCacheService;
import com.example.station.service.CompanyService;
import com.example.station.vo.CompanyVO;
import com.example.station.vo.CompanyVolumeVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 快递公司业务实现类
 * 负责公司增删改查、启用停用与业务量统计（错误提前 return，不抛业务异常）
 */
@Service
public class CompanyServiceImpl implements CompanyService {

    /** 快递公司 Mapper */
    @Autowired
    private ExpressCompanyMapper expressCompanyMapper;

    /** 通用缓存服务 */
    @Autowired
    private CommonCacheService cache;

    /**
     * 分页查询快递公司列表（PageHelper 自动按同条件生成 count）
     */
    @Override
    public R<List<CompanyVO>> list(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        // 1. 关键字去首尾空格，空串归一为 null
        String keywordTrim = StringUtils.hasText(keyword) ? keyword.trim() : null;
        // 1.5 先查缓存
        String cacheKey = cache.companyListKey(keywordTrim, status, pageNum, pageSize);
        CommonCacheService.CachedPage<CompanyVO> cached = cache.get(cacheKey,
                new TypeReference<CommonCacheService.CachedPage<CompanyVO>>() {
                });
        if (cached != null) {
            return R.success(cached.list())
                    .add("total", cached.total())
                    .add("pageNum", cached.pageNum())
                    .add("pageSize", cached.pageSize())
                    .add("pages", cached.pages());
        }
        // 2. 分页查询列表
        PageHelper.startPage(pageNum, pageSize);
        List<CompanyVO> list = expressCompanyMapper.selectList(keywordTrim, status);
        PageInfo<CompanyVO> pageInfo = new PageInfo<>(list);
        // 2.5 写入缓存
        cache.put(cacheKey, new CommonCacheService.CachedPage<>(list, pageInfo.getTotal(),
                pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getPages()),
                CommonCacheService.TTL_LIST);
        // 3. 组装分页字段返回
        return R.success(list)
                .add("total", pageInfo.getTotal())
                .add("pageNum", pageInfo.getPageNum())
                .add("pageSize", pageInfo.getPageSize())
                .add("pages", pageInfo.getPages());
    }

    /**
     * 查询快递公司详情（不存在提前返回错误）
     */
    @Override
    public R<CompanyVO> detail(Long id) {
        // 1. 空校验
        if (id == null) {
            return R.error("公司ID不能为空！");
        }
        // 2. 先查缓存，未命中查库并回填
        CompanyVO company = cache.get(cache.companyDetailKey(id), CompanyVO.class);
        boolean fromDb = false;
        if (company == null) {
            company = expressCompanyMapper.selectById(id);
            fromDb = true;
        }
        if (company == null) {
            return R.error("快递公司不存在！");
        }
        if (fromDb) {
            cache.put(cache.companyDetailKey(id), company, CommonCacheService.TTL_DETAIL);
        }
        // 3. 返回详情
        return R.success(company);
    }

    /**
     * 新增快递公司（编码/名称查重，默认启用）
     */
    @Override
    public R<String> add(CompanyDTO dto) {
        // 1. 参数空校验（DTO @Valid 校验兜底）
        if (dto == null || !StringUtils.hasText(dto.getCompanyName()) || !StringUtils.hasText(dto.getCompanyCode())) {
            return R.error("公司名称和编码不能为空！");
        }
        // 2. 公司编码唯一性校验（utf8mb4_unicode_ci 大小写不敏感）
        if (expressCompanyMapper.countByCompanyCode(dto.getCompanyCode().trim(), null) > 0) {
            return R.error("公司编码已存在！");
        }
        // 3. 公司名称唯一性校验
        if (expressCompanyMapper.countByCompanyName(dto.getCompanyName().trim(), null) > 0) {
            return R.error("公司名称已存在！");
        }
        // 4. 组装实体：默认启用，手动填充时间
        ExpressCompany company = new ExpressCompany();
        company.setCompanyName(dto.getCompanyName().trim());
        company.setCompanyCode(dto.getCompanyCode().trim());
        company.setContactName(StringUtils.hasText(dto.getContactName()) ? dto.getContactName().trim() : null);
        company.setContactPhone(StringUtils.hasText(dto.getContactPhone()) ? dto.getContactPhone().trim() : null);
        company.setStatus(1);   // 新增默认启用
        company.setDeleted(0);  // 默认未删除
        company.setCreateTime(LocalDateTime.now());
        company.setUpdateTime(LocalDateTime.now());
        // 5. 执行新增
        expressCompanyMapper.insert(company);
        cache.evictPrefixAfterCommit(CommonCacheService.COMPANY_PREFIX);
        return R.success("新增成功！");
    }

    /**
     * 修改快递公司（全量更新可编辑字段，状态由专用接口维护）
     */
    @Override
    public R<String> update(CompanyDTO dto) {
        // 1. 参数空校验：修改必须携带 ID
        if (dto == null || dto.getId() == null) {
            return R.error("公司ID不能为空！");
        }
        if (!StringUtils.hasText(dto.getCompanyName()) || !StringUtils.hasText(dto.getCompanyCode())) {
            return R.error("公司名称和编码不能为空！");
        }
        // 2. 存在性校验
        if (expressCompanyMapper.selectById(dto.getId()) == null) {
            return R.error("快递公司不存在！");
        }
        // 3. 公司编码唯一性校验（排除自身）
        if (expressCompanyMapper.countByCompanyCode(dto.getCompanyCode().trim(), dto.getId()) > 0) {
            return R.error("公司编码已存在！");
        }
        // 4. 公司名称唯一性校验（排除自身）
        if (expressCompanyMapper.countByCompanyName(dto.getCompanyName().trim(), dto.getId()) > 0) {
            return R.error("公司名称已存在！");
        }
        // 5. 组装实体并执行修改
        ExpressCompany company = new ExpressCompany();
        company.setId(dto.getId());
        company.setCompanyName(dto.getCompanyName().trim());
        company.setCompanyCode(dto.getCompanyCode().trim());
        company.setContactName(StringUtils.hasText(dto.getContactName()) ? dto.getContactName().trim() : null);
        company.setContactPhone(StringUtils.hasText(dto.getContactPhone()) ? dto.getContactPhone().trim() : null);
        company.setUpdateTime(LocalDateTime.now());
        expressCompanyMapper.update(company);
        cache.evictPrefixAfterCommit(CommonCacheService.COMPANY_PREFIX);
        return R.success("修改成功！");
    }

    /**
     * 删除快递公司（逻辑删除，有快递记录时不允许删除）
     */
    @Override
    public R<String> delete(Long id) {
        // 1. 空校验
        if (id == null) {
            return R.error("公司ID不能为空！");
        }
        // 2. 存在性校验
        if (expressCompanyMapper.selectById(id) == null) {
            return R.error("快递公司不存在！");
        }
        // 3. 引用检查：名下有快递记录禁止删除，防止历史数据失联，提示改用停用
        if (expressCompanyMapper.countExpressByCompanyId(id) > 0) {
            return R.error("该公司存在快递记录，不允许删除，请改用停用！");
        }
        // 4. 逻辑删除
        expressCompanyMapper.delete(id, LocalDateTime.now());
        cache.evictPrefixAfterCommit(CommonCacheService.COMPANY_PREFIX);
        return R.success("删除成功！");
    }

    /**
     * 启用/停用快递公司（状态值与存在性校验）
     */
    @Override
    public R<String> updateStatus(Long id, Integer status) {
        // 1. 空校验与状态范围校验
        if (id == null) {
            return R.error("公司ID不能为空！");
        }
        if (status == null || (status != 0 && status != 1)) {
            return R.error("状态值不正确，只能为1启用或0停用！");
        }
        // 2. 存在性校验
        if (expressCompanyMapper.selectById(id) == null) {
            return R.error("快递公司不存在！");
        }
        // 3. 更新状态
        expressCompanyMapper.updateStatus(id, status, LocalDateTime.now());
        cache.evictPrefixAfterCommit(CommonCacheService.COMPANY_PREFIX);
        return R.success(status == 1 ? "启用成功！" : "停用成功！");
    }

    /**
     * 查询启用中的公司列表（入库/寄件选择用）
     */
    @Override
    public R<List<CompanyVO>> enabledList() {
        // 1. 先查缓存（启用列表变化少，长 TTL；写公司时统一清）
        List<CompanyVO> list = cache.get(cache.companyEnabledKey(), new TypeReference<List<CompanyVO>>() {
        });
        if (list == null) {
            list = expressCompanyMapper.selectEnabledList();
            cache.put(cache.companyEnabledKey(), list, CommonCacheService.TTL_LONG);
        }
        return R.success(list);
    }

    /**
     * 查询各快递公司业务量（LEFT JOIN 聚合，含 0 快递的公司）
     */
    @Override
    public R<List<CompanyVolumeVO>> volume() {
        // 1. 先查缓存（聚合结果随快递变化，短 TTL 60 秒）
        List<CompanyVolumeVO> list = cache.get(cache.companyVolumeKey(),
                new TypeReference<List<CompanyVolumeVO>>() {
                });
        if (list == null) {
            list = expressCompanyMapper.selectVolume();
            cache.put(cache.companyVolumeKey(), list, CommonCacheService.TTL_STAT);
        }
        return R.success(list);
    }
}
