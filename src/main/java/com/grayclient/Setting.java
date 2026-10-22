package com.grayclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public abstract class Setting<T> {
    public final String name;
    protected T value;

    protected Setting(String name, T def) { this.name = name; this.value = def; }

    public T get() { return value; }
    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement e);

    public static class Bool extends Setting<Boolean> {
        public Bool(String name, boolean def) { super(name, def); }
        public boolean on() { return value; }
        public void toggle() { value = !value; }
        @Override public JsonElement toJson() { return new JsonPrimitive(value); }
        @Override public void fromJson(JsonElement e) {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) value = e.getAsBoolean();
        }
    }

    public static class Num extends Setting<Double> {
        public final double min, max, step;
        public Num(String name, double def, double min, double max, double step) {
            super(name, def); this.min = min; this.max = max; this.step = step;
        }
        public double d() { return value; }
        public int i() { return (int) Math.round(value); }
        public void set(double v) {
            if (!Double.isFinite(v)) return;
            v = Math.max(min, Math.min(max, v));
            v = Math.round((v - min) / step) * step + min;
            value = Math.max(min, Math.min(max, Math.round(v * 1000.0) / 1000.0));
        }
        public void inc() { set(value + step); }
        public void dec() { set(value - step); }
        public String display() {
            if (step >= 1) return String.valueOf(i());
            if (step >= 0.1) return String.format(Locale.ROOT, "%.1f", value);
            return String.format(Locale.ROOT, "%.2f", value);
        }
        @Override public JsonElement toJson() { return new JsonPrimitive(value); }
        @Override public void fromJson(JsonElement e) {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) set(e.getAsDouble());
        }
    }
}
