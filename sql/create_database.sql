/* This script will create the database */
CREATE DATABASE IF NOT EXISTS dots;

USE dots;

/* Creating the table of users */
CREATE TABLE IF NOT EXISTS users (
	username varchar(255) NOT NULL,
	password varchar(255) NOT NULL,
	played INT NOT NULL DEFAULT 0,
	won INT NOT NULL DEFAULT 0,
	points INT NOT NULL DEFAULT 0,
	games varchar(255) NOT NULL,
	PRIMARY KEY (username)
);
