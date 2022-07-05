/**
 * Copyright (c) 2018, 1Kosmos Inc. All rights reserved.
 * Licensed under 1Kosmos Open Source Public License version 1.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of this license at
 *    https://github.com/1Kosmos/1Kosmos_License/blob/main/LICENSE.txt
 */
package com.bidsdk;

import com.bidsdk.BIDTenant;
import com.bidsdk.model.BIDAttestationOptionsResponse;
import com.bidsdk.model.BIDAttestationOptionsValue;
import com.bidsdk.model.BIDAttestationResultData;
import com.bidsdk.model.BIDAttestationResultResponseValue;
import com.bidsdk.model.BIDAttestationResultValue;
import com.bidsdk.model.BIDCommunityInfo;
import com.bidsdk.model.BIDKeyPair;
import com.bidsdk.model.BIDSD;
import com.bidsdk.model.BIDTenantInfo;
import com.bidsdk.model.BIDAssertionOptionValue;
import com.bidsdk.model.BIDAssertionOptionResponse;
import com.bidsdk.model.BIDAssertionResultValue;
import com.bidsdk.model.BIDAssertionResultResponse;
import com.bidsdk.utils.InMemCache;
import com.bidsdk.utils.WTM;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

public class BIDWebAuthn {

  private static String getPublicKey(BIDTenantInfo tenantInfo) {
    String ret = null;
    try {
      BIDSD sd = BIDTenant.getInstance().getSD(tenantInfo);
      String url = sd.webauthn + "/publickeys";

      String cache_key = url;
      String cache_str = InMemCache.getInstance().get(cache_key);
      if (cache_str != null) {
        Map<String, String> map = new Gson().fromJson(cache_str, Map.class);
        ret = map.get("publicKey");
        return ret;
      }

      //load from services
      Map<String, Object> response = WTM.execute("get",
        url,
        WTM.defaultHeaders(),
        null
      );
      String responseStr = (String) response.get("response");

      int statusCode = (Integer) response.get("status");
      if (statusCode == 200) {
        Map<String, String> map = new Gson().fromJson(responseStr, Map.class);
        ret = map.get("publicKey");
        InMemCache.getInstance().set(cache_key, responseStr);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return ret;
  }

  public static BIDAttestationOptionsResponse fetchAttestationOptions(
    BIDTenantInfo tenantInfo, BIDAttestationOptionsValue attestationOptionsRequest
  ) {
    BIDAttestationOptionsResponse ret = null;
    try {
      BIDCommunityInfo communityInfo = BIDTenant.getInstance().getCommunityInfo(tenantInfo);
      BIDKeyPair keySet = BIDTenant.getInstance().getKeySet();
      String licenseKey = tenantInfo.licenseKey;
      BIDSD sd = BIDTenant.getInstance().getSD(tenantInfo);
      String webAuthnPublicKey = getPublicKey(tenantInfo);


      String sharedKey = BIDECDSA.createSharedKey(
        keySet.privateKey,
        webAuthnPublicKey
      );

      Map<String, String> headers = WTM.defaultHeaders();
      headers.put("licensekey", BIDECDSA.encrypt(licenseKey, sharedKey));
      headers.put("requestid", BIDECDSA.encrypt(new Gson().toJson(WTM.makeRequestId()), sharedKey));
      headers.put("publickey", keySet.publicKey);

      Map<String, Object> body = new HashMap<>();
      body.put("dns", attestationOptionsRequest.dns);
      body.put("username", attestationOptionsRequest.username);
      body.put("displayName", attestationOptionsRequest.displayName);
      body.put("attestation", attestationOptionsRequest.attestation);
      body.put("authenticatorSelection", attestationOptionsRequest.authenticatorSelection);
      body.put("communityId", communityInfo.community.id);
      body.put("tenantId", communityInfo.tenant.id);

      String enc_data = BIDECDSA.encrypt(new Gson().toJson(body), sharedKey);

      Map<String, Object> data = new HashMap<>();
      data.put("data", enc_data);

      Map<String, Object> response = WTM.execute("post",
        sd.webauthn + "/attestation/options",
        headers,
        new Gson().toJson(data)
      );

      String responseStr = (String) response.get("response");

      ret = new Gson().fromJson(responseStr, BIDAttestationOptionsResponse.class);

      if (ret.data != null) {
        String dec_data = BIDECDSA.decrypt(ret.data, sharedKey);
        ret = new Gson().fromJson(dec_data, BIDAttestationOptionsResponse.class);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return ret;
  }

  public static BIDAttestationResultData submitAttestationResult(BIDTenantInfo tenantInfo, BIDAttestationResultValue attestationResultRequest) {
    BIDAttestationResultData ret = null;
    try {
      BIDCommunityInfo communityInfo = BIDTenant.getInstance().getCommunityInfo(tenantInfo);
      BIDKeyPair keySet = BIDTenant.getInstance().getKeySet();
      String licenseKey = tenantInfo.licenseKey;
      BIDSD sd = BIDTenant.getInstance().getSD(tenantInfo);

      String webAuthnPublicKey = getPublicKey(tenantInfo);

      String sharedKey = BIDECDSA.createSharedKey(keySet.privateKey, webAuthnPublicKey);

      Map<String, String> headers = WTM.defaultHeaders();
      headers.put("licensekey", BIDECDSA.encrypt(licenseKey, sharedKey));
      headers.put("requestid", BIDECDSA.encrypt(new Gson().toJson(WTM.makeRequestId()), sharedKey));
      headers.put("publickey", keySet.publicKey);

      Map<String, Object> body = new HashMap<>();
      body.put("rawId", attestationResultRequest.rawId);
      body.put("response", attestationResultRequest.response);
      body.put("authenticatorAttachment", attestationResultRequest.authenticatorAttachment);
      body.put("getClientExtensionResults", attestationResultRequest.getClientExtensionResults);
      body.put("id", attestationResultRequest.id);
      body.put("type", attestationResultRequest.type);
      body.put("dns", attestationResultRequest.dns);
      body.put("communityId", communityInfo.community.id);
      body.put("tenantId", communityInfo.tenant.id);

      String enc_data = BIDECDSA.encrypt(new Gson().toJson(body), sharedKey);

      Map<String, Object> data = new HashMap<>();
      data.put("data", enc_data);

      Map<String, Object> response = WTM.execute("post",
        sd.webauthn + "/attestation/result",
        headers,
        new Gson().toJson(data)
      );

      String responseStr = (String) response.get("response");

      ret = new Gson().fromJson(responseStr, BIDAttestationResultData.class);
      
      if (ret.data != null) {
        String dec_data = BIDECDSA.decrypt(ret.data, sharedKey);
        ret = new Gson().fromJson(dec_data, BIDAttestationResultData.class);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return ret;
  }

  public static BIDAssertionOptionResponse fetchAssertionOptions(BIDTenantInfo tenantInfo, BIDAssertionOptionValue assertionOptionRequest) {
    BIDAssertionOptionResponse ret = null;
    try {
      BIDCommunityInfo communityInfo = BIDTenant.getInstance().getCommunityInfo(tenantInfo);
      BIDKeyPair keySet = BIDTenant.getInstance().getKeySet();
      String licenseKey = tenantInfo.licenseKey;
      BIDSD sd = BIDTenant.getInstance().getSD(tenantInfo);

      String webAuthnPublicKey = getPublicKey(tenantInfo);

      String sharedKey = BIDECDSA.createSharedKey(keySet.privateKey, webAuthnPublicKey);

      Map<String, String> headers = WTM.defaultHeaders();
      headers.put("licensekey", BIDECDSA.encrypt(licenseKey, sharedKey));
      headers.put("requestid", BIDECDSA.encrypt(new Gson().toJson(WTM.makeRequestId()), sharedKey));
      headers.put("publickey", keySet.publicKey);

      Map<String, Object> body = new HashMap<>();
      body.put("username", assertionOptionRequest.username);
      body.put("username", assertionOptionRequest.displayName);
      body.put("dns", assertionOptionRequest.dns);
      body.put("communityId", communityInfo.community.id);
      body.put("tenantId", communityInfo.tenant.id);

      String enc_data = BIDECDSA.encrypt(new Gson().toJson(body), sharedKey);

      Map<String, Object> data = new HashMap<>();
      data.put("data", enc_data);

      Map<String, Object> response = WTM.execute("post",
        sd.webauthn + "/assertion/options",
        headers,
        new Gson().toJson(data)
      );

      String responseStr = (String) response.get("response");

      ret = new Gson().fromJson(responseStr, BIDAssertionOptionResponse.class);
      
      if (ret.data != null) {
        String dec_data = BIDECDSA.decrypt(ret.data, sharedKey);
        ret = new Gson().fromJson(dec_data, BIDAssertionOptionResponse.class);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return ret;
  }

  public static BIDAssertionResultResponse submitAssertionResult(BIDTenantInfo tenantInfo, BIDAssertionResultValue assertionResultRequest) {
    BIDAssertionResultResponse ret = null;
    try {
      BIDCommunityInfo communityInfo = BIDTenant.getInstance().getCommunityInfo(tenantInfo);
      BIDKeyPair keySet = BIDTenant.getInstance().getKeySet();
      String licenseKey = tenantInfo.licenseKey;
      BIDSD sd = BIDTenant.getInstance().getSD(tenantInfo);

      String webAuthnPublicKey = getPublicKey(tenantInfo);

      String sharedKey = BIDECDSA.createSharedKey(keySet.privateKey, webAuthnPublicKey);

      Map<String, String> headers = WTM.defaultHeaders();
      headers.put("licensekey", BIDECDSA.encrypt(licenseKey, sharedKey));
      headers.put("requestid", BIDECDSA.encrypt(new Gson().toJson(WTM.makeRequestId()), sharedKey));
      headers.put("publickey", keySet.publicKey);

      Map<String, Object> body = new HashMap<>();
      body.put("rawId", assertionResultRequest.rawId);
      body.put("dns", assertionResultRequest.dns);
      body.put("authenticatorData", assertionResultRequest.authenticatorData);
      body.put("signature", assertionResultRequest.signature);
      body.put("userHandle", assertionResultRequest.userHandle);
      body.put("clientDataJSON", assertionResultRequest.clientDataJSON);
      body.put("getClientExtensionResults", assertionResultRequest.getClientExtensionResults);
      body.put("id", assertionResultRequest.id);
      body.put("type", assertionResultRequest.type);
      body.put("communityId", communityInfo.community.id);
      body.put("tenantId", communityInfo.tenant.id);

      String enc_data = BIDECDSA.encrypt(new Gson().toJson(body), sharedKey);

      Map<String, Object> data = new HashMap<>();
      data.put("data", enc_data);

      Map<String, Object> response = WTM.execute("post",
        sd.webauthn + "/assertion/result",
        headers,
        new Gson().toJson(data)
      );

      String responseStr = (String) response.get("response");

      ret = new Gson().fromJson(responseStr, BIDAssertionResultResponse.class);
      
      if (ret.data != null) {
        String dec_data = BIDECDSA.decrypt(ret.data, sharedKey);
        ret = new Gson().fromJson(dec_data, BIDAssertionResultResponse.class);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return ret;
  }
}
