package edu.stevens.cs594.chat.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import edu.stevens.cs594.driver.Driver;
import edu.stevens.cs594.util.ExceptionWrapper;
import edu.stevens.cs594.util.FileUtils;
import edu.stevens.cs594.util.Reporter;
import edu.stevens.cs594.util.StringUtils;
//import edu.stevens.cs594.crypto.SecurityParams;

public class App implements Driver.Callback<App.Command,App.Option> {
	
	/**
	 * Invoke with arguments.
	 */
	
	private static final Logger logger = Logger.getLogger(App.class.getCanonicalName());

	private static final String PROMPT = "client> ";
	
	/*
	 * Properties in the passwords file.
	 */
	private static final String CLIENT_TRUSTSTORE_PASSWORD = "client.truststore.password";
	
	private static final String CLIENT_KEYSTORE_PASSWORD = "client.keystore.password";
	
	private static final String CLIENT_KEY_PASSWORD = "client.key.password";
		
	
	/*
	 * Aliases for credentials and certificates.
	 */
	
	// CA root 
	public static final String CA_ROOT_ALIAS = "ca-root";
	
	// Online CA for app client certs 
	public static final String CA_ONLINE_CERT_ALIAS = "ca-online";
	
	// Client cert provided to app 
	public static final String CLIENT_CERT_ALIAS = "client-cert";
	
	
	
	/**
	 * Keystore types.
	 */
	private static final String CLIENT_KEYSTORE_TYPE = "JKS";

	private static final String CLIENT_TRUSTSTORE_TYPE = "JKS";
	
		
	/**
	 * Files:
	 */		
	private static final String CLIENT_KEYSTORE_FILENAME = "keystoreFile.jks";
	
	private static final String CLIENT_TRUSTSTORE_FILENAME = "truststoreFile.jks";

	private static final String PASSWORDS_FILENAME = "passwords.properties";
	
	private File keystoreFile;
	
	private File truststoreFile;
	
	private char[] keystorePassword;
	
	private char[] keyPassword;
	
	private char[] truststorePassword;
	
	private String sender;
	
	private ChatClient client;
	
	private Reporter reporter;
	
	/**
	 * Command line arguments (options and option arguments)
	 */
	public static enum Command {
		
		/*
		 * Admin commands
		 */
		HELP("help"),
		POST("post");
		
		private String value;
		private Command(String v) {
			value = v;
		}
		public String value() {
			return value;
		}
	}
	
	public static enum Option {
		/*
		 * Command-line options:
		 */
		KEYSTORE("keystore"),
		TRUSTSTORE("truststore"),
		PASSWORD_FILE("passwordfile"),
		SERVER_URI("uri"),
		SENDER_ID("sender"),
		SENDER_PASSWORD("password"),
		CLIENT_CERT("cert", false),
		SCRIPT_FILE("scriptfile");
		/*
		 * Arguments:
		 */
		
		private String value;
		private boolean param;
		private Option(String v, boolean p) {
			value = v;
			param = p;
		}
		private Option(String v) {
			this(v,true);
		}
		public String value() {
			return value;
		}
		public boolean isParam() {
			return param;
		}
	}
	
	private void say(String msg, Command arg) {
		reporter.say(String.format(msg, arg.value()));
	}

	private void say(String msg, Option arg) {
		reporter.say(String.format(msg, arg.value()));
	}

	private void say(String msg) {
		reporter.say(msg);
	}

	private void flush() {
		reporter.flush();
	}

	
	private void execute(Driver<Command,Option> driver, String[] args) throws Exception {
		Map<Option,String> options = new HashMap<Option,String>();
		driver.parseOptions(args, options);
		initialize(options);
		
		if (options.containsKey(Option.SCRIPT_FILE)) {
			String scriptName = options.get(Option.SCRIPT_FILE);
			BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(scriptName), StringUtils.CHARSET));
			driver.batch(rd);
		} else {
			driver.interactive(PROMPT);
		}
	}
	
	@Override
	public void execute(Command command, Map<Option, String> options) throws Exception {
		if (command == null) {
			displayHelp();
			return;
		}
		switch (command) {
		case HELP:
			displayHelp();
			break;
		case POST:
			System.out.print("Message: ");
			BufferedReader rd = new BufferedReader(new InputStreamReader(System.in, StringUtils.CHARSET));
			String text = rd.readLine();
			if (text == null) {
				say("Must provide the text of the message to post.");
				return;
			}
			int rc = client.postMessage(sender, text);
			reporter.say("Response status: "+rc);
			break;
		default:
			throw new IllegalArgumentException("Unrecognized command: " + command.name());
		}
	}
	
	public void displayHelp() {
		say("");
		say("Commands for offline keystoreFile:");
		say("--%s: Post a message to the server.", Command.POST);
		// say("");
		// say("Command options:");
		say("");
		say("Command-line options:");
		say("--%s: Client keystoreFile.", Option.KEYSTORE);
		say("--%s: Client truststoreFile.", Option.TRUSTSTORE);
		say("--%s: Properties file with keystoreFile passwords.", Option.PASSWORD_FILE);
		say("--%s: Server URI.", Option.SERVER_URI);
		say("--%s: Message sender.", Option.SENDER_ID);
		say("--%s: Sender password.", Option.SENDER_PASSWORD);
		say("--%s: Use client certificates for authentication.", Option.CLIENT_CERT);
		say("--%s: Name of a file containing a script to execute.", Option.SCRIPT_FILE);
		say("");
		flush();
	}
	
	
	public static void main(String[] args) {
		
		Reporter reporter = Reporter.createReporter();
		
		App app = new App(reporter, args);
		
		Driver<Command,Option> driver = new Driver<Command,Option>(reporter, app);
		
		try {
			app.execute(driver, args);
		} catch (Exception e) {
			// reporter.error(e.getMessage(), e);
			logger.log(Level.SEVERE, "Uncaught exception.", e);
		}

	}

	private Map<String,Command> commands;

	private Map<String,Option> options;
	
//	{
//		Security.addProvider(new BouncyCastleProvider());
//	}

	public App(Reporter reporter, String[] args) {
		this.reporter = reporter;

		commands = new HashMap<String, Command>();
		for (Command command : Command.values()) {
			commands.put(command.value(), command);
		}
				
		options = new HashMap<String, Option>();
		for (Option option : Option.values()) {
			options.put(option.value(), option);
		}
		
	}
	
	@Override
	public Command lookupCommand(String arg) {
		return commands.get(arg);
	}

	@Override
	public Option lookupOption(String arg) {
		return options.get(arg);
	}

	@Override
	public boolean isParameterized(Option option) {
		return option.isParam();
	}
	
	private void initialize(Map<Option,String> options) throws IOException, GeneralSecurityException {

		initKeystores(options);
		
		initClient(options);
		
	}
	
	private void initClient(Map<Option, String> options) throws IOException, GeneralSecurityException {

		String serverAddress = options.get(Option.SERVER_URI);
		URI serverUri;
		if (serverAddress != null) {
			serverUri = URI.create(serverAddress);
		} else {
			throw new IOException("Failed to specify the server URI on the command line!");
		}

		sender = options.get(Option.SENDER_ID);
		if (sender == null) {
			throw new IOException("Failed to specify sender id on the command line!");
		}

		if (options.get(Option.CLIENT_CERT) != null) {

			// TODO use client cert (from keystoreFile) instead of username/password for auth at server
			
			KeyStore keystore = load(keystoreFile, keystorePassword, CLIENT_KEYSTORE_TYPE);
			KeyStore truststore = load(truststoreFile,truststorePassword,CLIENT_TRUSTSTORE_TYPE);
			String algorithm = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
			tmf.init(truststore);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
			kmf.init(keystore, keyPassword);
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

			client = new ChatClient(serverUri,context);

		} else {

			String password = options.get(Option.SENDER_PASSWORD);
			if (password == null) {
				throw new IOException("Failed to specify sender password on the command line!");
			}
			
			// TODO add SSL context for HTTPS (use truststoreFile to authenticate the server)
			KeyStore truststore = load(truststoreFile, truststorePassword, CLIENT_TRUSTSTORE_TYPE);

			client = new ChatClient(serverUri, getAuthContext(truststore), sender, password.toCharArray());
			
		}

	}
	
	private void initKeystores(Map<Option,String> options) throws IOException {
		File passwordsFile;
		
		String passwords = options.get(Option.PASSWORD_FILE);
		if (passwords != null) {
			passwordsFile = new File(passwords);
		} else {
			passwordsFile = new File(PASSWORDS_FILENAME);
		}
		
		String keystoreFileName = options.get(Option.KEYSTORE);
		if (keystoreFileName != null) {
			keystoreFile = new File(keystoreFileName);
		} else {
			keystoreFile = new File(CLIENT_KEYSTORE_FILENAME);
		}
		
		String truststoreFileName = options.get(Option.TRUSTSTORE);
		if (truststoreFileName != null) {
			truststoreFile = new File(truststoreFileName);
		} else {
			truststoreFile = new File(CLIENT_TRUSTSTORE_FILENAME);
		}
		
		loadPasswords(passwordsFile);
	}

	private void loadPasswords(File passwordFile) throws IOException {
		Properties properties = new Properties();
		Reader in = FileUtils.openInputCharFile(passwordFile);
		properties.load(in);
		in.close();

		String password = properties.getProperty(CLIENT_KEYSTORE_PASSWORD);
		if (password == null) {
			say("No keystoreFile password provided: " + CLIENT_KEYSTORE_PASSWORD);
			throw new IOException("Failed to provide keystoreFile password.");
		} else {
			keystorePassword = password.toCharArray();
		}

		password = properties.getProperty(CLIENT_KEY_PASSWORD);
		if (password == null) {
			say("No key password provided: " + CLIENT_KEY_PASSWORD);
			throw new IOException("Failed to provide key password.");
		} else {
			keyPassword = password.toCharArray();
		}

		password = properties.getProperty(CLIENT_TRUSTSTORE_PASSWORD);
		if (password == null) {
			say("No truststoreFile password provided: " + CLIENT_TRUSTSTORE_PASSWORD);
			throw new IOException("Failed to provide truststoreFile password.");
		} else {
			truststorePassword = password.toCharArray();
		}
	}
	
	public static KeyStore load(File store, char[] password, String keystoreType) throws GeneralSecurityException {
		// TODO complete this
		try {
			KeyStore keystore = KeyStore.getInstance(keystoreType);
			if (!store.exists()) {
				logger.info("Store does not exist, initializing " + store.getAbsolutePath());
				keystore.load(null, null);
			} else {
				InputStream in = new FileInputStream(store);
				keystore.load(in, password);
				in.close();
			}
			return keystore;
		} catch (KeyStoreException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (CertificateException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (NoSuchAlgorithmException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (FileNotFoundException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		} catch (IOException e) {
			throw ExceptionWrapper.wrap(GeneralSecurityException.class, e);
		}
	}

	public static SSLContext getAuthContext(KeyStore truststore) throws GeneralSecurityException {
		// TODO complete this
		String algorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
		tmf.init(truststore);
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null,tmf.getTrustManagers(), null);
		return context;
	}
	
}
