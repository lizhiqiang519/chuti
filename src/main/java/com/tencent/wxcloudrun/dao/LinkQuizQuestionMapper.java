package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.LinkQuizQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LinkQuizQuestionMapper {

    @Select("SELECT * FROM link_quiz_question WHERE id = #{id}")
    LinkQuizQuestion findById(int id);

    @Select("SELECT * FROM link_quiz_question WHERE link = #{link}")
    LinkQuizQuestion findByLink(String link);

    @Insert("INSERT INTO link_quiz_question(link_question, link_option_a, link_option_b, link_option_c, link_option_d, link_answer, link_explanation, link, link_prompt, link_summary) VALUES(#{linkQuestion}, #{linkOptionA}, #{linkOptionB}, #{linkOptionC}, #{linkOptionD}, #{linkAnswer}, #{linkExplanation}, #{link}, #{linkPrompt}, #{linkSummary})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(LinkQuizQuestion linkQuizQuestion);

}
