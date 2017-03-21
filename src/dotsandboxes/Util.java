package dotsandboxes;

import java.io.StringReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class Util {

	// ============================== //
	// ==========  String  ========== //
	// ============================== //

	public static JsonObjectBuilder parseJSON(String json) {
		StringReader stringReader = new StringReader(json);
		JsonReader jsonReader = Json.createReader(stringReader);

		JsonObjectBuilder result = Json.createObjectBuilder();
		for (Entry<String, JsonValue> entry : jsonReader.readObject().entrySet())
			result.add(entry.getKey(), entry.getValue());

		stringReader.close();
		jsonReader.close();

		return result;
	}

	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0 || s.trim().length() == 0;
	}

	public static int count(String haystack, String needle) {
		int count = 0;

		Matcher m = Pattern.compile(needle).matcher(haystack);
		while (m.find())
			++count;

		return count;
	}


	// ============================== //
	// ========= Encryption ========= //
	// ============================== //

	public static String encryptMD5(String password) {
		if (password == null)
			return "";

		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			BigInteger number = new BigInteger(1, md.digest(password.getBytes()));
			return number.toString(16);
		} catch (NoSuchAlgorithmException e) {
			return password;
		}
	}
}
