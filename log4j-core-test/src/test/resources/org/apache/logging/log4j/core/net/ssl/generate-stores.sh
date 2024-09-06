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
caPassword="aCaSecret"
keySize=2048
validDays=$[365 * 10]

### CA key and certificate ####################################################

# Generate the CA configuration
cat >ca.cfg <<EOF
[ req ]
distinguished_name = CA_DN
prompt             = no
output_password    = $caPassword
default_bits       = $keySize

[ CA_DN ]
CN = log4j-ca
EOF

# Create the CA key and certificate
openssl req -config ca.cfg -new -x509 -nodes -keyout ca.key -out ca.crt -days $validDays

### Trust store (JKS) #########################################################

generateJksTrustStore() {

  # Receive arguments
  local storeFileName="$1"
  local storePassword="$2"

  # Create the trust store and import the CA certificate
  keytool -keystore "$storeFileName" -storetype JKS -importcert -file ca.crt -alias log4j-ca -keypass "$storePassword" -storepass "$storePassword" -noprompt

  # Copy the result
  cp -f "$storeFileName" ../

}

# Create the primary trust store
keyStorePassword="aTrustStoreSecret"
generateJksTrustStore trustStore.jks "$keyStorePassword"

# Create the secondary trust store
generateJksTrustStore trustStore2.jks "${keyStorePassword}2"

### Key store (JKS) ###########################################################

generateJksKeyStore() {

  # Receive arguments
  local storeFileName="$1"
  local storePassword="$2"

  # Create the key store and import the CA certificate
  keytool -keystore "$storeFileName" -storetype JKS -importcert -file ca.crt -alias log4j-ca -keypass "$storePassword" -storepass "$storePassword" -noprompt

  # Create the private key in the key store
  keytool -genkeypair -keyalg RSA -alias log4j-client -keystore "$storeFileName" -storepass "$storePassword" -keypass "$storePassword" -validity $validDays -keysize $keySize -dname "CN=log4j-client"

  # Create a signing request for the client
  keytool -keystore "$storeFileName" -alias log4j-client -certreq -file "$storeFileName-client.csr" -keypass "$storePassword" -storepass "$storePassword"

  # Sign the client certificate
  openssl x509 -req -CA ca.crt -CAkey ca.key -in "$storeFileName-client.csr" -out "$storeFileName-client.crt_signed" -days $validDays -CAcreateserial -passin pass:"$caPassword"

  # Verify the client's signed certificate
  openssl verify -CAfile ca.crt "$storeFileName-client.crt_signed"

  # Import the client's signed certificate to the key store
  keytool -keystore "$storeFileName" -alias log4j-client -importcert -file "$storeFileName-client.crt_signed" -keypass "$storePassword" -storepass "$storePassword" -noprompt

  # Verify the key store
  keytool -list -keystore "$storeFileName" -storepass "$storePassword"

  # Copy the result
  cp -f "$storeFileName" ../  

}

# Create the primary key store
keyStorePassword="aKeyStoreSecret"
generateJksKeyStore keyStore.jks "$keyStorePassword"

# Create the secondary key store
generateJksKeyStore keyStore2.jks "${keyStorePassword}2"

### Key store (P12) ###########################################################

# Convert the key store to P12
keytool -importkeystore -srckeystore keyStore.jks -destkeystore keyStore.p12 -srcstoretype JKS -deststoretype PKCS12 -srcstorepass "$keyStorePassword" -deststorepass "$keyStorePassword"

# Copy the result
cp -f keyStore.p12 ../

### Key store (P12, no password) ##############################################

# Both `keytool` and `openssl` require password for JKS and P12 key store types.
# To workaround this limitation, we will
# 1. Convert from P12 to PEM
# 2. Convert from PEM to P12 without the password
openssl pkcs12 -in keyStore.p12 -out keyStore.pem -nodes -passin pass:"$keyStorePassword"
openssl pkcs12 -export -in keyStore.pem -out keyStore-nopass.p12 -passout pass:

# Copy the result
cp -f keyStore-nopass.p12 ../
