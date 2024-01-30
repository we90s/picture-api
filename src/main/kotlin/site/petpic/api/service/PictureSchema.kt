package site.petpic.api.service

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime


@Serializable
data class ExposedPicture(val email: String, val url: String)
@Serializable
data class ExposedStatus(val email: String, val status: Boolean)

class ServicePicture(private val database: Database) {
    object Picture : Table() {
        val id = integer("ID").autoIncrement()
        val email = varchar("USER_MAIL", length = 50)
        val url = varchar("IMAGE_URL", length = 120)
        val status = bool("STATUS").clientDefault { false }
        val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
        val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }


        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Picture)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(picture: ExposedPicture): Int = dbQuery {
        Picture.insert {
            it[email] = picture.email
        }[Picture.id]
    }

    suspend fun read(email: String): List<ExposedPicture> {
        return dbQuery {
            Picture.select { Picture.email eq email and (Picture.status eq false) }
                .map { ExposedPicture(it[Picture.email], it[Picture.url]) }
        }
    }

    suspend fun status(email: String): ExposedStatus {
        return dbQuery {
            Picture.select { Picture.email eq email }
                .map { ExposedStatus(it[Picture.email], it[Picture.status]) }
                .reversed()
                .first()
        }
    }

//    suspend fun update(id: Int, user: ExposedPicture) {
//        dbQuery {
//            Users.update({ Users.id eq id }) {
//                it[name] = user.name
//                it[age] = user.age
//            }
//        }
//    }

    suspend fun delete(id: Int) {
        dbQuery {
            Picture.deleteWhere { Picture.id.eq(id) }
        }
    }
}
