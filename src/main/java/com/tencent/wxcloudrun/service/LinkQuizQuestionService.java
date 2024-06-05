package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.model.LinkQuizQuestion;

public interface LinkQuizQuestionService {

    LinkQuizQuestion findById(int id);
    LinkQuizQuestion findByLink(String link);
    void insert(LinkQuizQuestion linkQuizQuestion);

}
