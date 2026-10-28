package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.ExpressConstants;
import com.example.station.annotation.constant.NotificationConstants;
import com.example.station.annotation.constant.RoleConstants;
import com.example.station.dto.ExpressDTO;
import com.example.station.dto.UpdateLocationDTO;
import com.example.station.entity.Express;
import com.example.station.entity.ExpressException;
import com.example.station.entity.PickupRecord;
import com.example.station.mapper.ExpressCompanyMapper;
import com.example.station.mapper.ExpressExceptionMapper;
import com.example.station.mapper.ExpressMapper;
import com.example.station.mapper.PickupRecordMapper;
import com.example.station.mapper.ShelfLocationMapper;
import com.example.station.mapper.ShelfMapper;
import com.example.station.mapper.SysUserMapper;
import com.example.station.service.CommonCacheService;
import com.example.station.service.ExpressCacheService;
import com.example.station.service.ExpressService;
import com.example.station.service.NotificationService;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.ExpressFlowVO;
import com.example.station.vo.ExpressVO;
import com.example.station.vo.ShelfLocationVO;
import com.example.station.vo.ShelfVO;
import com.example.station.vo.UserVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 快递信息管理业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressServiceImpl implements ExpressService {

    private final ExpressMapper expressMapper;
    private final ExpressCompanyMapper expressCompanyMapper;
    private final SysUserMapper sysUserMapper;
    private final PickupRecordMapper pickupRecordMapper;
    private final ExpressExceptionMapper expressExceptionMapper;
    private final ShelfLocationMapper shelfLocationMapper;
    private final ShelfMapper shelfMapper;
    private final NotificationService notificationService;
    private final ExpressCacheService expressCache;
    private final CommonCacheService commonCache;

    /** 取件码随机源 */
    private final SecureRandom random = new SecureRandom();

    /** 流转节点时间格式（与 DATE_FORMAT 出参一致，保证字符串序即时间序） */
    private static final java.time.format.DateTimeFormatter FLOW_TIME_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public R list(String keyword, String pickupCode, String receiverName, String receiverPhone,
                  Long companyId, String status, LocalDate arrivalTimeStart, LocalDate arrivalTimeEnd,
                  Integer pageNum, Integer pageSize) {
        // 1. 分页参数兜底（PageHelper 默认第 1 页、每页 10 条）
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        // 2. 文本条件去空白，状态做枚举校验
        String keywordTrim = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String pickupCodeTrim = StringUtils.hasText(pickupCode) ? pickupCode.trim() : null;
        String receiverNameTrim = StringUtils.hasText(receiverName) ? receiverName.trim() : null;
        String receiverPhoneTrim = StringUtils.hasText(receiverPhone) ? receiverPhone.trim() : null;
        String statusTrim = StringUtils.hasText(status) ? status.trim() : null;
        if (statusTrim != null && !ExpressConstants.STATUS_SET.contains(statusTrim)) {
            return R.error(statusErrorMsg());
        }
        // 3. 日期范围转时间范围（起始日 00:00:00 至 结束日 23:59:59）
        LocalDateTime start = arrivalTimeStart != null ? arrivalTimeStart.atStartOfDay() : null;
        LocalDateTime end = arrivalTimeEnd != null ? arrivalTimeEnd.atTime(23, 59, 59) : null;
        // 4. 先查 Redis 缓存（命中则跳过分页查询）
        String cacheKey = expressCache.listKey(keywordTrim, pickupCodeTrim, receiverNameTrim,
                receiverPhoneTrim, companyId, statusTrim,
                start != null ? start.toString() : null, end != null ? end.toString() : null,
                pageNum, pageSize);
        ExpressCacheService.CachedPage cached = expressCache.getPage(cacheKey);
        if (cached != null) {
            return R.success(cached.list()).add("total", cached.total());
        }
        // 5. 分页查询
        PageHelper.startPage(pageNum, pageSize);
        List<ExpressVO> list = expressMapper.selectList(keywordTrim, pickupCodeTrim,
                receiverNameTrim, receiverPhoneTrim, companyId, statusTrim, start, end);
        PageInfo<ExpressVO> pageInfo = new PageInfo<>(list);
        // 6. 写入缓存（列表 + 总条数，短 TTL）
        expressCache.putPage(cacheKey, list, pageInfo.getTotal());
        return R.success(list).add("total", pageInfo.getTotal());
    }

    @Override
    public R detail(Long id) {
        // 1. 主键必填
        if (id == null) {
            return R.error("快递ID不能为空！");
        }
        // 2. 先查 Redis 缓存
        ExpressVO cached = expressCache.getDetail(id);
        if (cached != null) {
            return R.success(cached);
        }
        // 3. 存在性校验
        ExpressVO vo = expressMapper.selectById(id);
        if (vo == null) {
            return R.error("快递不存在！");
        }
        // 4. 回填缓存（主键 + 单号两个维度）
        expressCache.putDetail(vo);
        expressCache.putByExpressNo(vo);
        return R.success(vo);
    }

    @Override
    public R detailByExpressNo(String expressNo) {
        // 1. 单号必填
        if (expressNo == null || expressNo.trim().isEmpty()) {
            return R.error("快递单号不能为空！");
        }
        // 2. 先查 Redis 缓存（单号维度）
        String no = expressNo.trim();
        ExpressVO cached = expressCache.getByExpressNo(no);
        if (cached != null) {
            return R.success(cached);
        }
        // 3. 精确查询（全局唯一）
        ExpressVO vo = expressMapper.selectByExpressNo(no);
        if (vo == null) {
            return R.error("快递不存在！");
        }
        // 4. 回填两个维度缓存
        expressCache.putByExpressNo(vo);
        expressCache.putDetail(vo);
        return R.success(vo);
    }

    @Override
    public R flow(Long id) {
        // 1. 主键必填与存在性校验（取基础时间轴）
        if (id == null) {
            return R.error("快递ID不能为空！");
        }
        ExpressVO vo = expressMapper.selectById(id);
        if (vo == null) {
            return R.error("快递不存在！");
        }
        // 2. 组装流转节点：自身时间轴 + 取件记录 + 异常记录
        List<ExpressFlowVO> nodes = new ArrayList<>();
        addNode(nodes, vo.getCreateTime(), "创建快递", "快递单号：" + vo.getExpressNo());
        addNode(nodes, vo.getArrivalTime(), "快递到达", null);
        addNode(nodes, vo.getStorageTime(), "入库上架", null);
        for (ExpressException e : expressExceptionMapper.selectByExpressId(id)) {
            addNode(nodes, formatTime(e.getCreateTime()), "异常登记", e.getDescription());
            if (e.getHandleTime() != null) {
                String detail = StringUtils.hasText(e.getHandleRemark()) ? e.getHandleRemark() : e.getHandleStatus();
                addNode(nodes, formatTime(e.getHandleTime()), "异常处理", detail);
            }
        }
        List<PickupRecord> records = pickupRecordMapper.selectByExpressId(id);
        PickupRecord record = records.isEmpty() ? null : records.get(0);
        String pickupTime = vo.getPickupTime() != null ? vo.getPickupTime()
                : (record != null ? formatTime(record.getPickupTime()) : null);
        if (pickupTime != null) {
            String detail = null;
            if (record != null) {
                detail = "取件方式：" + record.getPickupMethod()
                        + (StringUtils.hasText(record.getRemark()) ? "，" + record.getRemark() : "");
            }
            addNode(nodes, pickupTime, "完成取件", detail);
        }
        // 3. 按时间正序输出（时间统一 YYYY-MM-DD HH:mm:ss，字符串序即时间序）
        nodes.sort(Comparator.comparing(ExpressFlowVO::getTime));
        return R.success(nodes);
    }

    @Override
    public R add(ExpressDTO dto) {
        // 1. 兜底空值校验
        if (!StringUtils.hasText(dto.getExpressNo())) {
            return R.error("快递单号不能为空！");
        }
        if (!StringUtils.hasText(dto.getReceiverName())) {
            return R.error("收件人不能为空！");
        }
        if (!StringUtils.hasText(dto.getReceiverPhone())) {
            return R.error("收件人电话不能为空！");
        }
        if (dto.getCompanyId() == null) {
            return R.error("快递公司不能为空！");
        }
        // 2. 关联与格式校验
        String error = validateExpressInfo(dto);
        if (error != null) {
            return R.error(error);
        }
        // 3. 快递单号查重（与 uk_express_no 同口径）
        String expressNo = dto.getExpressNo().trim();
        if (expressMapper.countByExpressNo(expressNo, null) > 0) {
            return R.error("快递单号已存在！");
        }
        // 4. 生成唯一取件码（已取件的码永久占号，不会重复分配）
        String pickupCode = generatePickupCode();
        if (pickupCode == null) {
            return R.error("取件码生成失败，请稍后重试！");
        }
        // 5. 落库（初始状态待入库，未占用货位）
        LocalDateTime now = LocalDateTime.now();
        Express express = new Express();
        express.setExpressNo(expressNo);
        express.setCompanyId(dto.getCompanyId());
        express.setSenderName(dto.getSenderName());
        express.setSenderPhone(dto.getSenderPhone());
        express.setReceiverName(dto.getReceiverName().trim());
        express.setReceiverPhone(dto.getReceiverPhone().trim());
        express.setReceiverUserId(dto.getReceiverUserId());
        express.setPickupCode(pickupCode);
        express.setStatus(ExpressConstants.STATUS_WAITING);
        express.setSizeType(dto.getSizeType());
        express.setWeight(dto.getWeight());
        express.setArrivalTime(dto.getArrivalTime());
        express.setOverdueFlag(0);
        express.setRemark(dto.getRemark());
        express.setDeleted(0);
        express.setCreateTime(now);
        express.setUpdateTime(now);
        expressMapper.insert(express);
        // 新件无详情缓存，仅需清理列表缓存
        expressCache.evictExpress(express.getId(), expressNo);
        log.info("【快递】新增成功：id={}，expressNo={}，pickupCode={}", express.getId(), expressNo, pickupCode);
        return R.success("新增成功！");
    }

    @Override
    public R update(ExpressDTO dto) {
        // 1. 主键必填与兜底空值校验
        if (dto.getId() == null) {
            return R.error("快递ID不能为空！");
        }
        if (!StringUtils.hasText(dto.getExpressNo())) {
            return R.error("快递单号不能为空！");
        }
        if (!StringUtils.hasText(dto.getReceiverName())) {
            return R.error("收件人不能为空！");
        }
        if (!StringUtils.hasText(dto.getReceiverPhone())) {
            return R.error("收件人电话不能为空！");
        }
        if (dto.getCompanyId() == null) {
            return R.error("快递公司不能为空！");
        }
        // 2. 存在性校验
        if (expressMapper.selectById(dto.getId()) == null) {
            return R.error("快递不存在！");
        }
        // 3. 关联与格式校验
        String error = validateExpressInfo(dto);
        if (error != null) {
            return R.error(error);
        }
        // 4. 快递单号查重（排除自身）
        String expressNo = dto.getExpressNo().trim();
        if (expressMapper.countByExpressNo(expressNo, dto.getId()) > 0) {
            return R.error("快递单号已存在！");
        }
        // 5. 仅更新可维护字段（不改取件码/状态/货位/业务时间戳）
        Express express = new Express();
        express.setId(dto.getId());
        express.setExpressNo(expressNo);
        express.setCompanyId(dto.getCompanyId());
        express.setSenderName(dto.getSenderName());
        express.setSenderPhone(dto.getSenderPhone());
        express.setReceiverName(dto.getReceiverName().trim());
        express.setReceiverPhone(dto.getReceiverPhone().trim());
        express.setReceiverUserId(dto.getReceiverUserId());
        express.setSizeType(dto.getSizeType());
        express.setWeight(dto.getWeight());
        express.setArrivalTime(dto.getArrivalTime());
        express.setRemark(dto.getRemark());
        express.setUpdateTime(LocalDateTime.now());
        expressMapper.update(express);
        expressCache.evictExpress(dto.getId(), expressNo);
        return R.success("修改成功！");
    }

    @Override
    public R delete(Long id) {
        // 1. 主键必填
        if (id == null) {
            return R.error("快递ID不能为空！");
        }
        ExpressVO current = expressMapper.selectById(id);
        if (current == null) {
            return R.error("快递不存在！");
        }
        // 2. 引用守卫：占用货位的快递禁止删除（须先走取件流程释放货位）
        if (current.getLocationId() != null) {
            return R.error("该快递已入库占用货位，不允许删除，请先完成取件出库！");
        }
        // 3. 引用守卫：已有取件记录/异常登记的快递禁止删除
        if (!pickupRecordMapper.selectByExpressId(id).isEmpty()) {
            return R.error("该快递已有取件记录，不允许删除！");
        }
        if (!expressExceptionMapper.selectByExpressId(id).isEmpty()) {
            return R.error("该快递已有异常登记，不允许删除！");
        }
        // 4. 逻辑删除
        expressMapper.delete(id, LocalDateTime.now());
        expressCache.evictExpress(id, current.getExpressNo());
        log.info("【快递】删除成功：id={}，expressNo={}", id, current.getExpressNo());
        return R.success("删除成功！");
    }

    @Override
    public R updateStatus(Long id, String status) {
        // 1. 参数校验（六种状态）
        if (id == null) {
            return R.error("快递ID不能为空！");
        }
        if (status == null || !ExpressConstants.STATUS_SET.contains(status)) {
            return R.error(statusErrorMsg());
        }
        // 2. 存在性校验
        ExpressVO current = expressMapper.selectById(id);
        if (current == null) {
            return R.error("快递不存在！");
        }
        // 3. 一致性守卫：仍关联货位的快递不得手工置为已取件/已退回（须走取件流程释放货位）
        if (ExpressConstants.STATUS_NEED_NO_LOCATION.contains(status) && current.getLocationId() != null) {
            return R.error("该快递仍关联货位，请先通过取件流程释放货位！");
        }
        // 4. 仅更新状态（业务时间戳由入库/取件流程维护，本接口不做修正写入）
        expressMapper.updateStatus(id, status, LocalDateTime.now());
        expressCache.evictExpress(id, current.getExpressNo());
        return R.success("状态修改成功！");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R updateLocation(UpdateLocationDTO dto) {
        // 1. 入参必填校验（DTO @Valid 校验兜底）
        if (dto == null || dto.getExpressId() == null) {
            return R.error("快递ID不能为空！");
        }
        if (dto.getLocationId() == null) {
            return R.error("目标货位不能为空！");
        }
        // 2. 快递存在性与状态/占位守卫（已出库与未上架的快递不允许调整）
        ExpressVO current = expressMapper.selectById(dto.getExpressId());
        if (current == null) {
            return R.error("快递不存在！");
        }
        if (ExpressConstants.STATUS_PICKED_UP.equals(current.getStatus())
                || ExpressConstants.STATUS_RETURNED.equals(current.getStatus())) {
            return R.error("快递已出库，不能调整货位！");
        }
        if (current.getLocationId() == null) {
            return R.error("该快递未占用货位，请先通过入库流程上架！");
        }
        if (current.getLocationId().equals(dto.getLocationId())) {
            return R.error("快递已在当前货位！");
        }
        // 3. 原货位预读（拿所属货架，供写后自愈刷新；占用中的货位不可删，缺失属数据异常）
        ShelfLocationVO oldLocation = shelfLocationMapper.selectById(current.getLocationId());
        if (oldLocation == null) {
            return R.error("原货位数据异常，请联系管理员！");
        }
        // 4. 目标货位存在性与状态校验（与入库选位同口径）
        ShelfLocationVO location = shelfLocationMapper.selectById(dto.getLocationId());
        if (location == null) {
            return R.error("货位不存在！");
        }
        if (location.getStatus() != null && location.getStatus() == 4) {
            return R.error("该货位已停用！");
        }
        // 5. 所属货架可用性校验
        ShelfVO shelf = shelfMapper.selectById(location.getShelfId());
        if (shelf == null) {
            return R.error("所属货架不存在！");
        }
        if (shelf.getStatus() == null || shelf.getStatus() != 1) {
            return R.error("所属货架已停用，不能调整货位！");
        }
        // 6. 容量预检（并发抢占由 increaseUsed 行级守卫兜底）
        if (location.getUsedCount() != null && location.getCapacity() != null
                && location.getUsedCount() >= location.getCapacity()) {
            return R.error("货位已满，请重新选择货位！");
        }
        LocalDateTime now = LocalDateTime.now();
        // 7. 占用目标货位（首个写操作：失败时无任何变更）
        if (shelfLocationMapper.increaseUsed(location.getId(), now) == 0) {
            return R.error("货位已满，请重新选择货位！");
        }
        // 8. 释放原货位（used_count>0 守卫失败属数据不一致，抛异常回滚目标货位占用）
        if (shelfLocationMapper.decreaseUsed(oldLocation.getId(), now) == 0) {
            throw new IllegalStateException("货位占用数据异常：释放原货位失败，货位调整回滚");
        }
        // 9. 更新快递货位（货位与状态双守卫，affected=0 属并发变更，抛异常回滚）
        int rows = expressMapper.updateLocation(current.getId(), location.getId(),
                oldLocation.getId(), current.getStatus(), now);
        if (rows == 0) {
            throw new IllegalStateException("快递货位调整失败：并发状态变更，请重试");
        }
        // 10. 自愈式刷新新旧货架已使用货位数（跨货架调整时两个货架都刷新）
        shelfMapper.refreshUsed(location.getShelfId(), now);
        if (!oldLocation.getShelfId().equals(location.getShelfId())) {
            shelfMapper.refreshUsed(oldLocation.getShelfId(), now);
        }
        // 11. 通知收件用户货位变化（入库通知中的货位指引已失效，随事务提交）
        notificationService.send(current.getReceiverUserId(), current.getId(), "货位调整通知",
                "您的快递（单号 " + current.getExpressNo() + "）货位已调整为：" + location.getLocationNo()
                        + "，请及时取件。",
                NotificationConstants.TYPE_PICKUP_NOTICE);
        log.info("【快递】货位调整成功：expressId={}，{} → {}",
                current.getId(), oldLocation.getLocationNo(), location.getLocationNo());
        expressCache.evictExpress(current.getId(), current.getExpressNo());
        // 货位 used_count / status 变更 + 跨货架时 shelf.used_count 也变 → 两个前缀都清
        commonCache.evictPrefixAfterCommit(CommonCacheService.SHELF_PREFIX, CommonCacheService.LOCATION_PREFIX);
        return R.success("货位调整成功！");
    }

    /**
     * 关联与格式统一校验：快递公司存在、收件用户存在、尺寸枚举合法（新增/修改/入库复用）
     * @param dto 快递入参
     * @return 错误文案，全部通过返回 null
     */
    @Override
    public String validateExpressInfo(ExpressDTO dto) {
        if (expressCompanyMapper.selectById(dto.getCompanyId()) == null) {
            return "快递公司不存在！";
        }
        if (dto.getReceiverUserId() != null && sysUserMapper.countById(dto.getReceiverUserId()) == 0) {
            return "收件用户不存在！";
        }
        if (dto.getSizeType() != null && !ExpressConstants.SIZE_SET.contains(dto.getSizeType())) {
            return "尺寸类型不正确，只能为 SMALL、MEDIUM 或 LARGE！";
        }
        return null;
    }

    /**
     * 生成 6 位数字取件码并查重（最多尝试 50 次，全部冲突返回 null）
     * @return 未被占用的取件码
     */
    @Override
    public String generatePickupCode() {
        for (int i = 0; i < 50; i++) {
            String code = String.format("%06d", random.nextInt(1_000_000));
            if (expressMapper.countByPickupCode(code) == 0) {
                return code;
            }
        }
        return null;
    }

    @Override
    public R myList(String keyword, String status, Integer pageNum, Integer pageSize) {
        // 1. 分页参数与查询条件兜底
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        String keywordTrim = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String statusTrim = StringUtils.hasText(status) ? status.trim() : null;
        if (statusTrim != null && !ExpressConstants.STATUS_SET.contains(statusTrim)) {
            return R.error(statusErrorMsg());
        }
        // 2. 当前用户与手机号（收件用户未关联时按收件电话命中名下快递）
        String currentId = SecurityUtils.getCurrentUserId();
        if (!StringUtils.hasText(currentId)) {
            return R.error("未获取到当前用户信息！");
        }
        Long userId = Long.valueOf(currentId);
        UserVO me = sysUserMapper.selectById(userId);
        if (me == null) {
            return R.error("用户不存在！");
        }
        // 3. 先查 Redis 缓存
        String cacheKey = expressCache.myListKey(userId, keywordTrim, statusTrim, pageNum, pageSize);
        ExpressCacheService.CachedPage cached = expressCache.getPage(cacheKey);
        if (cached != null) {
            return R.success(cached.list()).add("total", cached.total());
        }
        // 4. 分视角分页查询：快递员看本人经手入库的快递，其余角色看收件人视角
        PageHelper.startPage(pageNum, pageSize);
        List<ExpressVO> list;
        if (RoleConstants.COURIER.equals(SecurityUtils.getCurrentRole())) {
            list = expressMapper.selectInboundedList(userId, keywordTrim, statusTrim);
        } else {
            list = expressMapper.selectReceiverList(userId, me.getPhone(), keywordTrim, statusTrim);
        }
        PageInfo<ExpressVO> pageInfo = new PageInfo<>(list);
        expressCache.putPage(cacheKey, list, pageInfo.getTotal());
        return R.success(list).add("total", pageInfo.getTotal());
    }

    /**
     * 追加流转节点（时间为空的事件不入流）
     * @param nodes 节点列表
     * @param time 节点时间（字符串）
     * @param event 事件名称
     * @param detail 事件详情（可空）
     */
    private void addNode(List<ExpressFlowVO> nodes, String time, String event, String detail) {
        if (!StringUtils.hasText(time)) {
            return;
        }
        ExpressFlowVO node = new ExpressFlowVO();
        node.setTime(time);
        node.setEvent(event);
        node.setDetail(detail);
        nodes.add(node);
    }

    /**
     * 时间格式化为 YYYY-MM-DD HH:mm:ss（实体 LocalDateTime 转出参字符串）
     * @param time 时间，可空
     * @return 格式化字符串，入参为空返回 null
     */
    private String formatTime(LocalDateTime time) {
        return time != null ? FLOW_TIME_FORMATTER.format(time) : null;
    }

    @Override
    public R getPendingLocationIdsByShelfId(Long shelfId) {
        List<Long> ids = expressMapper.selectPendingLocationIdsByShelfId(shelfId);
        return R.success(ids);
    }

    /**
     * 状态枚举错误文案（中英混排，枚举值与数据库字面量一致）
     * @return 错误文案
     */
    private String statusErrorMsg() {
        return "状态值不正确，只能为 WAITING、STORED、WAITING_PICKUP、PICKED_UP、EXCEPTION、RETURNED！";
    }
}
