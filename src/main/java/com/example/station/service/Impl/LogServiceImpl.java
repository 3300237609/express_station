package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.mapper.OperationLogMapper;
import com.example.station.service.LogService;
import com.example.station.vo.LogVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 操作日志业务实现（仅查询；写入由 OperationLogAspect 完成）
 */
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public R list(String module, String operation, String result, String keyword, Long userId,
                  Integer pageNum, Integer pageSize) {
        // 1. 分页参数与筛选条件归一
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        String moduleTrim = StringUtils.hasText(module) ? module.trim() : null;
        String operationTrim = StringUtils.hasText(operation) ? operation.trim() : null;
        String resultTrim = StringUtils.hasText(result) ? result.trim() : null;
        String keywordTrim = StringUtils.hasText(keyword) ? keyword.trim() : null;
        // 2. 分页查询日志
        PageHelper.startPage(pageNum, pageSize);
        List<LogVO> list = operationLogMapper.selectList(moduleTrim, operationTrim, resultTrim, keywordTrim, userId);
        PageInfo<LogVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }
}
