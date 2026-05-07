package com.tooba.EduEvent.pattern;

public class NullUser implements UserInterface {

    @Override
    public Long getId() {
        return -1L; // Safe default ID
    }

    @Override
    public String getName() {
        return "Anonymous";
    }

    @Override
    public String getEmail() {
        return "not_available@eduevent.com";
    }

    @Override
    public String getRole() {
        return "GUEST";
    }

    @Override
    public boolean isNull() {
        return true; // This is a Null Object
    }
}