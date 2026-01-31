package org.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Database factory for initializing and managing the SQLite database connection.
 * Handles database initialization, schema creation, and connection pooling.
 */
object DatabaseFactory {
    
    private var isInitialized = false
    
    /**
     * Initialize the database connection and create tables if they don't exist.
     * 
     * @param databasePath Path to the SQLite database file (default: data/911-calls.db)
     */
    fun init(databasePath: String = "data/911-calls.db") {
        if (isInitialized) {
            logger.warn { "Database already initialized. Skipping re-initialization." }
            return
        }
        
        logger.info { "Initializing SQLite database at: $databasePath" }
        
        // Ensure the data directory exists
        val dbFile = File(databasePath)
        dbFile.parentFile?.mkdirs()
        
        // Configure HikariCP connection pool for SQLite
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$databasePath"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 3 // SQLite doesn't benefit from many connections
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_SERIALIZABLE"
            
            // SQLite-specific settings
            addDataSourceProperty("foreign_keys", "true")
            addDataSourceProperty("journal_mode", "WAL") // Write-Ahead Logging for better concurrency
            
            validate()
        }
        
        try {
            val dataSource = HikariDataSource(config)
            Database.connect(dataSource)
            
            // Create tables if they don't exist
            transaction {
                SchemaUtils.create(
                    CallRecords,
                    Transcripts,
                    ExtractedEntities
                )
            }
            
            isInitialized = true
            logger.info { "✅ Database initialized successfully. Tables created." }
            
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to initialize database" }
            throw e
        }
    }
    
    /**
     * Check if the database has been initialized.
     */
    fun isInitialized(): Boolean = isInitialized
}
