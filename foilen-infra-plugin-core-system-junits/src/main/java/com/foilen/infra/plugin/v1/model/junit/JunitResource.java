/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.junit;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;

public class JunitResource extends AbstractIPResource implements IPResource {

    public static final String PROPERTY_BOOL = "bool";
    public static final String PROPERTY_DATE = "date";
    public static final String PROPERTY_DOUBLE_NUMBER = "doubleNumber";
    public static final String PROPERTY_ENUMERATION = "enumeration";
    public static final String PROPERTY_FLOAT_NUMBER = "floatNumber";
    public static final String PROPERTY_INTEGER_NUMBER = "integerNumber";
    public static final String PROPERTY_LONG_NUMBER = "longNumber";
    public static final String PROPERTY_TEXT = "text";
    public static final String PROPERTY_SET_DATES = "setDates";
    public static final String PROPERTY_SET_DOUBLES = "setDoubles";
    public static final String PROPERTY_SET_ENUMERATIONS = "setEnumerations";
    public static final String PROPERTY_SET_FLOATS = "setFloats";
    public static final String PROPERTY_SET_LONGS = "setLongs";
    public static final String PROPERTY_SET_INTEGERS = "setIntegers";
    public static final String PROPERTY_SET_TEXTS = "setTexts";

    private String text;
    private JunitResourceEnum enumeration;
    private Date date;
    private Integer integerNumber;
    private long longNumber;
    private Double doubleNumber;
    private Float floatNumber;
    private boolean bool;

    private Set<Date> setDates = new HashSet<>();
    private Set<Double> setDoubles = new HashSet<>();
    private Set<JunitResourceEnum> setEnumerations = new HashSet<>();
    private Set<Float> setFloats = new HashSet<>();
    private Set<Long> setLongs = new HashSet<>();
    private Set<Integer> setIntegers = new HashSet<>();
    private Set<String> setTexts = new HashSet<>();

    public JunitResource() {
    }

    public JunitResource(String text) {
        this.text = text;
    }

    public JunitResource(String text, JunitResourceEnum enumeration, Date date, Integer integerNumber, long longNumber, Double doubleNumber, Float floatNumber, boolean bool, String... setTexts) {
        this.text = text;
        this.enumeration = enumeration;
        this.date = date;
        this.integerNumber = integerNumber;
        this.longNumber = longNumber;
        this.doubleNumber = doubleNumber;
        this.floatNumber = floatNumber;
        this.bool = bool;
        this.setTexts = Arrays.asList(setTexts).stream().collect(Collectors.toSet());
    }

    public JunitResource(String text, JunitResourceEnum enumeration, Integer integerNumber) {
        this.text = text;
        this.enumeration = enumeration;
        this.integerNumber = integerNumber;
    }

    @Override
    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) {
            return false;
        }

        JunitResource se = (JunitResource) o;
        return Objects.equals(text, se.text) //
                && Objects.equals(enumeration, se.enumeration) //
                && Objects.equals(date, se.date) //
                && Objects.equals(integerNumber, se.integerNumber) //
                && Objects.equals(longNumber, se.longNumber) //
                && Objects.equals(doubleNumber, se.doubleNumber) //
                && Objects.equals(floatNumber, se.floatNumber) //
                && Objects.equals(bool, se.bool) //
                && Objects.equals(setTexts, se.setTexts);
    }

    public Date getDate() {
        return date;
    }

    public Double getDoubleNumber() {
        return doubleNumber;
    }

    public JunitResourceEnum getEnumeration() {
        return enumeration;
    }

    public Float getFloatNumber() {
        return floatNumber;
    }

    public Integer getIntegerNumber() {
        return integerNumber;
    }

    public long getLongNumber() {
        return longNumber;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.INFRASTRUCTURE;
    }

    @Override
    public String getResourceDescription() {
        return toString();
    }

    @Override
    public String getResourceName() {
        return text;
    }

    public Set<Date> getSetDates() {
        return setDates;
    }

    public Set<Double> getSetDoubles() {
        return setDoubles;
    }

    public Set<JunitResourceEnum> getSetEnumerations() {
        return setEnumerations;
    }

    public Set<Float> getSetFloats() {
        return setFloats;
    }

    public Set<Integer> getSetIntegers() {
        return setIntegers;
    }

    public Set<Long> getSetLongs() {
        return setLongs;
    }

    public Set<String> getSetTexts() {
        return setTexts;
    }

    public String getText() {
        return text;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, enumeration, date, integerNumber, longNumber);
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDoubleNumber(Double doubleNumber) {
        this.doubleNumber = doubleNumber;
    }

    public void setEnumeration(JunitResourceEnum enumeration) {
        this.enumeration = enumeration;
    }

    public void setFloatNumber(Float floatNumber) {
        this.floatNumber = floatNumber;
    }

    public void setIntegerNumber(Integer integerNumber) {
        this.integerNumber = integerNumber;
    }

    public void setLongNumber(long longNumber) {
        this.longNumber = longNumber;
    }

    public void setSetDates(Set<Date> setDates) {
        this.setDates = setDates;
    }

    public void setSetDoubles(Set<Double> setDoubles) {
        this.setDoubles = setDoubles;
    }

    public void setSetEnumerations(Set<JunitResourceEnum> setEnumerations) {
        this.setEnumerations = setEnumerations;
    }

    public void setSetFloats(Set<Float> setFloats) {
        this.setFloats = setFloats;
    }

    public void setSetIntegers(Set<Integer> setIntegers) {
        this.setIntegers = setIntegers;
    }

    public void setSetLongs(Set<Long> setLongs) {
        this.setLongs = setLongs;
    }

    public void setSetTexts(Set<String> setTexts) {
        this.setTexts = setTexts;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("JunitResource [text=");
        builder.append(text);
        builder.append(", enumeration=");
        builder.append(enumeration);
        builder.append(", date=");
        builder.append(date);
        builder.append(", integerNumber=");
        builder.append(integerNumber);
        builder.append(", longNumber=");
        builder.append(longNumber);
        builder.append(", doubleNumber=");
        builder.append(doubleNumber);
        builder.append(", floatNumber=");
        builder.append(floatNumber);
        builder.append(", bool=");
        builder.append(bool);
        builder.append(", setDates=");
        builder.append(setDates);
        builder.append(", setDoubles=");
        builder.append(setDoubles);
        builder.append(", setEnumerations=");
        builder.append(setEnumerations);
        builder.append(", setFloats=");
        builder.append(setFloats);
        builder.append(", setLongs=");
        builder.append(setLongs);
        builder.append(", setIntegers=");
        builder.append(setIntegers);
        builder.append(", setTexts=");
        builder.append(setTexts);
        builder.append("]");
        return builder.toString();
    }

}
