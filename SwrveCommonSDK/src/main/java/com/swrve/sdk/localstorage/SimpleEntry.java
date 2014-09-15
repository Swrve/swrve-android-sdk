package com.swrve.sdk.localstorage;

public class SimpleEntry<K, V> implements java.util.Map.Entry<K, V> {

    private K key;
    private V value;

    public SimpleEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V newValue) {
        value = newValue;
        return value;
    }

}
