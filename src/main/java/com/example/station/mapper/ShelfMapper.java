package com.example.station.mapper;

import com.example.station.entity.Shelf;
import com.example.station.vo.ShelfVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 货架表 Shelf 数据访问接口
 */
@Mapper
public interface ShelfMapper {

    /**
     * 条件查询货架列表
     * @param keyword 关键字（匹配编号或区域，可空）
     * @param status 货架状态：1 正常，0 停用，可空
     * @return 货架信息列表
     */
    List<ShelfVO> selectList(@Param("keyword") String keyword, @Param("status") Integer status);

    /**
     * 主键查询货架（逻辑删除排除）
     * @param id 货架ID
     * @return 货架信息，不存在返回 null
     */
    ShelfVO selectById(@Param("id") Long id);

    /**
     * 按货架编号查重（排除指定ID）
     * @param shelfNo 货架编号
     * @param excludeId 排除的ID（新增传 null，修改传自身ID）
     * @return 匹配数量
     */
    int countByShelfNo(@Param("shelfNo") String shelfNo, @Param("excludeId") Long excludeId);

    /**
     * 新增货架（回填主键）
     * @param shelf 货架实体
     * @return 影响行数
     */
    int insert(Shelf shelf);

    /**
     * 修改货架基础信息（不触碰 capacity/usedCount/status，均为系统维护或专用接口）
     * @param shelf 货架实体
     * @return 影响行数
     */
    int update(Shelf shelf);

    /**
     * 修改货架状态
     * @param id 货架ID
     * @param status 目标状态：1 正常，0 停用
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("updateTime") LocalDateTime updateTime);

    /**
     * 逻辑删除货架（deleted 置 1）
     * @param id 货架ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int delete(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 货位数 +1（新增货位时调用）
     * @param id 货架ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int increaseCapacity(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 货位数 -1（删除货位时调用）
     * @param id 货架ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    /**
     * 减少总货位数量（新增物理删除货位时同步，条件 used_count > 0）
     * @param id 货架ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int decreaseCapacity(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 刷新已使用货位数量（按货位表实时统计 used_count>0 的货位数重算，自愈式同步无并发漂移）
     * @param id 货架ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int refreshUsed(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
}
