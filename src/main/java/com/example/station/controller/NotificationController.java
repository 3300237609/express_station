package com.example.station.controller;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.dto.NotificationPublishDTO;
import com.example.station.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息通知接口
 * 我的消息（登录即可，自身范围）；公告发布/通知记录仅管理员
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    /** 消息通知业务层 */
    private final NotificationService notificationService;

    /**
     * 分页查询我的消息（登录即可）
     * 请求方式：GET
     * 请求路径：/notification/list
     * 请求参数：isRead（0 未读 1 已读，可选）、type（消息类型，可选）、pageNum（默认1）、pageSize（默认10）
     * 返回值：我的消息分页列表
     * @param isRead 已读状态，可空
     * @param type 消息类型，可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页消息列表，附 total 总条数
     */
    @GetMapping("/list")
    public R list(@RequestParam(required = false) Integer isRead,
                  @RequestParam(required = false) String type,
                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return notificationService.list(isRead, type, pageNum, pageSize);
    }

    /**
     * 查询我的未读消息数量（消息角标用，登录即可）
     * 请求方式：GET
     * 请求路径：/notification/unreadCount
     * 返回值：未读条数
     * @return 未读条数
     */
    @GetMapping("/unreadCount")
    public R<Long> unreadCount() {
        return notificationService.unreadCount();
    }

    /**
     * 标记单条消息已读（登录即可，接收人校验防越权）
     * 请求方式：PUT
     * 请求路径：/notification/read/{id}
     * 请求参数：路径参数 id 消息ID
     * 返回值：标记结果
     * @param id 消息ID
     * @return 标记结果
     */
    @PutMapping("/read/{id}")
    public R<String> read(@PathVariable Long id) {
        return notificationService.read(id);
    }

    /**
     * 全部标记已读（仅我的未读消息，登录即可）
     * 请求方式：PUT
     * 请求路径：/notification/readAll
     * 返回值：标记结果
     * @return 标记结果
     */
    @PutMapping("/readAll")
    public R<String> readAll() {
        return notificationService.readAll();
    }

    /**
     * 发布系统公告（管理员，向全部有效用户广播）
     * 请求方式：POST
     * 请求路径：/notification/publish
     * 请求体：NotificationPublishDTO（title、content 必填）
     * 返回值：发布结果
     * @param dto 公告入参
     * @return 发布结果
     */
    @OperationLog(module = "消息通知", operation = "发布公告", description = "标题：#{#dto.title}")
    @PostMapping("/publish")
    public R<String> publish(@Valid @RequestBody NotificationPublishDTO dto) {
        return notificationService.publish(dto);
    }

    /**
     * 分页查询全部消息记录（管理员，可按接收人/类型筛选）
     * 请求方式：GET
     * 请求路径：/notification/manage
     * 请求参数：userId（接收用户ID，可选）、type（可选）、pageNum（默认1）、pageSize（默认10）
     * 返回值：消息记录分页列表
     * @param userId 接收用户ID，可空
     * @param type 消息类型，可空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页消息列表，附 total 总条数
     */
    @GetMapping("/manage")
    public R manageList(@RequestParam(required = false) Long userId,
                        @RequestParam(required = false) String type,
                        @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                        @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return notificationService.manageList(userId, type, pageNum, pageSize);
    }
}
