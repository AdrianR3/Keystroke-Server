# Keystroke-Server

## Creating a certificate

Generate a certificate using `keytool`:

```shell
keytool 
  -genkey 
  -keyalg RSA 
  -validity 3650 
  -keystore "keystore.jks" 
  -storepass "storepassword" 
  -keypass "keypassword" 
  -alias "default" 
  -dname "CN=127.0.0.1, OU=MyOrgUnit, O=MyOrg, L=MyCity, S=MyRegion, C=MyCountry"
```

Modify the arguments as necessary. Don't change the `-keystore` flag.
Optionally, exclude the `-dname` flag to be walked through the options.

Once finished, place the newly created `keystore.jks` file in the `src/main/resources` directory. Then, repackage the jar or compile.