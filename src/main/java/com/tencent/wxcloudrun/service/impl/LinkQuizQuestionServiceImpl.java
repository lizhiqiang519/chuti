package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.LinkQuizQuestionMapper;
import com.tencent.wxcloudrun.model.LinkQuizQuestion;
import com.tencent.wxcloudrun.service.LinkQuizQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LinkQuizQuestionServiceImpl implements LinkQuizQuestionService {

    private final LinkQuizQuestionMapper linkQuizQuestionMapper;

    @Autowired
    public LinkQuizQuestionServiceImpl(LinkQuizQuestionMapper linkQuizQuestionMapper) {
        this.linkQuizQuestionMapper = linkQuizQuestionMapper;
    }

    @Override
    public LinkQuizQuestion findById(int id) {
        return linkQuizQuestionMapper.findById(id);
    }

    @Override
    public LinkQuizQuestion findByLink(String link) {
        return linkQuizQuestionMapper.findByLink(link);
    }

    @Override
    public void insert(LinkQuizQuestion linkQuizQuestion) {
        linkQuizQuestionMapper.insert(linkQuizQuestion);
    }


}
