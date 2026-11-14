package com.xinzhe.projectmentor.indexing.service;

import com.xinzhe.projectmentor.config.VectorIndexProperties;
import com.xinzhe.projectmentor.indexing.entity.VectorIndexOutboxEvent;
import com.xinzhe.projectmentor.indexing.mapper.VectorIndexOutboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorIndexOutboxClaimService {
    private final VectorIndexOutboxMapper mapper;
    private final VectorIndexProperties properties;

    @Transactional(rollbackFor = Exception.class)
    public List<VectorIndexOutboxEvent> claim(String owner) {
        List<VectorIndexOutboxEvent> claimed = new ArrayList<>();
        for (VectorIndexOutboxEvent event : mapper.selectClaimable(properties.getOutboxBatchSize())) {
            if (mapper.claim(event.getId(), owner, properties.getOutboxClaimLeaseSeconds()) == 1) {
                event.setClaimOwner(owner);
                event.setPublishAttempt((event.getPublishAttempt() == null ? 0 : event.getPublishAttempt()) + 1);
                claimed.add(event);
            }
        }
        return claimed;
    }
}
