package com.example.cinema_middleware.v1.support.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import java.util.Arrays;

public class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToEnumConverter<>(targetType);
    }

    private record StringToEnumConverter<T extends Enum>(Class<T> enumType) implements
                Converter<String, T> {

        @Override
            public T convert(String source) {
                if (source.isBlank()) {
                    return null;
                }

                return (T) Arrays.stream(enumType.getEnumConstants())
                        .filter(e -> ((Enum<?>) e).name().equalsIgnoreCase(source.trim()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "%s 값은 %s에 존재하지 않습니다.".formatted(source,
                                        enumType.getSimpleName())));
            }
        }
}
