package site.petpic.api.model

import kotlinx.serialization.Serializable

@Serializable
data class JwtPayload(
    val sub: String,
    val iat : Long,
    val exp : Long,
)
{
}