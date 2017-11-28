package com.casc.rfidscanner.backend;

import com.casc.rfidscanner.bean.Tag;

public interface TagCache {

    void init();

    void insert(Tag tag);

    int totalTagCount();

    int cachedTagCount();

    int storedTagCount();
}
