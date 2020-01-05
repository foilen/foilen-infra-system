/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories.documents;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.models.MessageLevel;

@Document
public class Message {

    @Id
    private String id;
    @Version
    private long version;

    private MessageLevel level;

    private Date sentOn;
    private String sender;

    private String shortDescription;
    private String longDescription;

    private boolean acknowledged;
    private String acknowledgedBatch;

    public Message() {
    }

    public Message(MessageLevel level, Date sentOn, String sender, String shortDescription, String longDescription) {
        this.level = level;
        this.sentOn = sentOn;
        this.sender = sender;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
    }

    public String getAcknowledgedBatch() {
        return acknowledgedBatch;
    }

    public String getId() {
        return id;
    }

    public MessageLevel getLevel() {
        return level;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public String getSender() {
        return sender;
    }

    public Date getSentOn() {
        return sentOn;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public void setAcknowledgedBatch(String acknowledgedBatch) {
        this.acknowledgedBatch = acknowledgedBatch;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLevel(MessageLevel level) {
        this.level = level;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setSentOn(Date sentOn) {
        this.sentOn = sentOn;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

}
