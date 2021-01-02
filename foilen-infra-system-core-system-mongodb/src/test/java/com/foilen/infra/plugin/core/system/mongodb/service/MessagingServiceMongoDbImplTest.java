/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.foilen.infra.plugin.core.system.mongodb.AbstractSpringTest;
import com.foilen.infra.plugin.core.system.mongodb.repositories.MessageRepository;
import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.Message;
import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.models.MessageLevel;
import com.foilen.infra.plugin.v1.core.service.MessagingService;
import com.foilen.smalltools.test.asserts.AssertTools;

public class MessagingServiceMongoDbImplTest extends AbstractSpringTest {

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private MessageRepository messageRepository;

    private List<Message> findAll() {
        return messageRepository.findAll().stream() //
                .peek(m -> m.setId(null)) //
                .peek(m -> m.setSentOn(null)) //
                .collect(Collectors.toList());
    }

    @Before
    public void init() {
        messageRepository.deleteAll();
    }

    @Test
    public void testAlerting() {

        // Create some
        messagingService.alertingError("An error", "Error Desc");
        messagingService.alertingInfo("An info", "Info Desc");
        messagingService.alertingWarn("A warn", "Warn Desc");

        // Assert they are there
        AssertTools.assertJsonComparisonWithoutNulls("MessagingServiceMongoDbImplTest-testAlerting-expected.json", getClass(), findAll());

    }

    @Test
    public void testProcessing() {

        // Create some
        for (int i = 1; i <= 10; ++i) {
            messageRepository.save(new Message(MessageLevel.INFO, new Date(i), "Batch1", "Message " + i, "Desc"));
        }

        // Count
        Assert.assertEquals(5, messageRepository.countBySentOnBeforeAndAcknowledgedIsFalse(new Date(6)));

        // Find and Acknowledge
        List<Message> acknowledged = messageRepository.findAllNotAcknowledgedAndAcknowledgedThem();
        acknowledged.forEach(m -> m.setId(null));
        acknowledged.forEach(m -> m.setAcknowledgedBatch(null));
        AssertTools.assertJsonComparisonWithoutNulls("MessagingServiceMongoDbImplTest-testProcessing-1-expected.json", getClass(), acknowledged);

        // Count
        Assert.assertEquals(0, messageRepository.countBySentOnBeforeAndAcknowledgedIsFalse(new Date(6)));
        Assert.assertEquals(0, messageRepository.countBySentOnBeforeAndAcknowledgedIsFalse(new Date(11)));

        // Add more
        for (int i = 11; i <= 20; ++i) {
            messageRepository.save(new Message(MessageLevel.INFO, new Date(i), "Batch2", "Message " + i, "Desc"));
        }

        // Delete some
        Assert.assertEquals(2, messageRepository.deleteBySentOnBeforeAndAcknowledgedIsTrue(new Date(3)));
        AssertTools.assertJsonComparisonWithoutNulls("MessagingServiceMongoDbImplTest-testProcessing-2-expected.json", getClass(), findAll());
        Assert.assertEquals(0, messageRepository.deleteBySentOnBeforeAndAcknowledgedIsTrue(new Date(3)));
        AssertTools.assertJsonComparisonWithoutNulls("MessagingServiceMongoDbImplTest-testProcessing-2-expected.json", getClass(), findAll());
        Assert.assertEquals(8, messageRepository.deleteBySentOnBeforeAndAcknowledgedIsTrue(new Date(15)));
        AssertTools.assertJsonComparisonWithoutNulls("MessagingServiceMongoDbImplTest-testProcessing-3-expected.json", getClass(), findAll());

        // Count
        Assert.assertEquals(5, messageRepository.countBySentOnBeforeAndAcknowledgedIsFalse(new Date(16)));

        // Find and Acknowledge
        acknowledged = messageRepository.findAllNotAcknowledgedAndAcknowledgedThem();
        acknowledged.forEach(m -> m.setId(null));
        acknowledged.forEach(m -> m.setAcknowledgedBatch(null));
        AssertTools.assertJsonComparisonWithoutNulls("MessagingServiceMongoDbImplTest-testProcessing-4-expected.json", getClass(), acknowledged);

        // Count
        Assert.assertEquals(0, messageRepository.countBySentOnBeforeAndAcknowledgedIsFalse(new Date(6)));
        Assert.assertEquals(0, messageRepository.countBySentOnBeforeAndAcknowledgedIsFalse(new Date(11)));
        Assert.assertEquals(0, messageRepository.countBySentOnBeforeAndAcknowledgedIsFalse(new Date(30)));

    }

}
