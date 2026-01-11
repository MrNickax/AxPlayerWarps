package com.artillexstudios.axplayerwarps.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;

public class MySQL extends Base {

    private HikariDataSource dataSource;

    @Override
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public String getType() {
        return "MySQL";
    }

    @Override
    public void setup() {
        final HikariConfig hConfig = new HikariConfig();

        hConfig.setPoolName("axplayerwarps-pool");

        hConfig.setMaximumPoolSize(CONFIG.getInt("mysql.pool.maximum-pool-size"));
        hConfig.setMinimumIdle(CONFIG.getInt("mysql.pool.minimum-idle"));
        hConfig.setMaxLifetime(CONFIG.getInt("mysql.pool.maximum-lifetime"));
        hConfig.setKeepaliveTime(CONFIG.getInt("mysql.pool.keepalive-time"));
        hConfig.setConnectionTimeout(CONFIG.getInt("mysql.pool.connection-timeout"));

        hConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hConfig.setJdbcUrl("jdbc:mysql://" + CONFIG.getString("mysql.address") + ":"+ CONFIG.getString("mysql.port") +"/" + CONFIG.getString("mysql.database"));
        hConfig.addDataSourceProperty("user", CONFIG.getString("mysql.username"));
        hConfig.addDataSourceProperty("password", CONFIG.getString("mysql.password"));

        this.dataSource = new HikariDataSource(hConfig);
        super.setup();
    }

    @Override
    public void disable() {
        try {
            dataSource.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}