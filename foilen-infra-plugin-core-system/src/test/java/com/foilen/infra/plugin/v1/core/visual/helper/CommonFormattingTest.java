/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.helper;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class CommonFormattingTest {

    @Test
    public void testEmptyIfNull() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("alreadyEmpty", "");
        expectedFormValues.put("isNull", "");
        expectedFormValues.put("something", "10");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("something", "10");

        CommonFormatting.emptyIfNull(actualFormValues, "isNull", "alreadyEmpty", "something");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

    @Test
    public void testFirstLetterOfEachWordCapital() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("empty", "");
        expectedFormValues.put("aNumber", "10");
        expectedFormValues.put("lowers", "Abc");
        expectedFormValues.put("uppers", "Abc");
        expectedFormValues.put("many", "Abc Def Ghi,Jkl");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("empty", "");
        actualFormValues.put("aNumber", "10");
        actualFormValues.put("lowers", "abc");
        actualFormValues.put("many", "aBc DEF gHI,JKL");
        actualFormValues.put("uppers", "ABC");

        CommonFormatting.firstLetterOfEachWordCapital(actualFormValues, "empty", "aNumber", "isNull", "lowers", "many", "uppers");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

    @Test
    public void testToLowerCase() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("alreadyEmpty", "");
        expectedFormValues.put("aNumber", "10");
        expectedFormValues.put("lowers", "abc");
        expectedFormValues.put("uppers", "abc");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("aNumber", "10");
        actualFormValues.put("lowers", "abc");
        actualFormValues.put("uppers", "ABC");

        CommonFormatting.toLowerCase(actualFormValues, "alreadyEmpty", "aNumber", "isNull", "lowers", "uppers");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

    @Test
    public void testToUpperCase() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("alreadyEmpty", "");
        expectedFormValues.put("aNumber", "10");
        expectedFormValues.put("lowers", "ABC");
        expectedFormValues.put("uppers", "ABC");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("aNumber", "10");
        actualFormValues.put("lowers", "abc");
        actualFormValues.put("uppers", "ABC");

        CommonFormatting.toUpperCase(actualFormValues, "alreadyEmpty", "aNumber", "isNull", "lowers", "uppers");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

    @Test
    public void testTrimSpaces() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("alreadyEmpty", "");
        expectedFormValues.put("allSpaces", "");
        expectedFormValues.put("aNumber", "10");
        expectedFormValues.put("spacesTabsAndLines", "abcd");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("allSpaces", "     ");
        actualFormValues.put("aNumber", "    10    ");
        actualFormValues.put("spacesTabsAndLines", " a \n\r b \t c d\t");

        CommonFormatting.trimSpaces(actualFormValues, "alreadyEmpty", "allSpaces", "aNumber", "isNull", "spacesTabsAndLines");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

    @Test
    public void testTrimSpacesAround() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("alreadyEmpty", "");
        expectedFormValues.put("allSpaces", "");
        expectedFormValues.put("aNumber", "10");
        expectedFormValues.put("spacesTabsAndLines", "a \n\r b \t c d");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("allSpaces", "     ");
        actualFormValues.put("aNumber", "    10    ");
        actualFormValues.put("spacesTabsAndLines", " a \n\r b \t c d\t");

        CommonFormatting.trimSpacesAround(actualFormValues, "alreadyEmpty", "allSpaces", "aNumber", "isNull", "spacesTabsAndLines");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

}
