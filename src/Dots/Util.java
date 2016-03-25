package Dots;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class Util {

	// ============================== //
	// ========== Database ========== //
	// ============================== //

	private static Connection dbConnection = null;

	public static void openDBConnection() {
		if (dbConnection != null)
			return;

		try {
			Class.forName(Defaults.dbDriver);
			dbConnection = DriverManager.getConnection(Defaults.dbURL, Defaults.dbUsername, Defaults.dbPassword);
		} catch (SQLException e) {
		} catch (ClassNotFoundException e) {
		}
	}

	public static void closeDBConnection() {
		if (dbConnection == null)
			return;

		try {
			dbConnection.close();
			dbConnection = null;
		} catch (SQLException e) {
		}
	}

	public static Connection getDBConnection() {
		if (dbConnection == null)
			openDBConnection();

		return dbConnection;
	}

	public static void execute(String query) {
		if (getDBConnection() == null)
			return;

		PreparedStatement ps = null;
		try {
			ps = getDBConnection().prepareStatement(query);
			ps.execute();
		} catch (SQLException e) {
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
				}
			}

			closeDBConnection();
		}
	}

	public static <T> T query(String query, String[] args, Function<ResultSet, T> callback) {
		if (getDBConnection() == null)
			return null;

		T result = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getDBConnection().prepareStatement(query);

			for (int i = 0; i < args.length; ++i)
				ps.setString(i + 1, args[i]); // SQL indexes start at 1

			rs = ps.executeQuery();
			result = callback.apply(rs);
		} catch (SQLException e) {
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}

			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
				}
			}

			closeDBConnection();
		}

		return result;
	}


	// ============================== //
	// ==========   File   ========== //
	// ============================== //

	public static String getFileContents(String path) {
		if (isEmpty(path))
			return null;

		try {
			return new String(Files.readAllBytes(Paths.get(path)));
		} catch (IOException e) {
		}
		return null;
	}

	public static void writeToFile(String path, String content) {
		if (isEmpty(path))
			return;

		try {
			Files.write(Paths.get(path), content.getBytes());
		} catch (IOException e) {
		}
	}


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
