package net.tiny.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see https://github.com/hmalphettes/cloudfoundry-vcapservices-java-helper
 */
public final class VcapServices {

	private static final Logger LOGGER = Logger.getLogger(VcapServices.class.getName());

	public static final String ENV_VCAP_SERVICES = "VCAP_SERVICES";

	public static class Credentials extends Properties {
		private static final long serialVersionUID = 1L;
	}

	public static class VcapService {
		private String type;
		private String name;
		private String label;
		private String plan;
		private String provider;
		private List<String> tags = new ArrayList<>();
		private Credentials credentials = new Credentials();

		public VcapService(Map<String, Object> map) {
			map.keySet()
			   .forEach(n -> parse(n, map.get(n)));
		}

		@SuppressWarnings("unchecked")
		void parse(String key, Object value) {
			switch(key) {
			case "name":
				name = value.toString();
				break;
			case "label":
				label = value.toString();
				break;
			case "plan":
				plan = value.toString();
				break;
			case "provider":
				provider = value.toString();
				break;
			case "credentials":
				credentials.putAll((Map<String, Object>)value);
				break;
			case "tags":
				tags.addAll((List<String>)value);
				break;
			default:
				break;
			}
		}

		public String getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public String getLabel() {
			return label;
		}
		public String getProvider() {
			return provider;
		}
		public String getPlan() {
			return plan;
		}
		public String[] getTags() {
			return tags.toArray(new String[tags.size()]);
		}

		public String getCredential(String key) {
			Object value = credentials.get(key);
			if(null == value)
				return null;
			return String.valueOf(value);
		}

		public Credentials getCredentials() {
			return credentials;
		}
	}

	/** Service name index */
	private final Map<String, VcapService> services = new HashMap<String, VcapService>();

	/** Service type index */
	private final Map<String, List<VcapService>> types = new LinkedHashMap<String, List<VcapService>>();

	@SuppressWarnings("unchecked")
	private VcapServices(String json) {
		if (json == null || json.isEmpty()) {
			throw new IllegalArgumentException("Not found env variable 'VCAP_SERVICES' ");
		}
		final Map<String, Object> map = JsonParser.unmarshal(json, Map.class);
		if (map.isEmpty()) {
			throw new IllegalArgumentException(String.format("Invalid 'VCAP_SERVICES' : %s", json));
		}
		map.keySet()
		   .forEach(vs -> parse(vs, map.get(vs)));
	}

	@SuppressWarnings("unchecked")
	void parse(String type, Object vcaps) {
		final List<Object> list = (List<Object>)vcaps;
		List<VcapService> typed = types.get(type);
		if (null == typed) {
			typed = new ArrayList<>();
			types.put(type, typed);
		}
		for (Object s : list) {
			VcapService vcap = new VcapService((Map<String, Object>)s);
			typed.add(vcap);
			services.put(vcap.getName(), vcap);
		}
	}

	/**
	 * Helper method: traverses the vcap services and
	 * returns the first one that name matches the argument.
	 * @param name
	 * @return The first service with this name or null.
	 */
	public VcapService getVcapService(String name) {
		return services.get(name);
	}

	/**
	 * @param type
	 * @return The services of a given type.
	 */
	public List<VcapService> geVcapServicesByType(String type) {
		return types.get(type);
	}

	/**
	 * Helper method: traverses the vcap services and
	 * returns the first one that name matches the argument.
	 * @param name
	 * @return The first service with this name or null.
	 */
	public Credentials getCredentials(String name) {
		VcapService vcaps = getVcapService(name);
		if (vcaps != null) {
			return vcaps.getCredentials();
		}
		return null;
	}

	public Properties getAllCredentials() {
		final String format = "vcap.services.%s.credentials.%s";
		final Properties prop = new Properties();
		for (String name : services.keySet()) {
			VcapService vcap = services.get(name);
			Credentials credentials = vcap.getCredentials();
			for (Object key : credentials.keySet()) {
				String vkey = String.format(format, vcap.getName(), key);
				prop.put(vkey, credentials.get(key));
			}
		}
		return prop;
	}

	public void applyCredentials(Configuration config) {
		config.append(getAllCredentials());
	}

	public int size() {
		return services.size();
	}

	public static void apply(Configuration config) {
		String json = loadVcapServices(ENV_VCAP_SERVICES);
		if( json != null) {
			final VcapServices vcaps = valueOf(json);
			if(vcaps != null) {
				vcaps.applyCredentials(config);
			}
		}
	}

	public static VcapServices get() {
		return valueOf(loadVcapServices(ENV_VCAP_SERVICES));
	}

	public static VcapServices valueOf(String json) {
		try {
			return new VcapServices(json);
		} catch (Exception e) {
			LOGGER.warning(e.getMessage());
			return null;
		}
	}

	/** 加载环境变量定义的VCAP_SERVICES  **/
	static String loadVcapServices(String env) {
		String json = System.getenv(env);
		if(null == json) {
			json = System.getProperty(env);
		}
		return json;
	}

	/**
	 * recursively substitute the ${sysprop} by their actual system property.
	 * ${sysprop,defaultvalue} will use 'defaultvalue' as the value if no sysprop is defined.
	 * Not the most efficient code but we are shooting for simplicity and speed of development here.
	 *
	 * @param value
	 * @return
	 */
	public static String resolvePropertyValue(String value)	{
		return resolvePropertyValue(value, System.getProperties());
	}

	static String resolvePropertyValue(String value, Properties properties)	{
        Configuration.VariablesReplacement replacement = new Configuration.VariablesReplacement() {
            @Override
            String replace(String var) {
            	String[] keys = var.split(",");
            	if(keys.length > 1) {
            		return String.valueOf(properties.getOrDefault(keys[0], keys[1].trim()));
            	} else {
            		return properties.getProperty(keys[0]);
            	}
            }
        };
        return replacement.replaceValue(value);
	}

	/**
	 * Simple wrapper class for a regexp pattern to keep track of
	 * whether we want a match or a mismatch
	 */
	static class NegatablePattern {
		private final Pattern pattern;
		private final boolean negated;

		/**
		 * Resolves a system-property or env-variable with the ${KEY,defult-value} notation.
		 * If the string starts with a '!' the pattern is negated.
		 * If the remaining of the string starts and finishes with '/' then make a regexp out of it.
		 * Otherwise makes a literal match regexp.
		 * @param regexp
		 */
		public NegatablePattern(String regexp) {
			String regex = resolvePropertyValue(regexp);
			if (regex.startsWith("!")) {
				negated = true;
				regex = regex.substring(1);
			} else {
				negated = false;
			}
			if (regex.startsWith("/") && regex.endsWith("/")) {
				regex = regex.substring(1, regex.length() -1);
				pattern = Pattern.compile(regex);
			} else {
				pattern = Pattern.compile(Pattern.quote(regex));
			}
		}

		public NegatablePattern(Pattern p, boolean n) {
			pattern = p;
			negated = n;
		}

		public boolean matches(String input) {
			Matcher m = pattern.matcher(input);
			if (m.matches())
				return !negated;
			else
				return negated;
		}
	}
}
