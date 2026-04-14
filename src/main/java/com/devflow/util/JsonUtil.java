package com.devflow.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                    (json, type, ctx) -> LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_DATE_TIME))
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (src, type, ctx) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_DATE_TIME)))
            .create();

    private JsonUtil() {}

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> List<T> fromJsonList(String json, Class<T> elementClass) {
        Type type = TypeToken.getParameterized(List.class, elementClass).getType();
        return GSON.fromJson(json, type);
    }

    public static Gson gson() {
        return GSON;
    }
}
