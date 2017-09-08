/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.resource;

import org.junit.Assert;
import org.junit.Test;

public class AbstractIPResourceTest {

    public static class TestResource extends AbstractIPResource {
        private String text;
        private int number;

        public TestResource(String text, int number) {
            this.text = text;
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

        @Override
        public InfraPluginResourceCategory getResourceCategory() {
            return InfraPluginResourceCategory.INFRASTRUCTURE;
        }

        @Override
        public String getResourceDescription() {
            return "Test";
        }

        @Override
        public String getResourceName() {
            return "Test";
        }

        public String getText() {
            return text;
        }

    }

    @Test
    public void testEqualsAndHashCode() {
        TestResource rA1 = new TestResource("I am A", 1);
        TestResource rA2 = new TestResource("I am A", 1);

        TestResource rB1 = new TestResource("I am A", 2);
        TestResource rB2 = new TestResource("I am A", 2);

        TestResource rC1 = new TestResource("I am B", 1);

        // Not the same object
        Assert.assertTrue(rA1 != rA2);
        Assert.assertTrue(rB1 != rB2);

        // Same values
        Assert.assertEquals(rA1, rA2);
        Assert.assertEquals(rB1, rB2);

        // Not same values
        Assert.assertNotEquals(rA1, rB1);
        Assert.assertNotEquals(rA1, rC1);
        Assert.assertNotEquals(rB1, rC1);

        // Same hash
        Assert.assertEquals(rA1.hashCode(), rA2.hashCode());
        Assert.assertEquals(rB1.hashCode(), rB2.hashCode());

        // Not same hash
        Assert.assertNotEquals(rA1.hashCode(), rB1.hashCode());
        Assert.assertNotEquals(rA1.hashCode(), rC1.hashCode());
        Assert.assertNotEquals(rB1.hashCode(), rC1.hashCode());

        // Ignore the ID
        rA2.setInternalId(4L);
        Assert.assertEquals(rA1, rA2);
        Assert.assertEquals(rA1.hashCode(), rA2.hashCode());
    }

}
