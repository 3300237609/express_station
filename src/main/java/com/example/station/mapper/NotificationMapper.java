package com.example.station.mapper;

import com.example.station.entity.Notification;
import com.example.station.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息通知表 Notification 数据访问接口（表无逻辑删除列）
 */
@Mapper
public interface NotificationMapper {

    /**
     * 分页查询我的消息（接收人=当前用户，可按已读状态/类型筛选，按创建时间倒序）
     * @param userId 接收用户ID
     * @param isRead 已读状态：0 未读，1 已读，可空
     * @param type 消息类型，可空
     * @return 消息列表（含关联快递单号）
     */
    List<NotificationVO> selectMyList(@Param("userId") Long userId, @Param("isRead") Integer isRead,
                                      @Param("type") String type);

    /**
     * 统计我的未读消息数量
     * @param userId 接收用户ID
     * @return 未读条数
     */
    Long countUnread(@Param("userId") Long userId);

    /**
     * 分页查询全部消息记录（管理员查看，可按接收人/类型筛选）
     * @param userId 接收用户ID，可空
     * @param type 消息类型，可空
     * @return 消息列表
     */
    List<NotificationVO> selectManageList(@Param("userId") Long userId, @Param("type") String type);

    /**
     * 标记单条消息已读（接收人校验防越权）
     * @param id 消息ID
     * @param userId 接收用户ID
     * @param readTime 阅读时间
     * @return 影响行数
     */
    int updateRead(@Param("id") Long id, @Param("userId") Long userId,
                   @Param("readTime") LocalDateTime readTime);

    /**
     * 全部标记已读（仅未读消息）
     * @param userId 接收用户ID
     * @param readTime 阅读时间
     * @return 影响行数
     */
    int updateReadAll(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);

    /**
     * 批量新增消息（系统公告广播用）
     * @param list 消息列表
     * @return 影响行数
     */
    int insertBatch(@Param("list") List<Notification> list);

    /**
     * 新增单条消息（入库/取件/异常/超期等事件通知）
     * @param notification 消息实体
     * @return 影响行数
     */
    int insert(Notification notification);
}
