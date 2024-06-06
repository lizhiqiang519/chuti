package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.LinkUnhandleQuestionDao;
import com.tencent.wxcloudrun.model.LinkUnhandleQuestion;
import com.tencent.wxcloudrun.service.LinkUnhandleQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LinkUnhandleQuestionServiceImpl implements LinkUnhandleQuestionService {

    @Autowired
    private LinkUnhandleQuestionDao linkUnhandleQuestionDao;

    @Override
    public void addLinkUnhandleQuestion(LinkUnhandleQuestion linkUnhandleQuestion) {
        linkUnhandleQuestionDao.insert(linkUnhandleQuestion);
    }

    @Override
    public LinkUnhandleQuestion getLinkUnhandleQuestionByUuid(String uuid) {
        return linkUnhandleQuestionDao.findByUuid(uuid);
    }
}
