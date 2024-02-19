package dev.cerus.hourlycatbot.net;

import java.util.Optional;
import java.util.function.Function;

public class Result<T> {

    private final T data;
    private final Error error;

    public Result(final T data, final Error error) {
        this.data = data;
        this.error = error;
    }

    public static <T> Result<T> successful(final T data) {
        return new Result<>(data, null);
    }

    public static <T> Result<T> failed(final Error error) {
        return new Result<>(null, error);
    }

    public <O> Result<O> map(final Function<T, O> mapper) {
        return new Result<>(this.data == null ? null : mapper.apply(this.data), this.error);
    }

    public Optional<T> optionalData() {
        return Optional.ofNullable(this.data);
    }

    public Optional<Error> optionalError() {
        return Optional.ofNullable(this.error);
    }

    public boolean isErroneous() {
        return this.getError() != null;
    }

    public T getData() {
        return this.data;
    }

    public Error getError() {
        return this.error;
    }

}
