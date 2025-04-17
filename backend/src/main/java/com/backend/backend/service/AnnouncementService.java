package com.backend.backend.service;

import com.backend.backend.entity.Announcement;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AnnouncementService extends IService<Announcement> {

    /**
     * 发布或取消发布公告
     * @param announcementId 公告 ID
     * @param publish 是否发布
     * @param publisherId 操作的管理员 ID (用于记录发布者)
     * @return 更新后的公告对象
     */
    Announcement publishAnnouncement(Long announcementId, boolean publish, Long publisherId);
} 