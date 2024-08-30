#!/bin/bash -eu
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Switch to the script directory
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

# Reset and switch to the `tmp`
rm -rf tmp
mkdir tmp
cd tmp

# Constants
password="someSecret"
keySize=2048
validDays=$[365 * 10]

### CA key and certificate ####################################################

# Generate the CA configuration
cat >ca.cfg <<EOF
[ req ]
distinguished_name = CA_DN
prompt             = no
output_password    = $password
default_bits       = $keySize

[ CA_DN ]
C  = US
CN = log4j2-ca
EOF

# Create the CA key and certificate
openssl req -config ca.cfg -new -x509 -nodes -keyout ca.key -out ca.crt -days $validDays

### Trust store ###############################################################

# Create the trust store and import the certificate
keytool -keystore trustStore.jks -storetype JKS -importcert -file ca.crt -keypass "$password" -storepass "$password" -alias log4j2-cacert -noprompt

# Copy the result
cp -f trustStore.jks ../

### Client key store (JKS) ####################################################

# Create the key store and import the certificate
keytool -keystore keyStore.jks  -storetype JKS -alias log4j2-ca -importcert -file ca.crt -keypass "$password" -storepass "$password" -noprompt

# Create the private key in the key store
keytool -genkeypair -keyalg RSA -alias client -keystore keyStore.jks -storepass "$password" -keypass "$password" -validity $validDays -keysize $keySize -dname "CN=client.log4j2, C=US" 

# Create a signing request for the client
keytool -keystore keyStore.jks -alias client -certreq -file client.csr -keypass "$password" -storepass "$password"

# Sign the client certificate
openssl x509 -req -CA ca.crt -CAkey ca.key -in client.csr -out client.crt_signed -days $validDays -CAcreateserial -passin pass:"$password" 

# Verify the client's signed certificate
openssl verify -CAfile ca.crt client.crt_signed

# Import the client's signed certificate to the key store
keytool -keystore keyStore.jks -alias client -importcert -file client.crt_signed -keypass "$password" -storepass "$password" -noprompt

# Verify the key store
keytool -list -keystore keyStore.jks -storepass "$password"

# Copy the result
cp -f keyStore.jks ../

### Client key store (P12) ####################################################

# Convert the key store to P12
keytool -importkeystore -srckeystore keyStore.jks -destkeystore keyStore.p12 -srcstoretype JKS -deststoretype PKCS12 -srcstorepass "$password" -deststorepass "$password"

# Copy the result
cp -f keyStore.p12 ../

### Client key store (P12 without password) ###################################

# Both `keytool` and `openssl` require password for JKS and P12 key store types.
# To workaround this limitation, we will
# 1. Convert from P12 to PEM
# 2. Convert from PEM to P12 without the password
openssl pkcs12 -in keyStore.p12 -out keyStore.pem -nodes -passin pass:"$password"
openssl pkcs12 -export -in keyStore.pem -out keyStore-nopass.p12 -passout pass:

# Copy the result
cp -f keyStore-nopass.p12 ../
