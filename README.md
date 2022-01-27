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
- Initialize SDK
```
BIDSDK.getInstance().setupTenant(<your tenant>, "<your license key>");
```

- Request OTP
```
BIDOtpResponse otpResponse = BIDOTP.requestOTP("<username>", "<emailTo>", "<smsTo>", "1");
```

- Verify OTP
```
BIDOtpVerifyResult result = BIDOTP.verifyOTP("<username>", "<otpcode>");
```

- Create new UWL2.0 session
```
BIDSession session = BIDSessions.createNewSession(null, null);
```

- Poll for UWL2.0 session response
```
BIDSessionResponse response = BIDSessions.pollSession("<sessionId>", true, true);
```
