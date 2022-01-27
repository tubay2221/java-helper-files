/**
 * Copyright (c) 2018, 1Kosmos Inc. All rights reserved.
 * Licensed under 1Kosmos Open Source Public License version 1.0 (the "License");
 * You may not use this file except in compliance with the License. 
 * You may obtain a copy of this license at 
 *    https://github.com/1Kosmos/1Kosmos_License/blob/main/LICENSE.txt
 */
package com.bidsdk.utils;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemCache {

    private HashMap<String, ACacheItem> map = new HashMap<String, ACacheItem>();
    private static InMemCache shared = new InMemCache();
    private InMemCache() {

    }

    public static InMemCache getInstance() {
        return shared;
    }

    protected class ACacheItem {
        String value;
        long created = System.currentTimeMillis();
        long ttl = 60 * 1000;//default 1 min. ttl

        public ACacheItem(String value) {
            this.value = value;
        }

        public ACacheItem(String value, long ttl) {
            this.value = value;
            this.ttl = ttl;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - this.created > this.ttl;
        }
    }

    private void cleanup() {
        synchronized (map) {
            List<String> toRemoveList = new ArrayList<>();
            for(Map.Entry<String, ACacheItem> entry : map.entrySet()) {
                if (entry.getValue().isExpired()) {
                    toRemoveList.add(entry.getKey());
                }
            }

            for (String toRemove : toRemoveList) {
                map.remove(toRemove);
            }
        }
    }

    public void set(String key, String value) {
        synchronized (map) {
            if (value == null) {
                map.remove(key);
            }
            else {
                map.put(key, new ACacheItem(value));
            }

            cleanup();

        }
    }

    public void set(String key, String value, long ttl) {
        synchronized (map) {
            if (value == null) {
                map.remove(key);
            }
            else {
                map.put(key, new ACacheItem(value, ttl));
            }
            cleanup();
        }
    }

    public String get(String key) {
        synchronized (map) {
            try {
                ACacheItem item = map.get(key);
                if (item.isExpired()) {
                    map.remove(key);
                    item = null;
                }
                return item != null ? item.value : null;
            }
            catch (Exception e) {

            }

            return null;
        }
    }


}
