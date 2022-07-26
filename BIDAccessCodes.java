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
import com.bidsdk.model.BIDTenantInfo;
import com.bidsdk.model.BIDRequestEmailVerificationLinkResponse;
import com.bidsdk.utils.WTM;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

public class BIDAccessCodes {
    public static BIDRequestEmailVerificationLinkResponse requestEmailVerificationLink(BIDTenantInfo tenantInfo, String emailTo, String emailTemplateB64OrNull, String emailSubjectOrNull, String createdBy, String ttl_seconds_or_null) {
    	BIDRequestEmailVerificationLinkResponse ret = null;
        try {
            if (emailTo == null || emailTo.length() == 0) {
            	BIDRequestEmailVerificationLinkResponse errorData = new BIDRequestEmailVerificationLinkResponse();
            	errorData.statusCode = 400;
            	errorData.message = "emailTo is required parameter";
                return errorData;
            }
            
            BIDCommunityInfo communityInfo = BIDTenant.getInstance().getCommunityInfo(tenantInfo);
            BIDKeyPair keySet = BIDTenant.getInstance().getKeySet();
            String licenseKey = tenantInfo.licenseKey;
            BIDSD sd = BIDTenant.getInstance().getSD(tenantInfo);

            String communityPublicKey = communityInfo.community.publicKey;

            String sharedKey = BIDECDSA.createSharedKey(keySet.privateKey, communityPublicKey);

            Map < String, String > headers = WTM.defaultHeaders();
            headers.put("licensekey", BIDECDSA.encrypt(licenseKey, sharedKey));
            headers.put("X-tenantTag", communityInfo.tenant.tenanttag);
            headers.put("requestid", BIDECDSA.encrypt(new Gson().toJson(WTM.makeRequestId()), sharedKey));
            headers.put("publickey", keySet.publicKey);

            Map<String, Object> body = new HashMap<>();
            body.put("createdBy", createdBy);
            body.put("version", "v0");
            body.put("type", "verification_link");
            body.put("emailTo", emailTo);

            if (ttl_seconds_or_null != null) {
                body.put("ttl_seconds", ttl_seconds_or_null);
            }
            
            if (emailTemplateB64OrNull != null) {
                body.put("emailTemplateB64", emailTemplateB64OrNull);
            }
            
            if (emailSubjectOrNull != null) {
                body.put("emailSubject", emailSubjectOrNull);
            }

            String enc_data = BIDECDSA.encrypt(new Gson().toJson(body), sharedKey);

            Map<String, Object> data = new HashMap<>();
            data.put("data", enc_data);

            Map < String, Object > response = WTM.execute("put",
                sd.adminconsole + "/api/r2/acr/community/" + communityInfo.community.name + "/code",
                headers,
                new Gson().toJson(data)
            );
            
            int statusCode = (Integer) response.get("status");
            
            String responseStr = (String) response.get("response");

            ret = new Gson().fromJson(responseStr, BIDRequestEmailVerificationLinkResponse.class);
            
            ret.statusCode = statusCode;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static BIDFetchAccessCodeResponse fetchAccessCode(BIDTenantInfo tenantInfo, String code) {
   	BIDFetchAccessCodeResponse ret = null;
       try {
           BIDCommunityInfo communityInfo = BIDTenant.getInstance().getCommunityInfo(tenantInfo);
           BIDKeyPair keySet = BIDTenant.getInstance().getKeySet();
           String licenseKey = tenantInfo.licenseKey;
           BIDSD sd = BIDTenant.getInstance().getSD(tenantInfo);

           String communityPublicKey = communityInfo.community.publicKey;

           String sharedKey = BIDECDSA.createSharedKey(keySet.privateKey, communityPublicKey);

           Map < String, String > headers = WTM.defaultHeaders();
           headers.put("licensekey", BIDECDSA.encrypt(licenseKey, sharedKey));
           headers.put("X-tenantTag", communityInfo.tenant.tenanttag);
           headers.put("requestid", BIDECDSA.encrypt(new Gson().toJson(WTM.makeRequestId()), sharedKey));
           headers.put("publickey", keySet.publicKey);

           Map < String, Object > response = WTM.execute("get",
           	sd.adminconsole + "/api/r1/acr/community/" + communityInfo.community.name + "/" + code,
               headers,
               null
           );
           
           int statusCode = (Integer) response.get("status");  

           String responseStr = (String) response.get("response");

           ret = new Gson().fromJson(responseStr, BIDFetchAccessCodeResponse.class);
           
           if (ret.data != null) {
               String dec_data = BIDECDSA.decrypt(ret.data, sharedKey);
               System.out.println("dec_data:::::" + dec_data);
               ret = new Gson().fromJson(dec_data, BIDFetchAccessCodeResponse.class);
           }
           
       } catch (Exception e) {
           e.printStackTrace();
       }
       return ret;
   }

   public static BIDRedeemEmailVerificationCodeResponse verifyAndRedeemEmailVerificationCode(BIDTenantInfo tenantInfo, String code) {
    BIDRedeemEmailVerificationCodeResponse ret = null;
    try {
        BIDCommunityInfo communityInfo = BIDTenant.getInstance().getCommunityInfo(tenantInfo);
        BIDKeyPair keySet = BIDTenant.getInstance().getKeySet();
        String licenseKey = tenantInfo.licenseKey;
        BIDSD sd = BIDTenant.getInstance().getSD(tenantInfo);

        String communityPublicKey = communityInfo.community.publicKey;

        BIDFetchAccessCodeResponse access_code_response =  fetchAccessCode(tenantInfo, code);

        if (access_code_response.statusCode !== 200) {
            return access_code_response;
        }

        if (access_code_response.type != "verification_link") {
            BIDRequestEmailVerificationLinkResponse errorData = new BIDRequestEmailVerificationLinkResponse();
            	errorData.statusCode = 400;
            	errorData.message = "Provided verification code is invalid type";
                return errorData;
        }

        String sharedKey = BIDECDSA.createSharedKey(keySet.privateKey, communityPublicKey);

        Map < String, String > headers = WTM.defaultHeaders();
        headers.put("licensekey", BIDECDSA.encrypt(licenseKey, sharedKey));
        headers.put("X-tenantTag", communityInfo.tenant.tenanttag);
        headers.put("requestid", BIDECDSA.encrypt(new Gson().toJson(WTM.makeRequestId()), sharedKey));
        headers.put("publickey", keySet.publicKey);

        Map < String, Object > response = WTM.execute("post",
            sd.adminconsole + "/api/r1/acr/community/" + communityInfo.community.name + "/" + code + "/redeem",
            headers,
            null
        );
        
        int statusCode = (Integer) response.get("status");  

        String responseStr = (String) response.get("response");

        ret = new Gson().fromJson(responseStr, BIDRedeemEmailVerificationCodeResponse.class);
        
        if (statusCode === 200) {
            ret.status = "redeemed";
        }
        
    } catch (Exception e) {
        e.printStackTrace();
    }
    return ret;
}
}
