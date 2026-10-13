package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.RoleConstants;
import com.example.station.dto.CourierDTO;
import com.example.station.entity.SysUser;
import com.example.station.mapper.ExpressExceptionMapper;
import com.example.station.mapper.OperationLogMapper;
import com.example.station.mapper.SysRoleMapper;
import com.example.station.mapper.SysUserMapper;
import com.example.station.mapper.SysUserRoleMapper;
import com.example.station.service.CommonCacheService;
import com.example.station.service.CourierService;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.CourierVO;
import com.example.station.vo.ExceptionVO;
import com.example.station.vo.LogVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 快递员管理业务实现类
 * 快递员账号增删改查（错误提前 return，不抛业务异常）
 */
@Service
public class CourierServiceImpl implements CourierService {

    /** 用户 Mapper（快递员账号存储于 sys_user，角色为 COURIER） */
    @Autowired
    private SysUserMapper sysUserMapper;

    /** 用户角色关联 Mapper */
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    /** 角色 Mapper */
    @Autowired
    private SysRoleMapper sysRoleMapper;

    /** 操作日志 Mapper（投递/入库记录视图） */
    @Autowired
    private OperationLogMapper operationLogMapper;

    /** 异常记录 Mapper（异常件记录视图） */
    @Autowired
    private ExpressExceptionMapper expressExceptionMapper;

    /** 密码编码器（BCrypt） */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 通用缓存服务 */
    @Autowired
    private CommonCacheService cache;

    /**
     * 分页查询快递员列表（PageHelper 自动按同条件生成 count，仅含 COURIER 角色）
     */
    @Override
    public R<List<CourierVO>> list(String kw, Integer status, Integer pageNum, Integer pageSize) {
        // 1. 关键字去首尾空格，空串归一为 null
        String kwTrim = StringUtils.hasText(kw) ? kw.trim() : null;
        // 1.5 先查缓存
        String cacheKey = cache.courierListKey(kwTrim, status, pageNum, pageSize);
        CommonCacheService.CachedPage<CourierVO> cached = cache.get(cacheKey,
                new TypeReference<CommonCacheService.CachedPage<CourierVO>>() {
                });
        if (cached != null) {
            return R.success(cached.list())
                    .add("total", cached.total())
                    .add("pageNum", cached.pageNum())
                    .add("pageSize", cached.pageSize())
                    .add("pages", cached.pages());
        }
        // 2. 分页查询列表（固定只查快递员角色账号）
        PageHelper.startPage(pageNum, pageSize);
        List<CourierVO> list = sysUserMapper.selectCourierList(kwTrim, status, RoleConstants.COURIER);
        PageInfo<CourierVO> pageInfo = new PageInfo<>(list);
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
     * 新增快递员（用户名/手机号查重，默认正常，BCrypt 加密密码并固定分配 COURIER 角色，多表写事务保证一致）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> add(CourierDTO dto) {
        // 1. 参数空校验（DTO @Valid 校验兜底，密码新增必填由这里兜底）
        if (dto == null || !StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            return R.error("用户名和密码不能为空！");
        }
        if (!StringUtils.hasText(dto.getRealName()) || !StringUtils.hasText(dto.getPhone())) {
            return R.error("姓名和手机号不能为空！");
        }
        // 2. 用户名唯一性校验（utf8mb4_unicode_ci 大小写不敏感）
        if (sysUserMapper.countByUsername(dto.getUsername().trim()) > 0) {
            return R.error("用户名已存在！");
        }
        // 3. 手机号唯一性校验
        if (sysUserMapper.countByPhone(dto.getPhone().trim(), null) > 0) {
            return R.error("手机号已被使用！");
        }
        // 4. 校验快递员角色存在（角色数据缺失时提前返回）
        Long roleId = sysRoleMapper.selectIdByCode(RoleConstants.COURIER);
        if (roleId == null) {
            return R.error("默认角色缺失，请联系管理员！");
        }
        // 5. 组装用户实体：BCrypt 加密密码，填充时间与默认值
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // 密码加密存储，禁止明文
        user.setRealName(dto.getRealName().trim());
        user.setPhone(dto.getPhone().trim());
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        user.setStatus(1);   // 新增默认正常
        user.setDeleted(0);  // 默认未删除
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        // 6. 保存快递员并分配 COURIER 角色（事务回滚保证两表一致）
        sysUserMapper.insert(user);
        sysUserRoleMapper.insert(user.getId(), roleId);
        // 快递员与用户同表：同时清快递员与用户缓存空间
        cache.evictPrefixAfterCommit(CommonCacheService.COURIER_PREFIX, CommonCacheService.USER_PREFIX);
        return R.success("新增成功！");
    }

    /**
     * 修改快递员（全量更新可编辑字段，角色保持 COURIER 不变）
     */
    @Override
    public R<String> update(CourierDTO dto) {
        // 1. 参数空校验：修改必须携带 ID
        if (dto == null || dto.getId() == null) {
            return R.error("快递员ID不能为空！");
        }
        if (!StringUtils.hasText(dto.getRealName()) || !StringUtils.hasText(dto.getPhone())) {
            return R.error("姓名和手机号不能为空！");
        }
        // 2. 存在性校验
        if (sysUserMapper.selectById(dto.getId()) == null) {
            return R.error("快递员不存在！");
        }
        // 3. 手机号唯一性校验（排除自身，防唯一键冲突）
        if (sysUserMapper.countByPhone(dto.getPhone().trim(), dto.getId()) > 0) {
            return R.error("手机号已被使用！");
        }
        // 4. 组装实体并执行修改（用户名创建后不可修改，前端禁用编辑，此处不更新 username）
        SysUser user = new SysUser();
        user.setId(dto.getId());
        user.setRealName(dto.getRealName().trim());
        user.setPhone(dto.getPhone().trim());
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.update(user);
        cache.evictPrefixAfterCommit(CommonCacheService.COURIER_PREFIX, CommonCacheService.USER_PREFIX);
        return R.success("修改成功！");
    }

    /**
     * 删除快递员（物理删除并同步清空角色绑定，禁止删除自己）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {
        // 1. 空校验
        if (id == null) {
            return R.error("快递员ID不能为空！");
        }
        // 2. 存在性校验
        if (sysUserMapper.selectById(id) == null) {
            return R.error("快递员不存在！");
        }
        // 3. 业务规则：禁止删除自己，避免误删自身导致无法登录
        if (id.toString().equals(SecurityUtils.getCurrentUserId())) {
            return R.error("不允许删除自己！");
        }
        // 4. 物理删除快递员与角色绑定（用户名/手机号有唯一键，逻辑删除会导致同名账号无法再注册）
        sysUserMapper.delete(id);
        sysUserRoleMapper.deleteByUserId(id);
        cache.evictPrefixAfterCommit(CommonCacheService.COURIER_PREFIX, CommonCacheService.USER_PREFIX);
        return R.success("删除成功！");
    }

    /** 投递记录对应的日志模块（须与快递管理接口 @OperationLog 的 module 一致） */
    private static final String MODULE_EXPRESS = "快递管理";

    /** 入库记录对应的日志模块（须与快递入库接口 @OperationLog 的 module 一致） */
    private static final String MODULE_INBOUND = "快递入库";

    /**
     * 分页查询快递员投递记录（操作日志：模块=快递管理且操作人=该用户）
     */
    @Override
    public R deliveryList(Long id, Integer pageNum, Integer pageSize) {
        return logList(id, MODULE_EXPRESS, pageNum, pageSize);
    }

    /**
     * 分页查询快递员入库记录（操作日志：模块=快递入库且操作人=该用户）
     */
    @Override
    public R storageList(Long id, Integer pageNum, Integer pageSize) {
        return logList(id, MODULE_INBOUND, pageNum, pageSize);
    }

    /**
     * 分页查询该操作人指定模块的操作日志（投递/入库记录共用实现）
     * @param id 操作人ID
     * @param module 日志模块
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页操作日志列表，附 total 总条数
     */
    private R logList(Long id, String module, Integer pageNum, Integer pageSize) {
        // 1. 空校验与存在性校验
        if (id == null) {
            return R.error("快递员ID不能为空！");
        }
        if (sysUserMapper.selectById(id) == null) {
            return R.error("该用户不存在！");
        }
        // 2. 分页查询该操作人的模块日志
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        PageHelper.startPage(pageNum, pageSize);
        List<LogVO> list = operationLogMapper.selectList(module, null, null, null, id);
        PageInfo<LogVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    /**
     * 分页查询快递员登记的异常件记录（登记人=该用户）
     */
    @Override
    public R exceptionList(Long id, Integer pageNum, Integer pageSize) {
        // 1. 空校验与存在性校验
        if (id == null) {
            return R.error("快递员ID不能为空！");
        }
        if (sysUserMapper.selectById(id) == null) {
            return R.error("该用户不存在！");
        }
        // 2. 分页查询该登记人的异常记录
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        PageHelper.startPage(pageNum, pageSize);
        List<ExceptionVO> list = expressExceptionMapper.selectList(null, null, null, id, null);
        PageInfo<ExceptionVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }
}
