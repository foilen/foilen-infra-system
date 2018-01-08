/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.editors;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.foilen.infra.plugin.core.system.fake.junits.AbstractIPPluginTest;
import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.smalltools.tuple.Tuple2;

public class UnixUserEditorTest extends AbstractIPPluginTest {

    private void assertUnixUserNotExists(String name) {
        IPResourceService resourceService = getCommonServicesContext().getResourceService();
        Assert.assertFalse(resourceService.resourceFind(resourceService.createResourceQuery(UnixUser.class) //
                .propertyEquals(UnixUser.PROPERTY_NAME, name)) //
                .isPresent());
    }

    private UnixUser findByName(String name) {
        IPResourceService resourceService = getCommonServicesContext().getResourceService();
        return resourceService.resourceFind(resourceService.createResourceQuery(UnixUser.class) //
                .propertyEquals(UnixUser.PROPERTY_NAME, name)) //
                .get();
    }

    @Test
    public void test() throws InstantiationException, IllegalAccessException {

        UnixUserEditor unixUserEditor = new UnixUserEditor();

        // Create without password [OK]
        Map<String, String> formValues = new HashMap<>();
        formValues.put(UnixUser.PROPERTY_NAME, "user1");
        assertEditorNoErrors(null, unixUserEditor, formValues);

        UnixUser unixUser1 = findByName("user1");
        Assert.assertEquals("/home/user1", unixUser1.getHomeFolder());
        Assert.assertNull(unixUser1.getHashedPassword());

        // Create with password [OK]
        formValues = new HashMap<>();
        formValues.put(UnixUser.PROPERTY_NAME, "user2");
        formValues.put(UnixUserEditor.FIELD_PASSWORD, "qwerty");
        formValues.put(UnixUserEditor.FIELD_PASSWORD_CONF, "qwerty");
        assertEditorNoErrors(null, unixUserEditor, formValues);

        UnixUser unixUser2 = findByName("user2");
        Assert.assertEquals("/home/user2", unixUser2.getHomeFolder());
        Assert.assertNotNull(unixUser2.getHashedPassword());

        // Create with different password [FAIL]
        formValues = new HashMap<>();
        formValues.put(UnixUser.PROPERTY_NAME, "user3");
        formValues.put(UnixUserEditor.FIELD_PASSWORD, "qwerty");
        formValues.put(UnixUserEditor.FIELD_PASSWORD_CONF, "qwerty2");
        assertEditorWithErrors(null, unixUserEditor, formValues, new Tuple2<>("passwordConf", "error.notSamePassword"));

        // Update password [OK]
        String initialHash = unixUser2.getHashedPassword();
        formValues = new HashMap<>();
        formValues.put(UnixUser.PROPERTY_NAME, "user2");
        formValues.put(UnixUserEditor.FIELD_PASSWORD, "qwerty");
        formValues.put(UnixUserEditor.FIELD_PASSWORD_CONF, "qwerty");
        assertEditorNoErrors(unixUser2.getInternalId(), unixUserEditor, formValues);

        unixUser2 = findByName("user2");
        Assert.assertEquals("/home/user2", unixUser2.getHomeFolder());
        Assert.assertNotNull(unixUser2.getHashedPassword());
        Assert.assertNotEquals(initialHash, unixUser2.getHashedPassword());

        // Clear password [OK]
        formValues = new HashMap<>();
        formValues.put(UnixUser.PROPERTY_NAME, "user2");
        formValues.put(UnixUserEditor.FIELD_PASSWORD, UnixUserEditor.CLEAR_PASSWORD_CHAR);
        formValues.put(UnixUserEditor.FIELD_PASSWORD_CONF, "");
        assertEditorNoErrors(unixUser2.getInternalId(), unixUserEditor, formValues);

        unixUser2 = findByName("user2");
        Assert.assertEquals("/home/user2", unixUser2.getHomeFolder());
        Assert.assertNull(unixUser2.getHashedPassword());

        // Create with existing name [FAIL]
        formValues = new HashMap<>();
        formValues.put(UnixUser.PROPERTY_NAME, "user2");
        formValues.put(UnixUserEditor.FIELD_PASSWORD, "qwerty");
        formValues.put(UnixUserEditor.FIELD_PASSWORD_CONF, "qwerty");
        assertEditorWithErrors(null, unixUserEditor, formValues, new Tuple2<>("name", "error.nameTaken"));

        // Update password [OK]
        formValues = new HashMap<>();
        formValues.put(UnixUser.PROPERTY_NAME, "user2");
        formValues.put(UnixUserEditor.FIELD_PASSWORD, "qwerty");
        formValues.put(UnixUserEditor.FIELD_PASSWORD_CONF, "qwerty");
        assertEditorNoErrors(unixUser2.getInternalId(), unixUserEditor, formValues);

        unixUser2 = findByName("user2");
        Assert.assertEquals("/home/user2", unixUser2.getHomeFolder());
        Assert.assertNotNull(unixUser2.getHashedPassword());

        // Update with different name [OK]
        initialHash = unixUser2.getHashedPassword();
        formValues = new HashMap<>();
        formValues.put(UnixUser.PROPERTY_NAME, "user20");
        assertEditorNoErrors(unixUser2.getInternalId(), unixUserEditor, formValues);

        assertUnixUserNotExists("user2");
        UnixUser unixUser20 = findByName("user20");
        Assert.assertEquals("/home/user2", unixUser20.getHomeFolder());
        Assert.assertNotNull(unixUser20.getHashedPassword());
        Assert.assertEquals(initialHash, unixUser20.getHashedPassword());

        // Update with existing name [FAIL]
        formValues = new HashMap<>();
        formValues.put(UnixUser.PROPERTY_NAME, "user1");
        assertEditorWithErrors(unixUser20.getInternalId(), unixUserEditor, formValues, new Tuple2<>("name", "error.nameTaken"));

    }

}
