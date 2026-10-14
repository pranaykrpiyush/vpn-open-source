package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.VpnProtocol
import com.example.model.VpnServer

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val ip: String,
    val port: Int,
    val protocol: String,
    val pingMs: Int,
    val speedMbps: Double,
    val isFavorite: Boolean,
    val isCustom: Boolean,
    val configData: String
) {
    fun toVpnServer(): VpnServer {
        val proto = try {
            VpnProtocol.valueOf(protocol)
        } catch (_: Exception) {
            VpnProtocol.OPENVPN_UDP
        }
        return VpnServer(
            id = id,
            name = name,
            countryCode = countryCode,
            countryName = countryName,
            ip = ip,
            port = port,
            protocol = proto,
            pingMs = pingMs,
            speedMbps = speedMbps,
            isFavorite = isFavorite,
            isCustom = isCustom,
            configData = configData
        )
    }

    companion object {
        fun fromVpnServer(server: VpnServer): ServerEntity {
            return ServerEntity(
                id = server.id,
                name = server.name,
                countryCode = server.countryCode,
                countryName = server.countryName,
                ip = server.ip,
                port = server.port,
                protocol = server.protocol.name,
                pingMs = server.pingMs,
                speedMbps = server.speedMbps,
                isFavorite = server.isFavorite,
                isCustom = server.isCustom,
                configData = server.configData
            )
        }
    }
}
