package edu.stevens.cs594.crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import edu.stevens.cs594.util.DateUtils;
import edu.stevens.cs594.util.ExceptionWrapper;

public class CAUtils {
	
	private static final String TAG = CAUtils.class.getCanonicalName();
	private static final Logger logger = Logger.getLogger(TAG);
	
	/**
	 * For use by the server-side CA.
	 */
	
	public final static KeyUsage CA_USAGE;

	public final static KeyUsage END_USAGE;
	
	public final static KeyUsage CODE_SIGN_USAGE;
	
	public final static KeyUsage ENCRYPT_USAGE;
	
	public final static ExtendedKeyUsage CLIENT_USAGE_EXT;

	public final static ExtendedKeyUsage SERVER_USAGE_EXT;

	public final static ExtendedKeyUsage CODE_SIGN_USAGE_EXT;

	static {
		CA_USAGE = new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign);
		END_USAGE = new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement | KeyUsage.keyEncipherment);
		CODE_SIGN_USAGE = new KeyUsage(KeyUsage.digitalSignature);
		ENCRYPT_USAGE = new KeyUsage(KeyUsage.keyEncipherment);
		CLIENT_USAGE_EXT = new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth);
		SERVER_USAGE_EXT = new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth);
		CODE_SIGN_USAGE_EXT = new ExtendedKeyUsage(KeyPurposeId.id_kp_codeSigning);
	}
	
	public static X500Name toX500Name(X500Principal principal) {
		// return X500Name.getInstance(principal.getEncoded());
		return new X500Name(principal.getName());
	}

	/**
	 * Certificate signer.
	 */
	public static ContentSigner getContentSigner(PrivateKey privateKey) throws OperatorCreationException{
		return new JcaContentSignerBuilder("SHA512withRSA")
				.setProvider("BC")
				.build(privateKey);
	}

	/**
	 * Certificate verifier.
	 */
	public static ContentVerifierProvider getContentVerifier(PublicKey publicKey) throws OperatorCreationException {
		return new JcaContentVerifierProviderBuilder()
				.setProvider("BC")
				.build(publicKey);
	}
	
	/**
	 * Build a self-signed v1 certificate to bootstrap a client.
	 */
	public static X509Certificate createClientRootCert(long id, X500Name clientDn, KeyPair keyPair,
			long durationHours) throws GeneralSecurityException {
		// TODO generate self-signed v1 certificate
		try {
			// generate self-signed v1 certificate
			ContentSigner signGenerator = getContentSigner(keyPair.getPrivate());
			
			X509v1CertificateBuilder  certBuilder = new JcaX509v1CertificateBuilder(clientDn,BigInteger.valueOf(id),DateUtils.getDate(System.currentTimeMillis()),
					DateUtils.getDate(System.currentTimeMillis()+durationHours),clientDn,keyPair.getPublic());
			X509CertificateHolder certHolder = certBuilder.build(signGenerator);
			return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
		} catch (OperatorCreationException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		}
	}
	
	/**
	 * Build a self-signed v3 certificate for the root CA.
	 */
	public static X509Certificate createCaRootCert(long id, X500Name caName, KeyPair keyPair,
			long durationHours) throws GeneralSecurityException {
		// TODO generate root CA self-signed v3 cert
		System.out.println("--------------------------");
		System.out.println(keyPair);
		try {
			// generate self-signed v1 certificate
			ContentSigner signGenerator = getContentSigner(keyPair.getPrivate());
			
			X509v1CertificateBuilder  certBuilder = new JcaX509v1CertificateBuilder(caName,BigInteger.valueOf(id),DateUtils.getDate(System.currentTimeMillis()),
					DateUtils.getDate(System.currentTimeMillis()+durationHours),caName,keyPair.getPublic());
			X509CertificateHolder certHolder = certBuilder.build(signGenerator);
			return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
		} catch (OperatorCreationException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		}
	}
	
	/**
	 * Create an intermediate or end-entity certificate.
	 */
	private static X509Certificate createCert(long id, PrivateKey issuerKey, X509Certificate issuerCert,
			X500Name subjectName, PublicKey subjectKey, BasicConstraints basicConstraints, KeyUsage usage,
			ExtendedKeyUsage extendedUsage, GeneralNames sans, long durationHours) throws GeneralSecurityException {
		X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
			// NOTE: BUG in slides: replace issuerCert with:
			toX500Name(issuerCert.getSubjectX500Principal()),
			BigInteger.valueOf(id), 
			DateUtils.now(),
			DateUtils.then(durationHours * DateUtils.ONE_HOUR),
			subjectName, 
			subjectKey);
		
		// TODO add extensions as described in lectures
		
		// TODO create end certificate
		try {
			ContentSigner signGen = getContentSigner(issuerKey);

			JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
			certBuilder.addExtension(Extension.subjectKeyIdentifier,false,extUtils.createSubjectKeyIdentifier(subjectKey)).addExtension(Extension.basicConstraints,
					true,new BasicConstraints(1)).addExtension(Extension.keyUsage,true,CA_USAGE);
			X509CertificateHolder certHolder = certBuilder.build(signGen);
			if (extendedUsage != null) {
				certBuilder.addExtension(Extension.extendedKeyUsage, false, extendedUsage);
			}
			
			if (sans != null) {
				certBuilder.addExtension(Extension.subjectAlternativeName, false, sans);
			}
			
			return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
		} catch (NoSuchAlgorithmException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (CertIOException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (OperatorCreationException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		}

	}
	
	/**
	 * Create an intermediate certificate for online CA.
	 */
	public static X509Certificate createOnlineCaCert(long id, PrivateKey issuerKey, X509Certificate issuerCert,
			X500Name subjectName, PublicKey subjectKey, long durationHours) throws GeneralSecurityException {
		BasicConstraints basicConstraints = new BasicConstraints(0);
		return createCert(id, issuerKey, issuerCert, subjectName, subjectKey, basicConstraints, CA_USAGE, null, null, durationHours);
	}
	
	/**
	 * Create an end-user certificate for the server.
	 */
	public static X509Certificate createServerCert(long id, PrivateKey issuerKey, X509Certificate issuerCert,
			X500Name subjectName, String serverDNS, PublicKey subjectKey, long durationHours) throws GeneralSecurityException {
		GeneralName san = new GeneralName(GeneralName.dNSName, serverDNS);
		GeneralNames sans = new GeneralNames(new GeneralName[]{ san });
		BasicConstraints basicConstraints = new BasicConstraints(false);
		return createCert(id, issuerKey, issuerCert, subjectName, subjectKey, basicConstraints, END_USAGE,
				SERVER_USAGE_EXT, sans, durationHours);
	}
	
	/**
	 * Request a certificate from the CA certifying our signature.
	 */
	public static PKCS10CertificationRequest createCSR(X500Name subject, KeyPair keyPair, 
			String dnsAddress) throws GeneralSecurityException {
		// TODO Generate CSR
		try {
			List<GeneralName> names = new ArrayList<GeneralName>();
			int nameType;
			nameType = GeneralName.dNSName;
			names.add(new GeneralName(nameType,dnsAddress));
			GeneralNames sans =new GeneralNames(names.toArray(new GeneralName[names.size()]));
			ExtensionsGenerator extGen = new ExtensionsGenerator();
			extGen.addExtension(Extension.subjectAlternativeName,false,sans);
			PKCS10CertificationRequestBuilder  requestBuilder = new JcaPKCS10CertificationRequestBuilder(subject,keyPair.getPublic());
			requestBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
			ContentSigner sigGenerator = getContentSigner(keyPair.getPrivate());
			PKCS10CertificationRequest request =  requestBuilder.build(sigGenerator);
			return request;
		} catch (IOException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (OperatorCreationException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		}
	}
	
	/**
	 * Generate a client certificate from a CSR.
	 */
	public static X509Certificate createClientCert(long id, PrivateKey issuerKey, X509Certificate issuerCert,
			PKCS10CertificationRequest csr,  String dnsAddress, long durationHours)
			throws GeneralSecurityException {
		try {
			BasicConstraints basicConstraints = new BasicConstraints(false);
			PublicKey subjectKey = new JcaPKCS10CertificationRequest(csr).getPublicKey();

			ContentVerifierProvider contentVerifier = getContentVerifier(subjectKey);
			if (!csr.isSignatureValid(contentVerifier)) {
				logger.log(Level.WARNING, "Bad signature on certificate signing request: " + csr.getSubject());
				return null;
			}
			
			// TODO generate client cert and add extensions (add DNS SAN if dnsAddress is provided)

			X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(X500Name.getInstance(
			         issuerCert.getSubjectX500Principal().getEncoded()),BigInteger.valueOf(id),DateUtils.getDate(System.currentTimeMillis()),DateUtils.getDate(System.currentTimeMillis()+durationHours),
					csr.getSubject(),subjectKey);
			JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
			certBuilder.addExtension(Extension.authorityKeyIdentifier,false,extUtils.createAuthorityKeyIdentifier(issuerCert)).addExtension(Extension.subjectKeyIdentifier,false,extUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo())).addExtension(
					Extension.basicConstraints,true,basicConstraints);
			for(Attribute attr:csr.getAttributes()){
				if(attr.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)){
					 Extensions extensions = Extensions.getInstance(attr.getAttrValues().getObjectAt(0));
					 GeneralNames sans = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
					 if(sans!=null){
						 certBuilder.addExtension(Extension.subjectAlternativeName,false,sans);
						 for(GeneralName name: sans.getNames()){
							 switch(name.getTagNo()){
								 case GeneralName.dNSName:
									name = new GeneralName(GeneralName.dNSName, dnsAddress); 
							 }
						 }
					 }
				}
			}
			ContentSigner sigGenerator = getContentSigner(issuerKey);
			X509CertificateHolder certHolder = certBuilder.build(sigGenerator);
			return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
		
			
		} catch (NoSuchAlgorithmException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (OperatorCreationException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (PKCSException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (CertIOException e) {
			// TODO Auto-generated catch block
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		}
	}
	
}
