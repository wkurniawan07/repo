package teammates.common.util;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import teammates.common.datatransfer.questions.FeedbackQuestionDetails;
import teammates.common.datatransfer.questions.FeedbackQuestionType;
import teammates.common.datatransfer.questions.FeedbackResponseDetails;

/**
 * Provides means to handle, manipulate, and convert JSON objects to/from strings.
 */
public final class JsonUtils {

    private JsonUtils() {
        // utility class
    }

    /**
     * This creates a Gson object that can handle the Date format we use in the
     * Json file and also reformat the Json string in pretty-print format.
     */
    private static Gson getTeammatesGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new TeammatesInstantAdapter())
                .registerTypeAdapter(ZoneId.class, new TeammatesZoneIdAdapter())
                .registerTypeAdapter(Duration.class, new TeammatesDurationMinutesAdapter())
                .registerTypeAdapter(FeedbackQuestionDetails.class, new TeammatesFeedbackQuestionDetailsAdapter())
                .registerTypeAdapter(FeedbackResponseDetails.class, new TeammatesFeedbackResponseDetailsAdapter())
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    /**
     * Serializes the specified object into its equivalent JSON string.
     *
     * @see Gson#toJson(Object, Type)
     */
    public static String toJson(Object src, Type typeOfSrc) {
        return getTeammatesGson().toJson(src, typeOfSrc);
    }

    /**
     * Serializes the specified object into its equivalent JSON string.
     *
     * @see Gson#toJson(Object)
     */
    public static String toJson(Object src) {
        return getTeammatesGson().toJson(src);
    }

    /**
     * Deserializes the specified JSON string into an object of the specified type.
     *
     * @see Gson#fromJson(String, Type)
     */
    public static <T> T fromJson(String json, Type typeOfT) {
        return getTeammatesGson().fromJson(json, typeOfT);
    }

    /**
     * Parses the specified JSON string into a {@link JsonElement} object.
     *
     * @see JsonParser#parseString(String)
     */
    public static JsonElement parse(String json) {
        return JsonParser.parseString(json);
    }

    private static class TeammatesInstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

        @Override
        public JsonElement serialize(Instant instant, Type type, JsonSerializationContext context) {
            synchronized (this) {
                return new JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(instant));
            }
        }

        @Override
        public Instant deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
            synchronized (this) {
                return Instant.parse(element.getAsString());
            }
        }
    }

    private static class TeammatesZoneIdAdapter implements JsonSerializer<ZoneId>, JsonDeserializer<ZoneId> {

        @Override
        public JsonElement serialize(ZoneId zoneId, Type type, JsonSerializationContext context) {
            synchronized (this) {
                return new JsonPrimitive(zoneId.getId());
            }
        }

        @Override
        public ZoneId deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
            synchronized (this) {
                return ZoneId.of(element.getAsString());
            }
        }
    }

    private static class TeammatesDurationMinutesAdapter implements JsonSerializer<Duration>, JsonDeserializer<Duration> {

        @Override
        public JsonElement serialize(Duration duration, Type type, JsonSerializationContext context) {
            synchronized (this) {
                return new JsonPrimitive(duration.toMinutes());
            }
        }

        @Override
        public Duration deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
            synchronized (this) {
                return Duration.ofMinutes(element.getAsLong());
            }
        }
    }

    private static class TeammatesFeedbackResponseDetailsAdapter implements JsonSerializer<FeedbackResponseDetails>,
            JsonDeserializer<FeedbackResponseDetails> {

        @Override
        public JsonElement serialize(FeedbackResponseDetails src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src, src.getQuestionType().getResponseDetailsClass());
        }

        @Override
        public FeedbackResponseDetails deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            FeedbackQuestionType questionType =
                    FeedbackQuestionType.valueOf(json.getAsJsonObject().get("questionType").getAsString());
            return context.deserialize(json, questionType.getResponseDetailsClass());
        }

    }

    private static class TeammatesFeedbackQuestionDetailsAdapter implements JsonSerializer<FeedbackQuestionDetails>,
            JsonDeserializer<FeedbackQuestionDetails> {

        @Override
        public JsonElement serialize(FeedbackQuestionDetails src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src, src.getQuestionType().getQuestionDetailsClass());
        }

        @Override
        public FeedbackQuestionDetails deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            FeedbackQuestionType questionType =
                    FeedbackQuestionType.valueOf(json.getAsJsonObject().get("questionType").getAsString());
            return context.deserialize(json, questionType.getQuestionDetailsClass());
        }
    }
}
