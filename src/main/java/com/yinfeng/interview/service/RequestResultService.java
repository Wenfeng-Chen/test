package com.yinfeng.interview.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yinfeng.interview.entity.RequestResult;
import com.yinfeng.interview.mapper.RequestResultMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("master")
public class RequestResultService extends ServiceImpl<RequestResultMapper, RequestResult> {
}
