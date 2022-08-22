mkdir tmp
# Create the CA key and certificate
openssl req -config rootca.conf -new -x509 -nodes -keyout tmp/log4j2-cacert.key -out tmp/log4j2-ca.crt -days 7302
# Create the trust store and import the certificate
keytool -keystore ../truststore.jks -storetype jks -importcert -file 'tmp/log4j2-ca.crt' -keypass changeit -storepass changeit -alias log4j2-cacert -noprompt
#Import the root certificate
keytool -keystore ../client.log4j2-keystore.jks -alias log4j2-ca -importcert -file tmp/log4j2-ca.crt -keypass changeit -storepass changeit -noprompt
# Create the client private key in the client key store
keytool -genkeypair -keyalg RSA -alias client -keystore ../client.log4j2-keystore.jks -storepass changeit -keypass changeit -validity 7302 -keysize 2048 -dname "CN=client.log4j2, C=US" 
# Create a signing request for the client                         #
keytool -keystore ../client.log4j2-keystore.jks -alias client -certreq -file tmp/client.csr -keypass changeit -storepass changeit
# Sign the client certificate
openssl x509 -req -CA 'tmp/log4j2-ca.crt' -CAkey 'tmp/log4j2-cacert.key' -in tmp/client.csr -out tmp/client.crt_signed -days 7302 -CAcreateserial -passin pass:changeit 
# Verify the signed certificate
openssl verify -CAfile 'tmp/log4j2-ca.crt' tmp/client.crt_signed
#Import the client's signed certificate
keytool -keystore ../client.log4j2-keystore.jks -alias client -importcert -file tmp/client.crt_signed -keypass changeit -storepass changeit -noprompt
#Verify the keystore
keytool -list -keystore ../client.log4j2-keystore.jks -storepass changeit
# Create the server private key in the server key store
keytool -genkeypair -keyalg RSA -alias server -keystore tmp/server.log4j2-keystore.p12 -storepass changeit -storetype PKCS12 -keypass changeit -validity 7302 -keysize 2048 -dname "CN=server.log4j2, C=US"
# Create a signing request for the server                         #
keytool -keystore tmp/server.log4j2-keystore.p12 -alias server -certreq -file tmp/server.csr -keypass changeit -storepass changeit
# Sign the server certificate
openssl x509 -req -CA 'tmp/log4j2-ca.crt' -CAkey 'tmp/log4j2-cacert.key' -in tmp/server.csr -out ../server.log4j2-crt.pem -days 7302 -CAcreateserial -passin pass:changeit
# Extract the private key
openssl pkcs12 -in tmp/server.log4j2-keystore.p12 -passin pass:changeit -nokeys -out ../server.log4j2.pem
rm -rf tmp
