package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.RoleConstants;
import com.example.station.dto.ChangePasswordDTO;
import com.example.station.dto.LoginDTO;
import com.example.station.dto.RegisterDTO;
import com.example.station.entity.SysUser;
import com.example.station.mapper.SysRoleMapper;
import com.example.station.mapper.SysUserMapper;
import com.example.station.mapper.SysUserRoleMapper;
import com.example.station.security.JwtUtils;
import com.example.station.service.AuthService;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.LoginVO;
import com.example.station.vo.UserInfoVO;
import com.example.station.vo.UserVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证业务实现类
 * 负责登录、注册、退出的业务逻辑（参数校验 → 查库 → 组装，错误提前 return）
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** 用户 Mapper */
    @Autowired
    private SysUserMapper sysUserMapper;

    /** 用户角色关联 Mapper */
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    /** 角色 Mapper */
    @Autowired
    private SysRoleMapper sysRoleMapper;

    /** 密码编码器（BCrypt） */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** JWT 工具类 */
    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 用户登录：校验账号密码与状态，签发 Token 并写入 HttpOnly Cookie
     */
    @Override
    public R<LoginVO> login(LoginDTO dto, HttpServletResponse response) {
        // 1. 参数空校验（DTO @Valid 校验兜底）
        if (dto == null || !StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            return R.error("用户名和密码不能为空！");
        }
        // 2. 按用户名查询用户
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername().trim());
        if (user == null) {
            return R.error("用户名或密码错误！");
        }
        // 3. 密码校验（用户不存在与密码错误返回同一文案，避免账号枚举）
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return R.error("用户名或密码错误！");
        }
        // 4. 账号状态校验
        if (user.getStatus() == null || user.getStatus() != 1) {
            return R.error("账号已被禁用，请联系管理员！");
        }
        // 5. 查询角色列表（无角色不允许登录）
        List<String> roles = sysUserRoleMapper.selectRoleCodesByUserId(user.getId());
        if (roles == null || roles.isEmpty()) {
            return R.error("该账号未分配角色，无法登录！");
        }
        // 6. 签发 Token 并写入 HttpOnly Cookie（path=/，有效期与 Token 一致）
        String token = jwtUtils.generateToken(String.valueOf(user.getId()), roles);
        Cookie cookie = new Cookie(jwtUtils.getCookieName(), token);
        cookie.setHttpOnly(true); // HttpOnly，防止 XSS 脚本读取 Token
        cookie.setPath("/");      // 全路径生效
        cookie.setMaxAge((int) jwtUtils.getExpireSeconds()); // 与 Token 有效期一致
        response.addCookie(cookie);
        // 7. 组装返回数据（password 等敏感字段不出参）
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(buildUserInfo(user, roles));
        return R.success(loginVO);
    }

    /**
     * 用户注册：创建普通用户账号并分配 USER 角色（多表写，事务保证一致）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> register(RegisterDTO dto) {
        // 1. 参数空校验（DTO @Valid 校验兜底）
        if (dto == null || !StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            return R.error("用户名和密码不能为空！");
        }
        // 2. 两次密码一致性校验
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return R.error("两次输入的密码不一致！");
        }
        // 3. 用户名唯一性校验（不过滤已删除用户，与 uk_username 一致）
        if (sysUserMapper.countByUsername(dto.getUsername().trim()) > 0) {
            return R.error("用户名已存在！");
        }
        // 4. 手机号唯一性校验
        if (sysUserMapper.countByPhone(dto.getPhone().trim(), null) > 0) {
            return R.error("手机号已被注册！");
        }
        // 5. 校验默认角色存在
        Long roleId = sysRoleMapper.selectIdByCode(RoleConstants.USER);
        if (roleId == null) {
            return R.error("默认角色缺失，请联系管理员！");
        }
        // 6. 组装用户实体：BCrypt 加密密码，填充时间与默认值
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // 密码加密存储，禁止明文
        user.setRealName(dto.getRealName().trim());
        user.setPhone(dto.getPhone().trim());
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        user.setStatus(1);   // 默认正常
        user.setDeleted(0);  // 默认未删除
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        // 7. 保存用户并分配 USER 角色
        sysUserMapper.insert(user);
        sysUserRoleMapper.insert(user.getId(), roleId);
        return R.success("注册成功！");
    }

    /**
     * 退出登录：清除 Token Cookie（Max-Age=0 让浏览器立即删除）
     */
    @Override
    public R<String> logout(HttpServletResponse response) {
        // 1. 写入同名空值 Cookie 并设为立即过期
        Cookie cookie = new Cookie(jwtUtils.getCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return R.success("退出成功！");
    }

    /**
     * 获取当前登录用户信息（凭登录态取本人资料与角色列表）
     */
    @Override
    public R<UserInfoVO> me() {
        // 1. 当前登录人（Security 过滤器已保证认证，此处兜底防空）
        String currentId = SecurityUtils.getCurrentUserId();
        if (!StringUtils.hasText(currentId)) {
            return R.error("未获取到当前用户信息！");
        }
        // 2. 用户存在性校验（selectById 不含敏感字段）
        UserVO user = sysUserMapper.selectById(Long.valueOf(currentId));
        if (user == null) {
            return R.error("用户不存在！");
        }
        // 3. 组装用户信息与角色列表（与登录出参同结构）
        List<String> roles = sysUserRoleMapper.selectRoleCodesByUserId(user.getId());
        return R.success(buildUserInfo(user, roles));
    }

    /**
     * 修改本人密码（校验原密码后 BCrypt 加密更新；Token 无状态不作废，由前端引导重新登录）
     */
    @Override
    public R<String> changePassword(ChangePasswordDTO dto) {
        // 1. 参数空校验（DTO @Valid 校验兜底）
        if (dto == null || !StringUtils.hasText(dto.getOldPassword()) || !StringUtils.hasText(dto.getNewPassword())) {
            return R.error("原密码和新密码不能为空！");
        }
        // 2. 当前登录人与存在性校验
        String currentId = SecurityUtils.getCurrentUserId();
        if (!StringUtils.hasText(currentId)) {
            return R.error("未获取到当前用户信息！");
        }
        UserVO me = sysUserMapper.selectById(Long.valueOf(currentId));
        if (me == null) {
            return R.error("用户不存在！");
        }
        // 3. 取含密码的用户记录校验原密码（selectById 不含敏感字段，按用户名二查；物理删除并发时兜底）
        SysUser user = sysUserMapper.selectByUsername(me.getUsername());
        if (user == null) {
            return R.error("用户不存在！");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            return R.error("原密码不正确！");
        }
        // 4. 新旧密码一致性校验
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            return R.error("新密码不能与原密码相同！");
        }
        // 5. 更新密码（复用按 ID 更新密码语句，BCrypt 加密存储，禁止明文）
        sysUserMapper.resetPassword(user.getId(), passwordEncoder.encode(dto.getNewPassword()),
                LocalDateTime.now());
        return R.success("密码修改成功！");
    }

    /**
     * 组装用户信息出参（敏感字段 password 不透出）
     */
    private UserInfoVO buildUserInfo(SysUser user, List<String> roles) {
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setUserId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRoles(roles);
        return userInfo;
    }

    /**
     * 组装用户信息出参（详情视图版，与登录出参同结构，敏感字段 password 不透出）
     */
    private UserInfoVO buildUserInfo(UserVO user, List<String> roles) {
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setUserId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRoles(roles);
        return userInfo;
    }
}
