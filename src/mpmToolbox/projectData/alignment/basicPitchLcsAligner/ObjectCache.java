package mpmToolbox.projectData.alignment.basicPitchLcsAligner;


import java.io.*;
import java.sql.*;

/**
 * The ObjectCache class is a Java class that provides an in-memory cache for storing objects.
 * The cache has a fixed maximum size, specified in the constructor. When the maximum size is reached,
 * the least recently accessed object is removed from the cache to make space for new objects.
 * The cache is implemented using SQLite, which is a lightweight, in-process relational database engine
 * that allows the cache to persist even if the program is restarted.
 * @author Vladimir Viro
 */
class ObjectCache {
    private final String tableName = "object_cache";
    private final String idColumn = "id";
    private final String objectColumn = "object";
    private final String sizeColumn = "size";
    private final String lastAccessColumn = "last_access";
    private final String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + idColumn + " TEXT PRIMARY KEY, " + objectColumn + " BLOB, " + sizeColumn + " INTEGER, " + lastAccessColumn + " INTEGER)";
    private final String insertSql = "INSERT INTO " + tableName + " (" + idColumn + ", " + objectColumn + ", " + sizeColumn + ", " + lastAccessColumn + ") VALUES (?, ?, ?, ?)";
    private final String updateSql = "UPDATE " + tableName + " SET " + objectColumn + " = ?, " + sizeColumn + " = ?, " + lastAccessColumn + " = ? WHERE " + idColumn + " = ?";
    private final String selectSql = "SELECT " + objectColumn + ", " + sizeColumn + " FROM " + tableName + " WHERE " + idColumn + " = ?";
    private final String deleteSql = "DELETE FROM " + tableName + " WHERE " + idColumn + " = ?";
    private final String deleteOldestSql = "DELETE FROM " + tableName + " WHERE " + lastAccessColumn + " = (SELECT MIN(" + lastAccessColumn + ") FROM " + tableName + ")";
    private final String getSizeSql = "SELECT SUM(" + sizeColumn + ") FROM " + tableName;
    private final String clearSql = "DELETE FROM " + tableName;
    private final String updateLastAccessSql = "UPDATE " + tableName + " SET " + lastAccessColumn + " = ? WHERE " + idColumn + " = ?";
    private final String getLastAccessSql = "SELECT " + lastAccessColumn + " FROM " + tableName + " WHERE " + idColumn + " = ?";
    private final String getOldestLastAccessSql = "SELECT MIN(" + lastAccessColumn + ") FROM " + tableName;
    private final String getOldestIdSql = "SELECT " + idColumn + " FROM " + tableName + " WHERE " + lastAccessColumn + " = (SELECT MIN(" + lastAccessColumn + ") FROM " + tableName + ")";

    private Connection connection;
    private PreparedStatement insertStatement;
    private PreparedStatement updateStatement;
    private PreparedStatement selectStatement;
    private PreparedStatement deleteStatement;
    private PreparedStatement deleteOldestStatement;
    private PreparedStatement getSizeStatement;
    private PreparedStatement clearStatement;
    private PreparedStatement updateLastAccessStatement;
    private PreparedStatement getLastAccessStatement;
    private PreparedStatement getOldestLastAccessStatement;
    private PreparedStatement getOldestIdStatement;

    private final long maxSize;

    /**
     * Constructs a new in-memory ObjectCache instance with default maximum size.
     */
    public ObjectCache() {
        this(":memory:", (int)Math.pow(2, 28));
    }

    /**
     * Constructs a new ObjectCache instance with the specified database file path and maximum size.
     * @param dbFilePath The file path to the SQLite database file that will be used to store the cache.
     * @param maxSize The maximum size of the cache, in bytes.
     */
    public ObjectCache(String dbFilePath, long maxSize) {
        this.maxSize = maxSize;
        this.init(dbFilePath);
    }

    /**
     * Initializes the ObjectCache instance by creating the necessary database tables and preparing the SQL statements that will be used to interact with the database.
     * @param dbFilePath The file path to the SQLite database file that will be used to store the cache.
     */
    private void init(String dbFilePath) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            statement.executeUpdate(createTableSql);
            statement.close();
            insertStatement = connection.prepareStatement(insertSql);
            updateStatement = connection.prepareStatement(updateSql);
            deleteStatement = connection.prepareStatement(deleteSql);
            deleteOldestStatement = connection.prepareStatement(deleteOldestSql);
            getSizeStatement = connection.prepareStatement(getSizeSql);
            clearStatement = connection.prepareStatement(clearSql);
            updateLastAccessStatement = connection.prepareStatement(updateLastAccessSql);
            getLastAccessStatement = connection.prepareStatement(getLastAccessSql);
            getOldestLastAccessStatement = connection.prepareStatement(getOldestLastAccessSql);
            getOldestIdStatement = connection.prepareStatement(getOldestIdSql);
        } catch (SQLException e) {
            e.printStackTrace();
            if (!dbFilePath.equals(":memory:")) {
                System.out.println("initializing cache in memory");
                init(":memory:");
            }
        }
    }

    /**
     * Stores the specified object in the cache with the given ID.
     * If an object with the same ID already exists in the cache, it will be overwritten.
     * If the cache is full, the least recently accessed object will be removed from the cache
     * to make space for the new object.
     *
     * @param id The ID of the object to be stored in the cache.
     * @param obj The object to be stored in the cache.
     */
    public void put(String id, Object obj) throws SQLException, IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        byte[] bytes = baos.toByteArray();
        int size = bytes.length;
        long lastAccess = System.currentTimeMillis();
        if (get(id) != null) {
            updateStatement.setBytes(1, bytes);
            updateStatement.setInt(2, size);
            updateStatement.setLong(3, lastAccess);
            updateStatement.setString(4, id);
            updateStatement.executeUpdate();
        } else {
            insertStatement.setString(1, id);
            insertStatement.setBytes(2, bytes);
            insertStatement.setInt(3, size);
            insertStatement.setLong(4, lastAccess);
            insertStatement.executeUpdate();
        }
        while (getSize() > maxSize) {
            deleteOldest();
        }
        connection.commit();
    }

    public Object get(String id) throws SQLException, IOException, ClassNotFoundException {
        selectStatement = connection.prepareStatement(selectSql);
        selectStatement.setString(1, id);
        ResultSet rs = selectStatement.executeQuery();
        if (rs.next()) {
            byte[] bytes = rs.getBytes(1);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();
            ois.close();
            long lastAccess = System.currentTimeMillis();
            updateLastAccessStatement.setLong(1, lastAccess);
            updateLastAccessStatement.setString(2, id);
            updateLastAccessStatement.executeUpdate();
            connection.commit();
            return obj;
        } else {
            return null;
        }
    }

    public void delete(String id) throws SQLException {
        deleteStatement.setString(1, id);
        deleteStatement.executeUpdate();
        connection.commit();
    }

    public void deleteOldest() throws SQLException {
        deleteOldestStatement.executeUpdate();
        connection.commit();
    }

    public int getSize() throws SQLException {
        ResultSet rs = getSizeStatement.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            return 0;
        }
    }

    public void clear() throws SQLException {
        clearStatement.executeUpdate();
        connection.commit();
    }

    public void close() {
        try {
            insertStatement.close();
            updateStatement.close();
            selectStatement.close();
            deleteStatement.close();
            deleteOldestStatement.close();
            getSizeStatement.close();
            clearStatement.close();
            updateLastAccessStatement.close();
            getLastAccessStatement.close();
            getOldestLastAccessStatement.close();
            getOldestIdStatement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}