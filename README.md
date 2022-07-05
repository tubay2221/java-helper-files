# java-helper-files

# Adding java helpers to your project

- Add repo to your project (as submodule) under path: `main › java › com › bidsdk`

_(*ability* to add as gradle dependency will be added soon)_

- add to build.gradle
```
ext {
	web3jVersion = "4.5.4"
}

dependencies {

	implementation group: 'org.bitcoinj', name: 'bitcoinj-core', version: '0.15.10'
	implementation "org.web3j:core:$web3jVersion"

	implementation group: 'com.squareup.okhttp3', name: 'okhttp', version: '4.3.1'
	implementation 'org.apache.httpcomponents:httpclient:4.5.13'
	implementation 'com.google.code.gson:gson:2.8.6'
}

```

- Know your tenant (BIDTenant) `dns` and `communityName`

- Request OTP
```
BIDTenantInfo tenantInfo = new BIDTenantInfo();
		 
tenantInfo.dns = "<dns>";
tenantInfo.communityName = "<communityName>";
tenantInfo.licenseKey = "<licenseKey>";

BIDOtpResponse otpResponse = BIDOTP.requestOTP(tenantInfo, "<username>", "<emailTo>", "<smsTo>", "1");
```

- Verify OTP
```
BIDTenantInfo tenantInfo = new BIDTenantInfo();
		 
tenantInfo.dns = "<dns>";
tenantInfo.communityName = "<communityName>";
tenantInfo.licenseKey = "<licenseKey>";

BIDOtpVerifyResult result = BIDOTP.verifyOTP(tenantInfo, "<username>", "<otpcode>");
```

- Create new UWL2.0 session
```
BIDTenantInfo tenantInfo = new BIDTenantInfo();
		 
tenantInfo.dns = "<dns>";
tenantInfo.communityName = "<communityName>";
tenantInfo.licenseKey = "<licenseKey>";

BIDSession session = BIDSessions.createNewSession(tenantInfo, null, null);
```

- Poll for UWL2.0 session response
```
BIDTenantInfo tenantInfo = new BIDTenantInfo();
		 
tenantInfo.dns = "<dns>";
tenantInfo.communityName = "<communityName>";
tenantInfo.licenseKey = "<licenseKey>";

BIDSessionResponse response = BIDSessions.pollSession(tenantInfo, "<sessionId>", true, true);
```
