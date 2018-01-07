/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.helper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.foilen.infra.plugin.v1.core.base.resources.model.DnsEntryType;
import com.foilen.smalltools.tuple.Tuple2;

public class CommonValidationTest {

    private String fieldName = "test";

    private void assertAlphaNum(List<String> values, List<Boolean> isValids) {
        Map<String, String> map = new HashMap<>();
        fillValues(values, map);
        List<Tuple2<String, String>> errors = CommonValidation.validateAlphaNum(map, fieldName);
        assertValues(errors, isValids);
    }

    private void assertAlphaNum(String value, boolean isValid) {
        Map<String, String> map = new HashMap<>();
        map.put(fieldName, value);
        List<Tuple2<String, String>> errors = CommonValidation.validateAlphaNum(map, fieldName);
        Assert.assertEquals(isValid, errors.isEmpty());
    }

    private void assertCronTime(List<String> values, List<Boolean> isValids) {
        Map<String, String> map = new HashMap<>();
        fillValues(values, map);
        List<Tuple2<String, String>> errors = CommonValidation.validateCronTime(map, fieldName);
        assertValues(errors, isValids);
    }

    private void assertCronTime(String value, boolean isValid) {
        Map<String, String> map = new HashMap<>();
        map.put(fieldName, value);
        List<Tuple2<String, String>> errors = CommonValidation.validateCronTime(map, fieldName);
        Assert.assertEquals(isValid, errors.isEmpty());
    }

    private void assertDomainName(List<String> values, List<Boolean> isValids) {
        Map<String, String> map = new HashMap<>();
        fillValues(values, map);
        List<Tuple2<String, String>> errors = CommonValidation.validateDomainName(map, fieldName);
        assertValues(errors, isValids);
    }

    private void assertDomainName(String value, boolean isValid) {
        Map<String, String> map = new HashMap<>();
        map.put(fieldName, value);
        List<Tuple2<String, String>> errors = CommonValidation.validateDomainName(map, fieldName);
        Assert.assertEquals(isValid, errors.isEmpty());
    }

    private void assertEmail(List<String> values, List<Boolean> isValids) {
        Map<String, String> map = new HashMap<>();
        fillValues(values, map);
        List<Tuple2<String, String>> errors = CommonValidation.validateEmail(map, fieldName);
        assertValues(errors, isValids);
    }

    private void assertEmail(String value, boolean isValid) {
        Map<String, String> map = new HashMap<>();
        map.put(fieldName, value);
        List<Tuple2<String, String>> errors = CommonValidation.validateEmail(map, fieldName);
        Assert.assertEquals(isValid, errors.isEmpty());
    }

    private void assertInEnum(List<String> values, List<Boolean> isValids, Enum<?>... possibleValues) {
        Map<String, String> map = new HashMap<>();
        fillValues(values, map);
        List<Tuple2<String, String>> errors = CommonValidation.validateInEnum(map, possibleValues, fieldName);
        assertValues(errors, isValids);
    }

    private void assertInEnum(String value, boolean isValid, Enum<?>... possibleValues) {
        Map<String, String> map = new HashMap<>();
        map.put(fieldName, value);
        List<Tuple2<String, String>> errors = CommonValidation.validateInEnum(map, possibleValues, fieldName);
        Assert.assertEquals(isValid, errors.isEmpty());
    }

    private void assertIpAddress(List<String> values, List<Boolean> isValids) {
        Map<String, String> map = new HashMap<>();
        fillValues(values, map);
        List<Tuple2<String, String>> errors = CommonValidation.validateIpAddress(map, fieldName);
        assertValues(errors, isValids);
    }

    private void assertIpAddress(String value, boolean isValid) {
        Map<String, String> map = new HashMap<>();
        map.put(fieldName, value);
        List<Tuple2<String, String>> errors = CommonValidation.validateIpAddress(map, fieldName);
        Assert.assertEquals(isValid, errors.isEmpty());
    }

    private void assertNotNullOrEmpty(List<String> values, List<Boolean> isValids) {
        Map<String, String> map = new HashMap<>();
        fillValues(values, map);
        List<Tuple2<String, String>> errors = CommonValidation.validateNotNullOrEmpty(map, fieldName);
        assertValues(errors, isValids);
    }

    private void assertNotNullOrEmpty(String value, boolean isValid) {
        Map<String, String> map = new HashMap<>();
        map.put(fieldName, value);
        List<Tuple2<String, String>> errors = CommonValidation.validateNotNullOrEmpty(map, fieldName);
        Assert.assertEquals(isValid, errors.isEmpty());
    }

    private void assertValues(List<Tuple2<String, String>> errors, List<Boolean> isValids) {
        for (int i = 0; i < isValids.size(); ++i) {
            String positionFieldName = fieldName + "[" + i + "]";
            boolean hasError = errors.stream().filter(it -> it.getA().equals(positionFieldName)).findAny().isPresent();
            Assert.assertEquals("Field " + positionFieldName + " is not as expected", isValids.get(i), !hasError);
        }
    }

    private void fillValues(List<String> values, Map<String, String> map) {
        for (int i = 0; i < values.size(); ++i) {
            map.put(fieldName + "[" + i + "]", values.get(i));
        }
    }

    @Test
    public void testValidateAlphaNum() {
        assertAlphaNum("abc", true);
        assertAlphaNum("ab.c", true);
        assertAlphaNum("ab_c", true);
        assertAlphaNum("ab c", false);
        assertAlphaNum("ab+c", false);
        assertAlphaNum("ab*c", false);

        assertAlphaNum(Arrays.asList("abc", "ab*c"), Arrays.asList(true, false));
    }

    @Test
    public void testValidateCronTime() {
        assertCronTime("* * * * *", true);

        assertCronTime("0 * * * *", true);
        assertCronTime("59 * * * *", true);
        assertCronTime("*/15 * * * *", true);

        assertCronTime("* 0 * * *", true);
        assertCronTime("* 23 * * *", true);
        assertCronTime("* */15 * * *", true);

        assertCronTime("* * 1 * *", true);
        assertCronTime("* * 31 * *", true);
        assertCronTime("* * */2 * *", true);

        assertCronTime("* * * 1 *", true);
        assertCronTime("* * * 12 *", true);
        assertCronTime("* * * */2 *", true);

        assertCronTime("* * * * 0", true);
        assertCronTime("* * * * 6", true);
        assertCronTime("* * * * */2", true);

        assertCronTime("abc", false);
        assertCronTime("60 * * * *", false);
        assertCronTime("* 24 * * *", false);
        assertCronTime("* * 0 * *", false);
        assertCronTime("* * 32 * *", false);
        assertCronTime("* * * 0 *", false);
        assertCronTime("* * * 13 *", false);
        assertCronTime("* * * * 7", false);
        assertCronTime("* * *", false);

        assertCronTime("1,11,21,31,41,51 * * * *", true);
        assertCronTime("1,11,21,31,41, * * * *", false);
        assertCronTime("1, * * * *", false);
        assertCronTime("1,11,21,31,41,51,61 * * * *", false);

        assertCronTime(Arrays.asList("* * * * *", "1,11,21,31,41, * * * *"), Arrays.asList(true, false));
    }

    @Test
    public void testValidateDomainName() {
        assertDomainName("example.com", true);
        assertDomainName("www.example.com", true);
        assertDomainName("123.example.com", true);
        assertDomainName("www.example-is-good.com", true);
        assertDomainName("www.éxample.com", true);

        assertDomainName(".example.com", false);
        assertDomainName("www.exampl/e.com", false);
        assertDomainName("www..com", false);
        assertDomainName("", false);
        assertDomainName("www.example@good.com", false);
        assertDomainName("www.example good.com", false);

        assertDomainName("amazonses.test-email-send.test.example.com", true);
        assertDomainName("_amazonses.test-email-send.test.example.com", true);
        assertDomainName("sss._domainkey.test-email-send.test.example.com", true);

        assertDomainName(Arrays.asList("example.com", "www.exampl/e.com"), Arrays.asList(true, false));
    }

    @Test
    public void testValidateEmail() {
        assertEmail("abc@example.com", true);
        assertEmail("ab_c@example.com", true);
        assertEmail("ab.c@example.com", true);
        assertEmail("abc@test.example.com", true);

        assertEmail("abc", false);
        assertEmail("abc@example@com", false);

        assertEmail(Arrays.asList("abc@example.com", "abc@example@com"), Arrays.asList(true, false));
    }

    @Test
    public void testValidateInEnum() {
        assertInEnum("A", true, DnsEntryType.values());
        assertInEnum("CNAME", true, DnsEntryType.values());
        assertInEnum("a", false, DnsEntryType.values());
        assertInEnum("i", false, DnsEntryType.values());

        assertInEnum(Arrays.asList("A", "i"), Arrays.asList(true, false), DnsEntryType.values());
    }

    @Test
    public void testValidateIpAddress() {
        assertIpAddress("127.0.0.1", true);
        assertIpAddress("0.0.0.0", true);
        assertIpAddress("255.255.255.255", true);
        assertIpAddress("192.168.0.10", true);

        assertIpAddress("192.168.0", false);
        assertIpAddress("192.168.0.aaa", false);
        assertIpAddress("hello", false);
        assertIpAddress("192.168.0.10.1", false);
        assertIpAddress("255.255.255.256", false);

        assertIpAddress(Arrays.asList("127.0.0.1", "255.255.255.256"), Arrays.asList(true, false));
    }

    @Test
    public void testValidateNotNullOrEmpty() {
        assertNotNullOrEmpty("abc", true);
        assertNotNullOrEmpty("10", true);
        assertNotNullOrEmpty("", false);
        assertNotNullOrEmpty(null, false);

        assertNotNullOrEmpty(Arrays.asList("abc", null), Arrays.asList(true, false));
    }

}
