package dev.cerus.hourlycatbot.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JsonBuilder<T extends JsonElement> {

    protected final T element;

    private JsonBuilder(final Supplier<T> constructor) {
        this.element = constructor.get();
    }

    public static ObjectBuilder newObjectBuilder() {
        return new ObjectBuilder();
    }

    public static ArrayBuilder newArrayBuilder() {
        return new ArrayBuilder();
    }

    public T build() {
        return this.element;
    }

    public static class ObjectBuilder extends JsonBuilder<JsonObject> {

        public ObjectBuilder() {
            super(JsonObject::new);
        }

        public ObjectBuilder set(final String key, final JsonBuilder<?> builder) {
            return this.set(key, builder.build());
        }

        public ObjectBuilder set(final String key, final JsonElement element) {
            this.element.add(key, element);
            return this;
        }

        public <E extends JsonElement> ObjectBuilder set(final String key, final Supplier<E> constructor, final Consumer<JsonBuilder<E>> consumer) {
            final JsonBuilder<E> builder = new JsonBuilder<>(constructor);
            consumer.accept(builder);
            return this.set(key, builder.build());
        }

    }

    public static class ArrayBuilder extends JsonBuilder<JsonArray> {

        public ArrayBuilder() {
            super(JsonArray::new);
        }

        public ArrayBuilder add(final JsonBuilder<?> builder) {
            return this.add(builder.build());
        }

        public ArrayBuilder add(final JsonElement element) {
            this.element.add(element);
            return this;
        }

        public <E extends JsonElement> ArrayBuilder add(final Supplier<E> constructor, final Consumer<JsonBuilder<E>> consumer) {
            final JsonBuilder<E> builder = new JsonBuilder<>(constructor);
            consumer.accept(builder);
            return this.add(builder.build());
        }

    }

}
