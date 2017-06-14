package nz.co.breakpoint.jmeter.modifiers;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;

import static org.apache.wss4j.common.crypto.Merlin.PREFIX;
import static org.apache.wss4j.common.crypto.Merlin.KEYSTORE_FILE;
import static org.apache.wss4j.common.crypto.Merlin.KEYSTORE_PASSWORD;
import static org.apache.wss4j.common.crypto.Merlin.KEYSTORE_TYPE;

/**
 * Abstract parent class of any preprocessors that perform crypto operations (e.g. signature or encryption).
 */
public abstract class CryptoWSSecurityPreProcessor extends AbstractWSSecurityPreProcessor {

	private static final Logger log = LoggingManager.getLoggerForClass();

	private final Properties cryptoProps; // Holds configured attributes for crypto instance

	private List<SecurityPart> partsToSecure; // Holds the names of XML elements to secure (e.g. SOAP Body)

	static final Map<String, Integer> keyIdentifierMap = new HashMap<String, Integer>();
	static {
		keyIdentifierMap.put("Binary Security Token",         WSConstants.BST_DIRECT_REFERENCE);
		keyIdentifierMap.put("Issuer Name and Serial Number", WSConstants.ISSUER_SERIAL);
		keyIdentifierMap.put("X509 Certificate",              WSConstants.X509_KEY_IDENTIFIER);
		keyIdentifierMap.put("Subject Key Identifier",        WSConstants.SKI_KEY_IDENTIFIER);
		keyIdentifierMap.put("Thumbprint SHA1 Identifier",    WSConstants.THUMBPRINT_IDENTIFIER);
		keyIdentifierMap.put("Encrypted Key SHA1",            WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER); // only for encryption (symmetric signature not implemented yet - would require UI fields for setSecretKey or setEncrKeySha1value)
		keyIdentifierMap.put("Custom Key Identifier",         WSConstants.CUSTOM_KEY_IDENTIFIER); // not implemented yet (requires UI fields for setCustomTokenId and setCustomTokenValueType)
		keyIdentifierMap.put("Key Value",                     WSConstants.KEY_VALUE); // only for signature
		keyIdentifierMap.put("Endpoint Key Identifier",       WSConstants.ENDPOINT_KEY_IDENTIFIER); // not supported by Merlin https://ws.apache.org/wss4j/apidocs/org/apache/wss4j/common/crypto/Merlin.html#getX509Certificates-org.apache.wss4j.common.crypto.CryptoType-
	}

	public CryptoWSSecurityPreProcessor() throws ParserConfigurationException {
		super();
		cryptoProps = new Properties();
		cryptoProps.setProperty("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
		cryptoProps.setProperty(PREFIX+KEYSTORE_TYPE, "jks");
	}

	/* Reverse lookup for above keyIdentifierMap. Mainly used for populating the GUI dropdown.
	 */
	protected static String getKeyIdentifierLabelForType(int keyIdentifierType) {
		for (Map.Entry<String, Integer> id : keyIdentifierMap.entrySet()) {
			if (id.getValue() == keyIdentifierType)
				return id.getKey();
		}
		return null;
	}

	protected Crypto getCrypto() throws WSSecurityException {
		// A new crypto instance needs to be created for every iteration as the config could contain variables which may change.
		log.debug("Getting crypto instance");
		return CryptoFactory.getInstance(cryptoProps);
	}

	// Accessors
	public String getCertAlias() {
		return getUsername();
	}

	public void setCertAlias(String certAlias) {
		setUsername(certAlias);
	}

	public String getCertPassword() {
		return getPassword();
	}

	public void setCertPassword(String certPassword) {
		setPassword(certPassword);
	}

	public String getKeystoreFile() {
		return cryptoProps.getProperty(PREFIX+KEYSTORE_FILE);
	}

	public void setKeystoreFile(String keystoreFile) {
		cryptoProps.setProperty(PREFIX+KEYSTORE_FILE, keystoreFile);
	}

	public String getKeystorePassword() {
		return cryptoProps.getProperty(PREFIX+KEYSTORE_PASSWORD);
	}

	public void setKeystorePassword(String keystorePassword) {
		cryptoProps.setProperty(PREFIX+KEYSTORE_PASSWORD, keystorePassword);
	}

	public String getKeyIdentifier() {
		return getKeyIdentifierLabelForType(getSecBuilder().getKeyIdentifierType());
	}

	public void setKeyIdentifier(String keyIdentifier) {
		getSecBuilder().setKeyIdentifierType(keyIdentifierMap.get(keyIdentifier));
	}

	public List<SecurityPart> getPartsToSecure() {
		return partsToSecure;
	}

	public void setPartsToSecure(List<SecurityPart> partsToSecure) {
		this.partsToSecure = partsToSecure;
		getSecBuilder().getParts().clear();
		for (SecurityPart part : partsToSecure) {
			getSecBuilder().getParts().add(part.getPart());
		}
	}
}
