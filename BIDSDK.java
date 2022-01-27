/**
 * Copyright (c) 2018, 1Kosmos Inc. All rights reserved.
 * Licensed under 1Kosmos Open Source Public License version 1.0 (the "License");
 * You may not use this file except in compliance with the License. 
 * You may obtain a copy of this license at 
 *    https://github.com/1Kosmos/1Kosmos_License/blob/main/LICENSE.txt
 */
package com.bidsdk;

import com.bidsdk.model.BIDCommunityInfo;
import com.bidsdk.model.BIDKeyPair;
import com.bidsdk.model.BIDSD;
import com.bidsdk.model.BIDTenant;
import com.bidsdk.utils.InMemCache;
import com.bidsdk.utils.WTM;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;


import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class BIDSDK {
    private static final BIDSDK instance = new BIDSDK();
    private BIDSDK() {

    }
    public static BIDSDK getInstance() {
        return instance;
    }

    private boolean loaded;
    private BIDTenant tenant;
    private BIDCommunityInfo communityInfo;
    private String licenseKey;
    private BIDKeyPair keySet;
    private BIDSD sd;


    public boolean isLoaded() {
        return loaded;
    }

    public void setupTenant(BIDTenant tenant, String license) {
        loaded = false;

        communityInfo = null;
        licenseKey = null;
        sd = null;

        this.tenant = tenant;
        this.licenseKey = license;

        if (this.keySet == null) {
            this.keySet = BIDECDSA.generateKeyPair();
        }
        loadCommunityInfo();

        loadSD();
    }

    synchronized
    private void loadCommunityInfo() {
        try {
            communityInfo = null;

            String cache_key = "communityInfo::" + tenant.dns + "::" + tenant.communityName;
            String cache_str = InMemCache.getInstance().get(cache_key);
            if (cache_str != null) {
                communityInfo = new Gson().fromJson(cache_str, BIDCommunityInfo.class);
                return;
            }

            String url = "https://" + tenant.dns + "/api/r1/system/community_info/fetch";

            Map<String, Object> response = WTM.execute("post"
                                            , url
                                            , WTM.defaultHeaders()
                                            , tenant.toString());

            String responseStr = (String) response.get("response");
            int statusCode = (Integer) response.get("status");

            if (statusCode == HttpStatus.SC_OK) {
                InMemCache.getInstance().set(cache_key, responseStr);
                communityInfo = new Gson().fromJson(responseStr, BIDCommunityInfo.class);
            }
            else {
                throw new Exception ("Unable to load communityInfo code" + statusCode + " with message: " + responseStr);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized
    private void loadSD() {
        sd = null;

        try {
            String sdUrl = "https://" + tenant.dns + "/caas/sd";
            String cache_key = sdUrl;

            String cache_str = InMemCache.getInstance().get(cache_key);
            if (cache_str != null) {
                sd = new Gson().fromJson(cache_str, BIDSD.class);
                return;
            }

            Map<String, Object> response = WTM.execute("get"
                                            , sdUrl
                                            , WTM.defaultHeaders()
                                            , null);

            String responseStr = (String) response.get("response");
            int statusCode = (Integer) response.get("status");

            if (statusCode == HttpStatus.SC_OK) {
                sd = new Gson().fromJson(responseStr, BIDSD.class);
                InMemCache.getInstance().set(cache_key, responseStr);
            }
            else {
                throw new Exception ("Unable to load sd code" + statusCode + " with message: " + responseStr);
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BIDTenant getTenant() {
        return tenant;
    }

    public BIDCommunityInfo getCommunityInfo() {
        loadCommunityInfo();
        return communityInfo;
    }

    public BIDSD getSD() {
        loadSD();
        return sd;
    }

    public BIDKeyPair getKeySet() {
        return keySet;
    }

    public void setKeySet(BIDKeyPair keySet) {
        this.keySet = keySet;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

}
