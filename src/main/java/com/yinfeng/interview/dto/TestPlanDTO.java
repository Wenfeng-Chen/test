package com.yinfeng.interview.dto;

import com.yinfeng.interview.enums.RunMode;
import lombok.Data;

import java.util.List;

@Data
public class TestPlanDTO {

    private String name;
    private RunMode mode = RunMode.STANDALONE;
    private LoadConfigDTO load;
    private List<HttpRequestDefDTO> requests;
}
