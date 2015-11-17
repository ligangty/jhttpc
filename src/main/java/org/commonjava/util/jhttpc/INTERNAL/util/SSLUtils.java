/**
 * Copyright (C) 2015 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.util.jhttpc.INTERNAL.util;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.bc.BcPEMDecryptorProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS12PfxPdu;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemObjectParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.security.auth.x500.X500Principal;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.io.IOUtils.closeQuietly;

public final class SSLUtils
{

    private static final Integer DNSNAME_TYPE = 2;

    private SSLUtils()
    {
    }

    public static KeyStore readKeyAndCert( final String pemContent, final String keyPass )
            throws PKCSException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
                   InvalidKeySpecException
    {
        final KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
        ks.load( null );

        //        final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
        //                KeyManagerFactory.getDefaultAlgorithm());
        //
        //        kmfactory.init(ks, keyPass.toCharArray());

        final CertificateFactory certFactory = CertificateFactory.getInstance( "X.509" );
        final KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );

        final List<String> lines = readLines( pemContent );

        String currentHeader = null;
        final StringBuilder current = new StringBuilder();

        int certIdx = 0;
        Logger logger = LoggerFactory.getLogger( SSLUtils.class );

        BouncyCastleProvider bcProvider = new BouncyCastleProvider();
        InputDecryptorProvider provider =
                new JcePKCSPBEInputDecryptorProviderBuilder().setProvider( bcProvider ).build( keyPass.toCharArray() );

        final List<Certificate> certs = new ArrayList<Certificate>();
        PrivateKey key = null;

        PEMParser pemParser = new PEMParser( new StringReader( pemContent ) );
        Object pemObj = null;
        while ( ( pemObj = pemParser.readObject() ) != null )
        {
            logger.debug( "Got PEM object: {}", pemObj );
            if ( pemObj instanceof X509CertificateHolder )
            {
                X509CertificateHolder holder = (X509CertificateHolder) pemObj;
                X509Certificate certificate =
                        new JcaX509CertificateConverter().setProvider( bcProvider ).getCertificate( holder );

                certs.add( certificate );

                Set<String> aliases = new HashSet<String>();
                aliases.add( "certificate" + certIdx );

                extractAliases( certificate, aliases );

                KeyStore.TrustedCertificateEntry ksEntry = new KeyStore.TrustedCertificateEntry( certificate );
                for ( String alias : aliases )
                {
                    ks.setEntry( alias, ksEntry, null );
                    logger.info( "Storing trusted cert under alias: {}\n  with DN: {}", alias,
                                 certificate.getSubjectDN().getName() );
                }

                certIdx++;
            }
            else if ( pemObj instanceof PKCS8EncryptedPrivateKeyInfo )
            {
                PKCS8EncryptedPrivateKeyInfo keyInfo = (PKCS8EncryptedPrivateKeyInfo) pemObj;
                PrivateKeyInfo privateKeyInfo = keyInfo.decryptPrivateKeyInfo( provider );
                key = new JcaPEMKeyConverter().getPrivateKey( privateKeyInfo );
            }
            else if ( pemObj instanceof PEMEncryptedKeyPair )
            {
                PEMEncryptedKeyPair keyPair = (PEMEncryptedKeyPair) pemObj;
                PEMKeyPair decryptedKeyPair = keyPair.decryptKeyPair( new BcPEMDecryptorProvider( keyPass.toCharArray() ) );
                PrivateKeyInfo privateKeyInfo = decryptedKeyPair.getPrivateKeyInfo();
                key = new JcaPEMKeyConverter().getPrivateKey( privateKeyInfo );
            }
        }

        if ( key != null && !certs.isEmpty() )
        {
            logger.debug( "Setting key entry: {}", key );
            ks.setKeyEntry( MonolithicKeyStrategy.KEY, key, keyPass.toCharArray(),
                            certs.toArray( new Certificate[certs.size()] ) );
        }
        else
        {
            logger.debug( "No private key found in PEM!" );
        }

        Enumeration<String> aliases = ks.aliases();
        while ( aliases.hasMoreElements() )
        {
            String alias = aliases.nextElement();
            logger.debug( "Got alias: {}. Is Cert? {} Is Private key? {}", alias, ks.isCertificateEntry( alias ),
                          ks.isKeyEntry( alias ) );
        }

        return ks;
    }

    public static KeyStore readCerts( final String pemContent, final String aliasPrefix )
            throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException
    {
        final KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
        ks.load( null );

        final CertificateFactory certFactory = CertificateFactory.getInstance( "X.509" );

        final List<String> lines = readLines( pemContent );

        final StringBuilder current = new StringBuilder();
        final List<String> entries = new ArrayList<String>();
        for ( final String line : lines )
        {
            if ( line == null )
            {
                continue;
            }

            if ( line.startsWith( "-----BEGIN" ) )
            {
                current.setLength( 0 );
            }
            else if ( line.startsWith( "-----END" ) )
            {
                entries.add( current.toString() );
            }
            else
            {
                current.append( line.trim() );
            }
        }

        int i = 0;
        Logger logger = LoggerFactory.getLogger( SSLUtils.class );
        for ( final String entry : entries )
        {
            final byte[] data = decodeBase64( entry );

            final Certificate c = certFactory.generateCertificate( new ByteArrayInputStream( data ) );
            X509Certificate cert = (X509Certificate) c;

            Set<String> aliases = new HashSet<String>();
            if ( i < 1 )
            {
                aliases.add( aliasPrefix );
            }
            else
            {
                aliases.add( aliasPrefix + i );
            }

            extractAliases( cert, aliases );

            KeyStore.TrustedCertificateEntry ksEntry = new KeyStore.TrustedCertificateEntry( cert );
            for ( String alias : aliases )
            {
                ks.setEntry( alias, ksEntry, null );
                logger.info( "Storing trusted cert under alias: {}\n  with DN: {}", alias,
                             cert.getSubjectDN().getName() );
            }

            i++;
        }

        return ks;
    }

    public static void extractAliases( Certificate certificate, Set<String> aliases )
            throws CertificateParsingException
    {
        Logger logger = LoggerFactory.getLogger( SSLUtils.class );

        X509Certificate cert = (X509Certificate) certificate;
        logger.debug( "Extracting aliases from:\n\n{}\n\n", cert );

        X500Principal x500Principal = cert.getSubjectX500Principal();
        X500Name x500Name = new X500Name( x500Principal.getName( X500Principal.RFC1779 ) );

        RDN[] matchingRDNs = x500Name.getRDNs( BCStyle.CN );
        if ( matchingRDNs != null && matchingRDNs.length > 0 )
        {
            RDN cn = matchingRDNs[0];
            AttributeTypeAndValue typeAndValue = cn.getFirst();
            if ( typeAndValue != null )
            {
                aliases.add( IETFUtils.valueToString( typeAndValue.getValue() ) );
            }
        }

        Collection<List<?>> subjectAlternativeNames = cert.getSubjectAlternativeNames();
        if ( subjectAlternativeNames != null )
        {
            for ( List<?> names : subjectAlternativeNames )
            {
                if ( names.size() > 1 && ( DNSNAME_TYPE.equals( names.get( 0 ) ) ) )
                {
                    aliases.add( (String) names.get( 1 ) );
                }
            }
        }
        else
        {
            logger.debug( "NO SubjectAlternativeNames available!" );
        }
    }

    private static List<String> readLines( final String content )
            throws IOException
    {
        final List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new InputStreamReader(
                    new ByteArrayInputStream( content.getBytes( Charset.forName( "UTF-8" ) ) ) ) );
            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                lines.add( line.trim() );
            }
        }
        finally
        {
            closeQuietly( reader );
        }

        return lines;
    }
}
