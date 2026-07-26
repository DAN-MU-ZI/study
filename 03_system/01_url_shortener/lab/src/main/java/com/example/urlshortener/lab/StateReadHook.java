package com.example.urlshortener.lab;

@FunctionalInterface
public interface StateReadHook {

    StateReadHook NONE = () -> {
    };

    void afterRead();
}
