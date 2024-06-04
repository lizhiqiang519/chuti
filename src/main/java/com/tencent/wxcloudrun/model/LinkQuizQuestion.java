package com.tencent.wxcloudrun.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class LinkQuizQuestion implements Serializable {

    private int id;
    private String linkQuestion;
    private String linkOptionA;
    private String linkOptionB;
    private String linkOptionC;
    private String linkOptionD;
    private String linkAnswer;
    private String linkExplanation;
    private String link;
    private String linkPrompt;
    private String linkSummary;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLinkQuestion() {
        return linkQuestion;
    }

    public void setLinkQuestion(String linkQuestion) {
        this.linkQuestion = linkQuestion;
    }

    public String getLinkOptionA() {
        return linkOptionA;
    }

    public void setLinkOptionA(String linkOptionA) {
        this.linkOptionA = linkOptionA;
    }

    public String getLinkOptionB() {
        return linkOptionB;
    }

    public void setLinkOptionB(String linkOptionB) {
        this.linkOptionB = linkOptionB;
    }

    public String getLinkOptionC() {
        return linkOptionC;
    }

    public void setLinkOptionC(String linkOptionC) {
        this.linkOptionC = linkOptionC;
    }

    public String getLinkOptionD() {
        return linkOptionD;
    }

    public void setLinkOptionD(String linkOptionD) {
        this.linkOptionD = linkOptionD;
    }

    public String getLinkAnswer() {
        return linkAnswer;
    }

    public void setLinkAnswer(String linkAnswer) {
        this.linkAnswer = linkAnswer;
    }

    public String getLinkExplanation() {
        return linkExplanation;
    }

    public void setLinkExplanation(String linkExplanation) {
        this.linkExplanation = linkExplanation;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLinkPrompt() {
        return linkPrompt;
    }

    public void setLinkPrompt(String linkPrompt) {
        this.linkPrompt = linkPrompt;
    }

    public String getLinkSummary() {
        return linkSummary;
    }

    public void setLinkSummary(String linkSummary) {
        this.linkSummary = linkSummary;
    }
}
