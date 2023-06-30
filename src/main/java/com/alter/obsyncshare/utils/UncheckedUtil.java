package com.alter.obsyncshare.utils;

import java.nio.file.Files;
import java.nio.file.Path;

public class UncheckedUtil {

    public static void main(String[] args) {
        Integer unchecked = unchecked(() -> 111);

        unchecked(() -> main(null));

    }

    public static <T> T unchecked(Call<T> function) throws RuntimeException {
        try {
            return function.apply();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void unchecked(Call2 function) throws RuntimeException {
        try {
            function.apply();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface Call<T> {
        T apply() throws Throwable;
    }

    @FunctionalInterface
    interface Call2 {
        void apply() throws Throwable;
    }

}
