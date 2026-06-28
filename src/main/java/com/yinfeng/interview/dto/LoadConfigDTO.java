package com.yinfeng.interview.dto;

import com.yinfeng.interview.enums.LoadMode;
import lombok.Data;

import java.util.List;

@Data
public class LoadConfigDTO {
    /** 压测模式，默认固定并发 */
    private LoadMode mode;
    /** 固定并发模式：并发线程数 */
    private int concurrency = 10;
    /** 固定并发 / RPS 模式：持续秒数 */
    private int durationSeconds = 30;
    /** RPS 模式：目标每秒请求数 */
    private int targetRps = 100;
    /** 阶梯加压模式：各阶段配置 */
    private List<LoadStageDTO> stages;
}
