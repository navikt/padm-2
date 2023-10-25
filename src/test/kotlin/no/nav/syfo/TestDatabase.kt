package no.nav.syfo

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.db.DatabaseInterface
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.SQLException
import java.sql.Timestamp

class TestDatabase : DatabaseInterface {
    private lateinit var pg: EmbeddedPostgres

    override val connection: Connection
        get() = pg.postgresDatabase.connection.apply { autoCommit = false }

    fun start() {
        pg = EmbeddedPostgres.start()

        Flyway.configure().run {
            dataSource(pg.postgresDatabase).load().migrate()
        }
    }

    fun stop() {
        pg.close()
    }
}

class TestDatabaseNotResponding : DatabaseInterface {

    override val connection: Connection
        get() = throw Exception("Not working")

    fun stop() {
    }
}

fun DatabaseInterface.dropData() {
    val queryList = listOf(
        """
        DELETE FROM BEHANDLINGSUTFALL
        """.trimIndent(),
        """
        DELETE FROM DIALOGMELDINGDOKUMENT
        """.trimIndent(),
        """
        DELETE FROM DIALOGMELDINGOPPLYSNINGER
        """.trimIndent(),
    )
    this.connection.use { connection ->
        queryList.forEach { query ->
            connection.prepareStatement(query).execute()
        }
        connection.commit()
    }
}

fun DatabaseInterface.updateSendtApprec(dialogmeldingId: String, timestamp: Timestamp) {
    connection.use { connection ->
        connection.prepareStatement(
            """
                UPDATE DIALOGMELDINGOPPLYSNINGER
                SET apprec=?
                WHERE ID=?;
                """
        ).use {
            it.setTimestamp(1, timestamp)
            it.setString(2, dialogmeldingId)
            val updated = it.executeUpdate()
            if (updated != 1) {
                throw SQLException("Expected a single row to be updated, got update count $updated")
            }
        }
        connection.commit()
    }
}
