package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.LinkUnhandleQuestion;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface  LinkUnhandleQuestionDao {

    @Insert("INSERT INTO link_unhandle_question (uuid, tishici, result, link) VALUES (#{uuid}, #{tishici}, #{result}, #{link})")
    void insert(LinkUnhandleQuestion linkUnhandleQuestion);

    @Select("SELECT * FROM link_unhandle_question WHERE uuid = #{uuid}")
    LinkUnhandleQuestion findByUuid(String uuid);

}
