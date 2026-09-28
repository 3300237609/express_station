package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.PasswordConstants;
import com.example.station.annotation.constant.RoleConstants;
import com.example.station.dto.UserDTO;
import com.example.station.entity.SysUser;
import com.example.station.mapper.ExpressMapper;
import com.example.station.mapper.PickupRecordMapper;
import com.example.station.mapper.SysRoleMapper;
import com.example.station.mapper.SysUserMapper;
import com.example.station.mapper.SysUserRoleMapper;
import com.example.station.service.UserService;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.ExpressVO;
import com.example.station.vo.PickupRecordVO;
import com.example.station.vo.UserVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 用户管理业务实现类
 * 账号增删改查、启用禁用与重置密码（错误提前 return，不抛业务异常）
 */
@Service
public class UserServiceImpl implements UserService {

    /** 手机号格式（与注册校验口径一致：1 开头、第二位 3-9、共 11 位） */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 用户 Mapper */
    @Autowired
    private SysUserMapper sysUserMapper;

    /** 用户角色关联 Mapper */
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    /** 角色 Mapper */
    @Autowired
    private SysRoleMapper sysRoleMapper;

    /** 快递 Mapper（用户收件记录视图） */
    @Autowired
    private ExpressMapper expressMapper;

    /** 取件记录 Mapper（用户取件记录视图） */
    @Autowired
    private PickupRecordMapper pickupRecordMapper;

    /** 密码编码器（BCrypt） */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表（PageHelper 自动按同条件生成 count，排除快递员角色）
     */
    @Override
    public R<List<UserVO>> list(String kw, Integer status, Integer pageNum, Integer pageSize) {
        // 1. 关键字去首尾空格，空串归一为 null
        String kwTrim = StringUtils.hasText(kw) ? kw.trim() : null;
        // 2. 分页查询列表（快递员在快递员管理页维护，此处排除）
        PageHelper.startPage(pageNum, pageSize);
        List<UserVO> list = sysUserMapper.selectList(kwTrim, status, RoleConstants.COURIER);
        PageInfo<UserVO> pageInfo = new PageInfo<>(list);
        // 3. 组装分页字段返回
        return R.success(list)
                .add("total", pageInfo.getTotal())
                .add("pageNum", pageInfo.getPageNum())
                .add("pageSize", pageInfo.getPageSize())
                .add("pages", pageInfo.getPages());
    }

    /**
     * 查询用户详情（不存在提前返回错误）
     */
    @Override
    public R<UserVO> detail(Long id) {
        // 1. 空校验
        if (id == null) {
            return R.error("用户ID不能为空！");
        }
        // 2. 查询详情并校验存在性
        UserVO user = sysUserMapper.selectById(id);
        if (user == null) {
            return R.error("用户不存在！");
        }
        // 3. 返回详情（password 不透出）
        return R.success(user);
    }

    /**
     * 新增用户（用户名/手机号查重，默认正常，BCrypt 加密密码并分配角色，多表写事务保证一致）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> add(UserDTO dto) {
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
        // 4. 角色校验：未传默认普通用户，角色缺失提前返回
        String roleCode = StringUtils.hasText(dto.getRole()) ? dto.getRole().trim() : RoleConstants.USER;
        Long roleId = sysRoleMapper.selectIdByCode(roleCode);
        if (roleId == null) {
            return R.error("角色不存在，请选择正确的角色！");
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
        // 6. 保存用户并分配角色（事务回滚保证两表一致）
        sysUserMapper.insert(user);
        sysUserRoleMapper.insert(user.getId(), roleId);
        return R.success("新增成功！");
    }

    /**
     * 修改用户（管理员可改任意用户并按需变更角色；登录用户可自助修改本人资料，非管理员强制仅本人且忽略 role）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(UserDTO dto) {
        // 1. 参数空校验：修改必须携带 ID
        if (dto == null || dto.getId() == null) {
            return R.error("用户ID不能为空！");
        }
        if (!StringUtils.hasText(dto.getRealName()) || !StringUtils.hasText(dto.getPhone())) {
            return R.error("姓名和手机号不能为空！");
        }
        // 2. 自助修改防护（/user/update 对登录用户开放）：非管理员仅能改本人资料，并强制忽略 role 防越权提权
        if (!RoleConstants.ADMIN.equals(SecurityUtils.getCurrentRole())) {
            String currentId = SecurityUtils.getCurrentUserId();
            if (currentId == null || !currentId.equals(String.valueOf(dto.getId()))) {
                return R.error("仅能修改本人资料！");
            }
            dto.setRole(null);
        }
        // 3. 存在性校验
        if (sysUserMapper.selectById(dto.getId()) == null) {
            return R.error("用户不存在！");
        }
        // 4. 手机号唯一性校验（排除自身，防唯一键冲突）
        if (sysUserMapper.countByPhone(dto.getPhone().trim(), dto.getId()) > 0) {
            return R.error("手机号已被使用！");
        }
        // 5. 角色校验：未传角色则不变更，传了必须是存在的角色
        String roleCode = null;
        Long roleId = null;
        if (StringUtils.hasText(dto.getRole())) {
            roleCode = dto.getRole().trim();
            roleId = sysRoleMapper.selectIdByCode(roleCode);
            if (roleId == null) {
                return R.error("角色不存在，请选择正确的角色！");
            }
        }
        // 6. 组装实体并执行修改（用户名创建后不可修改，前端禁用编辑，此处不更新 username）
        SysUser user = new SysUser();
        user.setId(dto.getId());
        user.setRealName(dto.getRealName().trim());
        user.setPhone(dto.getPhone().trim());
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.update(user);
        // 7. 角色变更：角色不同时清空原绑定再分配新角色（系统内用户为单角色模型，事务回滚保证一致）
        if (roleCode != null) {
            List<String> oldRoles = sysUserRoleMapper.selectRoleCodesByUserId(dto.getId());
            if (!oldRoles.contains(roleCode)) {
                sysUserRoleMapper.deleteByUserId(dto.getId());
                sysUserRoleMapper.insert(dto.getId(), roleId);
            }
        }
        return R.success("修改成功！");
    }

    /**
     * 删除用户（物理删除并同步清空角色绑定，禁止删除自己）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {
        // 1. 空校验
        if (id == null) {
            return R.error("用户ID不能为空！");
        }
        // 2. 存在性校验
        if (sysUserMapper.selectById(id) == null) {
            return R.error("用户不存在！");
        }
        // 3. 业务规则：禁止删除自己，避免管理员误删自身导致无法登录
        if (id.toString().equals(SecurityUtils.getCurrentUserId())) {
            return R.error("不允许删除自己！");
        }
        // 4. 物理删除用户与角色绑定（用户名/手机号有唯一键，逻辑删除会导致同名账号无法再注册）
        sysUserMapper.delete(id);
        sysUserRoleMapper.deleteByUserId(id);
        return R.success("删除成功！");
    }

    /**
     * 启用/禁用用户（状态值、存在性与自我保护校验）
     */
    @Override
    public R<String> updateStatus(Long id, Integer status) {
        // 1. 空校验与状态范围校验
        if (id == null) {
            return R.error("用户ID不能为空！");
        }
        if (status == null || (status != 0 && status != 1)) {
            return R.error("状态值不正确，只能为1正常或0禁用！");
        }
        // 2. 存在性校验
        if (sysUserMapper.selectById(id) == null) {
            return R.error("用户不存在！");
        }
        // 3. 业务规则：禁止禁用自己，避免管理员误禁用自身导致无法登录
        if (status == 0 && id.toString().equals(SecurityUtils.getCurrentUserId())) {
            return R.error("不允许禁用自己！");
        }
        // 4. 更新状态
        sysUserMapper.updateStatus(id, status, LocalDateTime.now());
        return R.success(status == 1 ? "启用成功！" : "禁用成功！");
    }

    /**
     * 重置用户密码（BCrypt 加密默认密码后覆盖）
     */
    @Override
    public R<String> resetPassword(Long id) {
        // 1. 空校验
        if (id == null) {
            return R.error("用户ID不能为空！");
        }
        // 2. 存在性校验
        if (sysUserMapper.selectById(id) == null) {
            return R.error("用户不存在！");
        }
        // 3. 重置为默认密码（加密后覆盖，明文不出响应）
        sysUserMapper.resetPassword(id, passwordEncoder.encode(PasswordConstants.DEFAULT_PASSWORD),
                LocalDateTime.now());
        return R.success("重置成功，默认密码为 " + PasswordConstants.DEFAULT_PASSWORD + "！");
    }

    /**
     * 分页查询用户名下快递（目标用户不存在时直接返回业务错误）
     */
    @Override
    public R expressList(Long id, String keyword, String status, Integer pageNum, Integer pageSize) {
        // 1. 分页参数与查询条件归一
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        String keywordTrim = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String statusTrim = StringUtils.hasText(status) ? status.trim() : null;
        // 2. 目标用户存在性（取手机号补齐未关联收件用户的快递）
        if (id == null) {
            return R.error("用户ID不能为空！");
        }
        UserVO target = sysUserMapper.selectById(id);
        if (target == null) {
            return R.error("用户不存在！");
        }
        // 3. 分页查询名下快递
        PageHelper.startPage(pageNum, pageSize);
        List<ExpressVO> list = expressMapper.selectReceiverList(id, target.getPhone(), keywordTrim, statusTrim);
        PageInfo<ExpressVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    /**
     * 分页查询用户取件记录（目标用户不存在时直接返回业务错误）
     */
    @Override
    public R pickupRecordList(Long id, String keyword, Integer pageNum, Integer pageSize) {
        // 1. 分页参数与查询条件归一
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        String keywordTrim = StringUtils.hasText(keyword) ? keyword.trim() : null;
        // 2. 目标用户存在性
        if (id == null) {
            return R.error("用户ID不能为空！");
        }
        if (sysUserMapper.selectById(id) == null) {
            return R.error("用户不存在！");
        }
        // 3. 分页查询取件记录
        PageHelper.startPage(pageNum, pageSize);
        List<PickupRecordVO> list = pickupRecordMapper.selectUserRecordList(id, keywordTrim);
        PageInfo<PickupRecordVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    /**
     * 按手机号匹配系统用户（先校验格式，未命中返回 data=null 不报错）
     */
    @Override
    public R<UserVO> matchByPhone(String phone) {
        // 1. 参数校验：非空且符合 11 位手机号格式（纯查询，校验失败直接返回业务错误）
        if (!StringUtils.hasText(phone)) {
            return R.error("手机号不能为空！");
        }
        String phoneTrim = phone.trim();
        if (!PHONE_PATTERN.matcher(phoneTrim).matches()) {
            return R.error("手机号格式不正确！");
        }
        // 2. 精确匹配未删除用户（uk_phone 唯一至多一行）
        UserVO user = sysUserMapper.selectByPhone(phoneTrim);
        // 3. 未命中为正常业务分支（收件人可能非系统用户），data 返回 null 不报错
        return R.success(user);
    }
}
