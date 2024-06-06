package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.model.LinkUnhandleQuestion;

public interface LinkUnhandleQuestionService {

    void addLinkUnhandleQuestion(LinkUnhandleQuestion linkUnhandleQuestion);

    LinkUnhandleQuestion getLinkUnhandleQuestionByUuid(String uuid);

}
