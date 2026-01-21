/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jspecify.annotations.Nullable;

/**
 * Utility class to generate X.509 certificates for testing purposes.
 */
final class X509Certificates {

    private static final String CA_DN = "CN=Test CA";
    private static final long MINUTE_IN_MILLIS = 60_000L;
    private static final long YEAR_IN_MILLIS = 365L * 24 * 60 * MINUTE_IN_MILLIS;

    private static final KeyPairGenerator RSA_GENERATOR;
    private static final Random RANDOM = new Random();

    static {
        try {
            RSA_GENERATOR = KeyPairGenerator.getInstance("RSA");
            RSA_GENERATOR.initialize(2048);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static KeyPair generateKeyPair() {
        return RSA_GENERATOR.generateKeyPair();
    }

    static X509Certificate generateCACertificate(KeyPair keyPair) throws Exception {
        JcaX509v3CertificateBuilder builder = getCertificateBuilder(keyPair.getPublic(), CA_DN, true);
        addKeyUsageExtension(builder, KeyUsage.keyCertSign);
        return buildCertificate(builder, keyPair.getPrivate());
    }

    /**
     * Create and sign a server X.509 certificate for tests.
     *
     * <p>The produced certificate complies with {@code sun.security.validator.EndEntityChecker}.</p>
     *
     * @param keyPair the subject key pair
     * @param caKey the private key of the issuing CA used to sign the certificate
     * @param subjectDn the subject distinguished name for the certificate (for example {@code CN=example.com})
     * @param dnsAltSubject optional DNS Subject Alternative Name; pass {@code null} to omit
     * @param ipAltSubject optional IP Subject Alternative Name; pass {@code null} to omit
     * @return a signed X.509 server certificate
     * @throws Exception if certificate creation or signing fails
     */
    static X509Certificate generateServerCertificate(
            KeyPair keyPair,
            PrivateKey caKey,
            String subjectDn,
            @Nullable String dnsAltSubject,
            @Nullable String ipAltSubject)
            throws Exception {
        JcaX509v3CertificateBuilder builder = getCertificateBuilder(keyPair.getPublic(), subjectDn, false);
        // The required key usage for the server certificate depends on the key exchange algorithm:
        // - keyEncipherment for RSA key exchange (deprecated)
        // - digitalSignature for ephemeral Diffie-Hellman key exchange (DHE or ECDHE)
        // - keyAgreement for static Diffie-Hellman key exchange (DH or ECDH)
        addKeyUsageExtension(builder, KeyUsage.digitalSignature | KeyUsage.keyAgreement);
        addExtendedKeyUsageExtension(builder, KeyPurposeId.id_kp_serverAuth);
        addSubjectAlternativeName(builder, dnsAltSubject, ipAltSubject);
        return buildCertificate(builder, caKey);
    }

    /**
     * Create and sign a client X.509 certificate for tests.
     *
     * <p>The produced certificate complies with {@code sun.security.validator.EndEntityChecker}.</p>
     *
     * @param keyPair the subject key pair
     * @param caKey the private key of the issuing CA used to sign the certificate
     * @param subjectDn the subject distinguished name for the certificate (for example {@code CN=example.com})
     * @return a signed X.509 server certificate
     * @throws Exception if certificate creation or signing fails
     */
    static X509Certificate generateClientCertificate(KeyPair keyPair, PrivateKey caKey, String subjectDn)
            throws Exception {
        JcaX509v3CertificateBuilder builder = getCertificateBuilder(keyPair.getPublic(), subjectDn, false);
        // The required key usage for the client certificate
        addKeyUsageExtension(builder, KeyUsage.digitalSignature);
        addExtendedKeyUsageExtension(builder, KeyPurposeId.id_kp_clientAuth);
        return buildCertificate(builder, caKey);
    }

    private static JcaX509v3CertificateBuilder getCertificateBuilder(
            PublicKey subjectPub, String subjectDn, boolean isCa) throws CertIOException {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - MINUTE_IN_MILLIS);
        Date notAfter = new Date(now + YEAR_IN_MILLIS);
        BigInteger serial = BigInteger.valueOf(RANDOM.nextLong());

        X500Name issuer = new X500Name(CA_DN);
        X500Name subject = new X500Name(subjectDn);

        JcaX509v3CertificateBuilder builder =
                new JcaX509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, subjectPub);

        // Basic Constraints
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCa));

        return builder;
    }

    private static void addKeyUsageExtension(JcaX509v3CertificateBuilder builder, int keyUsage) throws CertIOException {
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsage));
    }

    private static void addExtendedKeyUsageExtension(JcaX509v3CertificateBuilder builder, KeyPurposeId kp)
            throws CertIOException {
        builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(kp));
    }

    private static GeneralName getIpAddressGeneralName(String ipAltSubject) {
        return new GeneralName(GeneralName.iPAddress, ipAltSubject);
    }

    private static GeneralName getDnsGeneralName(String dnsAltSubject) {
        return new GeneralName(GeneralName.dNSName, dnsAltSubject);
    }

    private static void addSubjectAlternativeName(
            JcaX509v3CertificateBuilder builder, @Nullable String dnsAltSubject, @Nullable String ipAltSubject)
            throws CertIOException {
        if (ipAltSubject != null || dnsAltSubject != null) {
            List<GeneralName> names = new ArrayList<>();
            if (dnsAltSubject != null) {
                names.add(getDnsGeneralName(dnsAltSubject));
            }
            if (ipAltSubject != null) {
                names.add(getIpAddressGeneralName(ipAltSubject));
            }
            GeneralName[] gna = names.toArray(new GeneralName[0]);
            builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(gna));
        }
    }

    private static X509Certificate buildCertificate(JcaX509v3CertificateBuilder builder, PrivateKey signerKey)
            throws OperatorCreationException, CertificateException {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(signerKey);

        X509CertificateHolder holder = builder.build(signer);

        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    private X509Certificates() {
        // private constructor to prevent instantiation
    }
}
