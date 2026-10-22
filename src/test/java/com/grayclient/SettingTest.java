package com.grayclient;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingTest {
    @Test void corruptNumericInputCannotChangeWorkingValue() {
        Setting.Num setting = new Setting.Num("Range", 3, 1, 6, 0.1);
        setting.set(Double.NaN);
        setting.set(Double.POSITIVE_INFINITY);
        setting.set(Double.NEGATIVE_INFINITY);
        setting.fromJson(new JsonPrimitive("invalid"));
        setting.fromJson(JsonNull.INSTANCE);
        assertEquals(3, setting.d());
    }

    @Test void numbersRemainWithinBoundsAfterRounding() {
        Setting.Num setting = new Setting.Num("Uneven interval", 0, 0, 1, 0.6);
        setting.set(100);
        assertEquals(1, setting.d());
        setting.set(-100);
        assertEquals(0, setting.d());
    }

    @Test void decimalStepsDoNotAccumulateFloatingPointError() {
        Setting.Num setting = new Setting.Num("Cooldown", 0.92, 0.5, 1, 0.01);
        for (int i = 0; i < 8; i++) setting.inc();
        assertEquals(1, setting.d());
        for (int i = 0; i < 8; i++) setting.dec();
        assertEquals(0.92, setting.d());
    }

    @Test void booleanSettingsRejectWrongJsonTypes() {
        Setting.Bool setting = new Setting.Bool("Enabled", true);
        setting.fromJson(new JsonPrimitive("false"));
        setting.fromJson(new JsonPrimitive(0));
        setting.fromJson(JsonNull.INSTANCE);
        assertTrue(setting.on());
        setting.fromJson(new JsonPrimitive(false));
        assertFalse(setting.on());
    }

    @Test void savedValuesRoundTrip() {
        Setting.Num source = new Setting.Num("Speed", 4.5, 1, 6, 0.5);
        Setting.Num restored = new Setting.Num("Speed", 1, 1, 6, 0.5);
        restored.fromJson(source.toJson());
        assertEquals(source.d(), restored.d());
        Setting.Bool first = new Setting.Bool("Flag", false);
        Setting.Bool second = new Setting.Bool("Flag", true);
        second.fromJson(first.toJson());
        assertFalse(second.on());
    }
}
