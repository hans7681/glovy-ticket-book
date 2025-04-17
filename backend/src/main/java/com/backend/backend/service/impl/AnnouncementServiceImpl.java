package com.backend.backend.service.impl;

import com.backend.backend.entity.Announcement;
import com.backend.backend.mapper.AnnouncementMapper;
import com.backend.backend.service.AnnouncementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    @Override
    @Transactional
    public Announcement publishAnnouncement(Long announcementId, boolean publish, Long publisherId) {
        Announcement announcement = this.getById(announcementId);
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }

        if (publish) {
            announcement.setIsPublished(true);
            announcement.setPublishTime(LocalDateTime.now());
            announcement.setPublisherId(publisherId); // 记录发布者
        } else {
            announcement.setIsPublished(false);
            announcement.setPublishTime(null); // 取消发布时清除发布时间
            // publisherId 可以不清空，保留最后一次发布者信息
        }

        boolean updated = this.updateById(announcement);
        if (!updated) {
            throw new RuntimeException("更新公告发布状态失败");
        }
        return announcement;
    }
} 