package Dots;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

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

	public static void query(String query, Iterable<String> args, Consumer<ResultSet> callback) {
		if (getDBConnection() == null)
			return;
	
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getDBConnection().prepareStatement(query);

			int i = 1; // SQL indexes start at 1
			for (String arg : args)
				ps.setString(i++, arg);

			rs = ps.executeQuery();
			callback.accept(rs);
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
			return number.toString();
		} catch (NoSuchAlgorithmException e) {
			return password;
		}
	}
}
