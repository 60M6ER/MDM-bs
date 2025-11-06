package ru.baikalsr.backend.Security.model;

public record AuthResult(String jwt, long ttlSec) {}
