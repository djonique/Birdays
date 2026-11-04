/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.models;

public class Separator implements Item {

    public static final int TYPE_JANUARY = 0;
    public static final int TYPE_FEBRUARY = 1;
    public static final int TYPE_MARCH = 2;
    public static final int TYPE_APRIL = 3;
    public static final int TYPE_MAY = 4;
    public static final int TYPE_JUNE = 5;
    public static final int TYPE_JULY = 6;
    public static final int TYPE_AUGUST = 7;
    public static final int TYPE_SEPTEMBER = 8;
    public static final int TYPE_OCTOBER = 9;
    public static final int TYPE_NOVEMBER = 10;
    public static final int TYPE_DECEMBER = 11;

    private int type;

    public Separator(int type) {
        this.type = type;
    }

    public boolean isPerson() {
        return false;
    }

    public int getType() {
        return type;
    }
}