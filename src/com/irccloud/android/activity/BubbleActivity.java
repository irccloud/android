package com.irccloud.android.activity;

import android.os.Bundle;

public class BubbleActivity extends MainActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.bubble = true;
        super.onCreate(savedInstanceState);
    }
}
