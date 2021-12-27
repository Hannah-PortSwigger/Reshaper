package synfron.reshaper.burp.core.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import synfron.reshaper.burp.core.exceptions.WrappedException;
import synfron.reshaper.burp.core.vars.VariableString;

import java.io.IOException;
import java.util.stream.Stream;

public class Serializer {
    private static ObjectMapper objectMapper;

    private static ObjectMapper configureMapper(JsonDeserializer<?>[] deserializers) {
        return configureMapper(new JsonSerializer[0], deserializers);
    }

    private static ObjectMapper configureMapper(JsonSerializer<?>[] serializers) {
        return configureMapper(serializers, new JsonDeserializer[0]);
    }

    private static ObjectMapper configureMapper() {
        return configureMapper(new JsonSerializer[0], new JsonDeserializer[0]);
    }

    private static ObjectMapper configureMapper(JsonSerializer<?>[] serializers, JsonDeserializer<?>[] deserializers) {
        JsonMapper.Builder builder = JsonMapper.builder();
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        builder.configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false);
        builder.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        builder.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        builder.visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        builder.visibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        builder.visibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        builder.visibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.NONE);
        if (serializers.length > 0 || deserializers.length > 0) {
            SimpleModule module = new SimpleModule();
            Stream.of(serializers).forEach(module::addSerializer);
            Stream.of(deserializers).forEach(deserializer -> addDeserializer(module, deserializer.handledType(), deserializer));
            builder.addModule(module);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> void addDeserializer(SimpleModule module, Class<T> type, JsonDeserializer<?> deserializer) {
        module.addDeserializer(type, (JsonDeserializer<? extends T>)deserializer);
    }

    private static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            ObjectMapper objectMapper = configureMapper(new JsonDeserializer[] { new VariableStringDeserializer() });
            SimpleModule module = new SimpleModule();
            module.addDeserializer(VariableString.class, new VariableStringDeserializer());
            objectMapper.registerModule(module);

            Serializer.objectMapper = objectMapper;
        }
        return objectMapper;
    }

    @SuppressWarnings("unchecked")
    public static <T> T copy(T source) {
        return deserialize(serialize(source, false), (Class<T>)source.getClass());
    }

    public static String serialize(Object value, boolean prettyPrint) {
        try  {
            return (prettyPrint ?
                    getObjectMapper().writer().withDefaultPrettyPrinter() :
                    getObjectMapper().writer()
            ).writeValueAsString(value);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
    }

    public static <T> T deserialize(String json, TypeReference<T> typeReference) {
        if (json == null) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, typeReference);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
    }

    private static class VariableStringDeserializer extends JsonDeserializer<VariableString> {
        @Override
        public Class<VariableString> handledType() {
            return VariableString.class;
        }

        @Override
        public VariableString deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            ObjectMapper objectMapper = configureMapper();
            return (parser.currentTokenId() == JsonTokenId.ID_STRING) ?
                    VariableString.getAsVariableString(parser.getText()) :
                    objectMapper.readValue(parser, VariableString.class);
        }
    }
}
