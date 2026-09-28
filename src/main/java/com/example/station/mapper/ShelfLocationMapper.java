package com.example.station.mapper;

import com.example.station.entity.ShelfLocation;
import com.example.station.vo.LocationUsageVO;
import com.example.station.vo.ShelfLocationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 货位表 ShelfLocation 数据访问接口
 */
@Mapper
public interface ShelfLocationMapper {

    /**
     * 条件查询货位列表（JOIN 货架编号与区域）
     * @param shelfId 所属货架ID，可空
     * @param status 货位状态：1 空闲，2 部分占用，3 已满，4 停用，可空
     * @return 货位信息列表
     */
    List<ShelfLocationVO> selectList(@Param("shelfId") Long shelfId, @Param("status") Integer status);

    /**
     * 主键查询货位
     * @param id 货位ID
     * @return 货位信息，不存在返回 null
     */
    ShelfLocationVO selectById(@Param("id") Long id);

    /**
     * 查询空闲货位（状态=空闲且已用=0，仅所属货架正常）
     * @param shelfId 所属货架ID，可空（不传查全部货架）
     * @return 空闲货位列表（按货架、行、列排序）
     */
    List<ShelfLocationVO> selectFreeList(@Param("shelfId") Long shelfId);

    /**
     * 按货位编号查重（表级唯一键，排除指定ID）
     * @param locationNo 货位编号
     * @param excludeId 排除的ID（新增传 null，修改传自身ID）
     * @return 匹配数量
     */
    int countByLocationNo(@Param("locationNo") String locationNo, @Param("excludeId") Long excludeId);

    /**
     * 同一货架内行列号查重（排除指定ID）
     * @param shelfId 货架ID
     * @param rowNo 行号
     * @param columnNo 列号
     * @param excludeId 排除的ID
     * @return 匹配数量
     */
    int countByShelfIdRowCol(@Param("shelfId") Long shelfId, @Param("rowNo") Integer rowNo,
                             @Param("columnNo") Integer columnNo, @Param("excludeId") Long excludeId);

    /**
     * 统计货架下被占用的货位数（used_count > 0，删除货架守卫用）
     * @param shelfId 货架ID
     * @return 占用货位数
     */
    int countOccupiedByShelfId(@Param("shelfId") Long shelfId);

    /**
     * 新增货位（回填主键；无 deleted 列，不插入逻辑删除字段）
     * @param location 货位实体
     * @return 影响行数
     */
    int insert(ShelfLocation location);

    /**
     * 修改货位（不改 shelf_id 所属关系，不改 usedCount/status 系统维护字段）
     * @param location 货位实体
     * @return 影响行数
     */
    int update(ShelfLocation location);

    /**
     * 修改货位状态
     * @param id 货位ID
     * @param status 目标状态：1 空闲，2 部分占用，3 已满，4 停用
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("updateTime") LocalDateTime updateTime);

    /**
     * 物理删除货位（表无逻辑删除列；调用方须先校验未被占用）
     * @param id 货位ID
     * @return 影响行数
     */
    int delete(@Param("id") Long id);

    /**
     * 按货架批量物理删除货位（删除空货架时级联调用）
     * @param shelfId 货架ID
     * @return 影响行数
     */
    int deleteByShelfId(@Param("shelfId") Long shelfId);

    /**
     * 查询可推荐货位（未停用、有剩余容量，所属货架正常；JOIN 货架编号与区域）
     * @return 候选货位列表（按货架、行、列排序，评分在 Service 计算）
     */
    List<ShelfLocationVO> selectRecommendList();

    /**
     * 占用货位（used_count +1 并按容量重算状态；条件守卫 used_count < capacity，
     * 并发下最后一个容量被抢占用返回 0 行，由调用方返回业务错误）
     * @param id 货位ID
     * @param updateTime 更新时间
     * @return 影响行数（0 = 已满）
     */
    int increaseUsed(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 释放货位（used_count -1 并重算状态；条件守卫 used_count > 0，返回 0 行属数据不一致，调用方须回滚）
     * @param id 货位ID
     * @param updateTime 更新时间
     * @return 影响行数（0 = 占用数据异常）
     */
    int decreaseUsed(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 统计货位使用汇总（正常货架的非停用货位：总容量与已用数量，统计用）
     * @return 使用汇总（无数据返回全 0）
     */
    LocationUsageVO selectUsage();
}
