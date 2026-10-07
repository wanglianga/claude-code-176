package com.county.vision.service;

import com.county.vision.entity.TimelineEvent;
import com.county.vision.repo.TimelineEventRepository;
import org.springframework.stereotype.Service;

/** 统一健康管理时间线：筛查→告知→复诊→处方→配镜→补助→复查→学校干预 全部落一条记录 */
@Service
public class TimelineService {

    private final TimelineEventRepository repo;

    public TimelineService(TimelineEventRepository repo) {
        this.repo = repo;
    }

    public void event(Long studentId, String eventType, String title, String detail,
                      String actorName, String actorRole, String refType, Long refId) {
        TimelineEvent e = new TimelineEvent();
        e.setStudentId(studentId);
        e.setEventType(eventType);
        e.setTitle(title);
        e.setDetail(detail);
        e.setActorName(actorName);
        e.setActorRole(actorRole);
        e.setRefType(refType);
        e.setRefId(refId);
        repo.save(e);
    }
}
