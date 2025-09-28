package com.tntowns.model;

public enum NationRank {
    E1,
    E2,
    E3,
    E4,
    E5,
    E6,
    E7,
    E8,
    E9;

    public static NationRank fromString(String s) {
        try { return NationRank.valueOf(s.toUpperCase()); } catch (Exception ex) { return E1; }
    }
}


