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
        expectedFormValues.put("aList[0]", "10");
        expectedFormValues.put("aList[1]", "");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("something", "10");
        actualFormValues.put("aList[0]", "10");
        actualFormValues.put("aList[1]", null);

        CommonFormatting.emptyIfNull(actualFormValues, "isNull", "alreadyEmpty", "something", "aList");

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
        expectedFormValues.put("aList[0]", "Abc Def Ghi,Jkl");
        expectedFormValues.put("aList[1]", "Yep");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("empty", "");
        actualFormValues.put("aNumber", "10");
        actualFormValues.put("lowers", "abc");
        actualFormValues.put("many", "aBc DEF gHI,JKL");
        actualFormValues.put("uppers", "ABC");
        actualFormValues.put("aList[0]", "aBc DEF gHI,JKL");
        actualFormValues.put("aList[1]", "yep");

        CommonFormatting.firstLetterOfEachWordCapital(actualFormValues, "empty", "aNumber", "isNull", "lowers", "many", "uppers", "aList");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

    @Test
    public void testToLowerCase() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("alreadyEmpty", "");
        expectedFormValues.put("aNumber", "10");
        expectedFormValues.put("lowers", "abc");
        expectedFormValues.put("uppers", "abc");
        expectedFormValues.put("aList[0]", "jkl");
        expectedFormValues.put("aList[1]", "yep");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("aNumber", "10");
        actualFormValues.put("lowers", "abc");
        actualFormValues.put("uppers", "ABC");
        actualFormValues.put("aList[0]", "JKL");
        actualFormValues.put("aList[1]", "yEp");

        CommonFormatting.toLowerCase(actualFormValues, "alreadyEmpty", "aNumber", "isNull", "lowers", "uppers", "aList");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

    @Test
    public void testToUpperCase() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("alreadyEmpty", "");
        expectedFormValues.put("aNumber", "10");
        expectedFormValues.put("lowers", "ABC");
        expectedFormValues.put("uppers", "ABC");
        expectedFormValues.put("aList[0]", "JKL");
        expectedFormValues.put("aList[1]", "YEP");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("aNumber", "10");
        actualFormValues.put("lowers", "abc");
        actualFormValues.put("uppers", "ABC");
        actualFormValues.put("aList[0]", "jkl");
        actualFormValues.put("aList[1]", "yEp");

        CommonFormatting.toUpperCase(actualFormValues, "alreadyEmpty", "aNumber", "isNull", "lowers", "uppers", "aList");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

    @Test
    public void testTrimSpaces() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("alreadyEmpty", "");
        expectedFormValues.put("allSpaces", "");
        expectedFormValues.put("aNumber", "10");
        expectedFormValues.put("spacesTabsAndLines", "abcd");
        expectedFormValues.put("aList[0]", "10");
        expectedFormValues.put("aList[1]", "abcd");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("allSpaces", "     ");
        actualFormValues.put("aNumber", "    10    ");
        actualFormValues.put("spacesTabsAndLines", " a \n\r b \t c d\t");
        actualFormValues.put("aList[0]", "    10    ");
        actualFormValues.put("aList[1]", " a \n\r b \t c d\t");

        CommonFormatting.trimSpaces(actualFormValues, "alreadyEmpty", "allSpaces", "aNumber", "isNull", "spacesTabsAndLines", "aList");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

    @Test
    public void testTrimSpacesAround() {

        Map<String, String> expectedFormValues = new HashMap<>();
        expectedFormValues.put("alreadyEmpty", "");
        expectedFormValues.put("allSpaces", "");
        expectedFormValues.put("aNumber", "10");
        expectedFormValues.put("spacesTabsAndLines", "a \n\r b \t c d");
        expectedFormValues.put("aList[0]", "10");
        expectedFormValues.put("aList[1]", "a \n\r b \t c d");

        Map<String, String> actualFormValues = new HashMap<>();
        actualFormValues.put("alreadyEmpty", "");
        actualFormValues.put("allSpaces", "     ");
        actualFormValues.put("aNumber", "    10    ");
        actualFormValues.put("spacesTabsAndLines", " a \n\r b \t c d\t");
        actualFormValues.put("aList[0]", "    10    ");
        actualFormValues.put("aList[1]", " a \n\r b \t c d\t");

        CommonFormatting.trimSpacesAround(actualFormValues, "alreadyEmpty", "allSpaces", "aNumber", "isNull", "spacesTabsAndLines", "aList");

        Assert.assertEquals(expectedFormValues, actualFormValues);

    }

}
