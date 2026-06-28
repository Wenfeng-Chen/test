package com.yinfeng.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yinfeng.interview.config.LoadTestProperties;
import com.yinfeng.interview.dto.WorkerRegisterDTO;
import com.yinfeng.interview.entity.WorkerNode;
import com.yinfeng.interview.enums.WorkerStatus;
import com.yinfeng.interview.mapper.WorkerNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Profile("master")
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerNodeMapper workerNodeMapper;
    private final LoadTestProperties properties;

    public String register(WorkerRegisterDTO dto) {
        String workerId = dto.getWorkerId() != null ? dto.getWorkerId() : UUID.randomUUID().toString();
        WorkerNode existing = workerNodeMapper.selectOne(
                new LambdaQueryWrapper<WorkerNode>().eq(WorkerNode::getWorkerId, workerId));

        if (existing != null) {
            existing.setHost(dto.getHost());
            existing.setPort(dto.getPort());
            existing.setStatus(WorkerStatus.ONLINE.name());
            existing.setLastHeartbeat(LocalDateTime.now());
            workerNodeMapper.updateById(existing);
        } else {
            WorkerNode node = new WorkerNode();
            node.setWorkerId(workerId);
            node.setHost(dto.getHost());
            node.setPort(dto.getPort());
            node.setStatus(WorkerStatus.ONLINE.name());
            node.setLastHeartbeat(LocalDateTime.now());
            workerNodeMapper.insert(node);
        }
        return workerId;
    }

    public void heartbeat(String workerId) {
        WorkerNode node = workerNodeMapper.selectOne(
                new LambdaQueryWrapper<WorkerNode>().eq(WorkerNode::getWorkerId, workerId));
        if (node != null) {
            node.setLastHeartbeat(LocalDateTime.now());
            node.setStatus(WorkerStatus.ONLINE.name());
            workerNodeMapper.updateById(node);
        }
    }

    public List<WorkerNode> listActiveWorkers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(properties.getWorkerTimeoutSeconds());
        return workerNodeMapper.selectList(new LambdaQueryWrapper<WorkerNode>()
                .eq(WorkerNode::getStatus, WorkerStatus.ONLINE.name())
                .ge(WorkerNode::getLastHeartbeat, threshold));
    }

    @Scheduled(fixedDelay = 15000)
    public void markOfflineWorkers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(properties.getWorkerTimeoutSeconds());
        List<WorkerNode> stale = workerNodeMapper.selectList(new LambdaQueryWrapper<WorkerNode>()
                .lt(WorkerNode::getLastHeartbeat, threshold));
        for (WorkerNode node : stale) {
            node.setStatus(WorkerStatus.OFFLINE.name());
            workerNodeMapper.updateById(node);
        }
    }
}
