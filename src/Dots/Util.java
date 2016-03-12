package Dots;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

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
				ps.setString(i + 1, args[i]);// SQL indexes start at 1

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
