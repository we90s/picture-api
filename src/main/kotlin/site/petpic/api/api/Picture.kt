package site.petpic.api.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*

import site.petpic.api.model.JwtPayload
import site.petpic.api.service.ExposedPicture
import site.petpic.api.service.ServicePicture
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.yaml.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.exposed.sql.Database
import site.petpic.api.service.UploadFile
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonObject

import java.util.*


fun Application.picture() {
    install(ContentNegotiation) {
        json(Json {
        prettyPrint = true
    })
    }
    val config = getProp()
 //   val authDomain = config.property("authDomain").getString()
    routing {
        get("/") {
            call.respond(HttpStatusCode.OK)
        }
    }
    install(Authentication) {
        bearer("auth-bearer") {
            // realm = "Access to the '/' path"


            authenticate { tokenCredential ->
                if (httpGet(config, tokenCredential.token) == HttpStatusCode.OK) {
                    UserIdPrincipal("User")
                } else {
                    null
                }
            }
        }
    }

    val bootstrap = config.property("kafka.bootstrap").getString()
    val jaasConfig = config.property("kafka.jaasConfig").getString()
    val env = config.property("env").getString()
    val kafkaConfigs = Properties().apply {
        setProperty("bootstrap.servers", bootstrap)
        setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    }
    if(env != "local") {
        kafkaConfigs.setProperty("sasl.mechanism", "SCRAM-SHA-256")
        kafkaConfigs.setProperty("security.protocol", "SASL_SSL")
        kafkaConfigs.setProperty("sasl.jaas.config", jaasConfig)
    }
    val url = config.property("ktor.database.url").getString()
    val user = config.property("ktor.database.user").getString()
    val password = config.property("ktor.database.password").getString()

    val database = Database.connect(
        url = url,
        user = user,
        driver = "org.postgresql.Driver",
        password = password
    )

    val pictureService = ServicePicture(database)
    val producer: KafkaProducer<String, String> = KafkaProducer<String, String>(kafkaConfigs)
    routing {
// CORS 추가하기
        authenticate("auth-bearer") {
            post("/upload") {
                val jwtPayload = call.request.header(HttpHeaders.Authorization)
                    ?.split(" ")?.get(1)
                    ?.split(".")?.get(1)
                    ?.decodeBase64String()
                val userMail = Json.decodeFromString<JwtPayload>(jwtPayload!!).sub
          //      val picture = pictureService.status(userMail)

                if (pictureService.status(userMail)?.status == false) {
                    return@post call.respond(HttpStatusCode(418, "In Progressing"))
                }

                val multipartData = call.receiveMultipart()
                var prompt = ""
                var imageBytes: ByteArray = byteArrayOf()
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            prompt = part.value
                            return@forEachPart
                        }

                        is PartData.FileItem -> {
                            val fileName = part.originalFileName as String
                            val extension = fileName.split(".")[1]

                            if (extension != "jpeg" &&
                                extension != "jpg" &&
                                extension !=("png")
                            ) {

                                return@forEachPart call.respond(HttpStatusCode(499, "Invalid file extension"))
                            }
                            imageBytes = part.streamProvider().readBytes()

                            //return@forEachPart
                            //uploadFile.uploadGCS(fileBytes)
                            //    File("uploads/$fileName").writeBytes(fileBytes)
                        }

                        else -> {
                            return@forEachPart call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    part.dispose()
                }

                val uploadFile = UploadFile()
                val animal = uploadFile.callVisionAPI(imageBytes)
                if (animal.equals(null)) {
                    return@post call.respond(HttpStatusCode(498, "Invalid Image"))
                }
                val originKey = uploadFile.uploadGCS(imageBytes, userMail)
                if (originKey.equals(null)) {
                    return@post call.respond(HttpStatusCode(599, "GCS Upload Error"))
                }

            //    val pictureCreate = call.receive<ExposedPicture>()
                //  val id =


                try {
                    pictureService.create(ExposedPicture(email = userMail, url= ""))
                }
                catch (e: Exception) {
                    return@post call.respond(HttpStatusCode(598, "DB Error"))
                }
                try {
                    producer.send(
                        ProducerRecord
                            (
                            "originImage2",
                            mapOf("prompt" to prompt, "origin_key" to originKey, "user_mail" to userMail).toString()
                        )
                    )
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode(597, "Kafka Error"))
                }
            }


        }
        authenticate("auth-bearer") {
            get("/pictures"){
                val jwtPayload = call.request.header(HttpHeaders.Authorization)
                    ?.split(" ")?.get(1)
                    ?.split(".")?.get(1)
                    ?.decodeBase64String()
                val userMail = Json.decodeFromString<JwtPayload>(jwtPayload!!).sub
                val pictures = pictureService.read(userMail)
                if (pictures != null) {
                    if (pictures.isNotEmpty()) {
                        call.respond(HttpStatusCode.OK, pictures)
                    }
                }else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
    environment.monitor.subscribe(ApplicationStopping) { application ->
        producer.flush()
        producer.close()
        application.environment.log.info("Server is stopping")
        application.environment.monitor.unsubscribe(ApplicationStopping) {}

    }

}
suspend fun httpGet(config: YamlConfig, token: String): HttpStatusCode {
  //  val authDomain = config.property("authDomain").getString()
    val client = HttpClient(CIO)

    return client.get("http://petpic-auth-service/auth/validate"){

        header(HttpHeaders.Authorization, "Bearer $token")
    }.status

}

fun getProp(): YamlConfig {
    return if(YamlConfig("/app/conf/application.yaml") != null) {
        YamlConfig("/app/conf/application.yaml")!!
    }
    else{
        YamlConfig("application.yaml")!!
    }
}