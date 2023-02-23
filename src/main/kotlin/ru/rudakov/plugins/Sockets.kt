package ru.rudakov.plugins

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.server.application.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.util.*

fun Application.configureSockets() {
    
    

}
/**
 * Two mains are provided, you must first start EchoApp.Server, and then EchoApp.Client.
 * You can also start EchoApp.Server and then use a telnet client to connect to the echo server.
 */
object EchoApp {
    val selectorManager = ActorSelectorManager(Dispatchers.IO)
    const val defaultPort = 8082
    const val defaultIp = "192.168.88.100"

    object Server {
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                val serverSocket = aSocitket(selectorManager).tcp().bind(defaultIp, port = defaultPort)
                val byteArray = ByteArray(1024)
                println("Echo Server listening at ${serverSocket.localAddress}")
                while (true) {
                    byteArray.fill(0)
                    var login: String = ""
                    val socket = serverSocket.accept()
                    println("Accepted $socket with address ${socket.remoteAddress}")
                    launch {
                        val read = socket.openReadChannel()
                        val write = socket.openWriteChannel(autoFlush = true)
                        try {
                            while (true) {
                                read.readAvailable(byteArray)
                                println("Server read: ${String(byteArray, Charsets.US_ASCII)}")
                                when(byteArray[0].toInt()) {
                                    97 -> {
                                        val data = byteArray.slice(2 until byteArray.indexOf(0.toByte())).toByteArray()
                                        val str = String(data, Charsets.US_ASCII)
                                        val lp = str.split(" ")
                                        login = lp[0]
                                        val hash = getHash(getPassword(login), lp[1])
                                        println("Server write: $hash")
                                        hash.forEachIndexed { ind, el ->
                                            byteArray[ind] = el.toInt().toByte()
                                        }
                                        write.writeAvailable(byteArray)
                                    }
                                    99 -> {
                                        val record = "$login c ${System.currentTimeMillis()}\n"
                                        File("db").appendBytes(record.toByteArray())
                                    }
                                    111 -> {
                                        val record = "$login o ${System.currentTimeMillis()}\n"
                                        File("db").appendBytes(record.toByteArray())
                                    }
                                    115 -> {
                                        if(File("db").exists())
                                        {
                                            var fileByteArray = File("db").readBytes()
                                            if(fileByteArray.size > 1024) {
                                                fileByteArray = fileByteArray.sliceArray(fileByteArray.size-1024 until  fileByteArray.size-1)
                                            }
                                            fileByteArray.forEachIndexed { ind, el ->
                                                byteArray[ind] = el
                                            }
                                            println("Server write: ${String(byteArray, Charsets.US_ASCII)}")
                                            write.writeAvailable(byteArray)
                                        }
                                    }
                                    112 -> {
                                        val password = String(byteArray.sliceArray(2..5), Charsets.US_ASCII)
                                        if (users.containsValue(password))
                                            byteArray[0] = 1.toByte()
                                        else {
                                            byteArray[0] = 0.toByte()
                                        }
                                        println("Server write: ${String(byteArray, Charsets.US_ASCII)}")
                                        write.writeAvailable(byteArray)
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            socket.close()
                        }
                    }
                }
            }
        }

        fun getPassword(login: String): String {
            return users[login] ?: ""
        }

        fun getHash(password: String, openKey: String): String {
            val m = MessageDigest.getInstance("MD5")
            m.reset()
            val digest = m.digest((password + openKey).toByteArray(Charsets.US_ASCII))
            val bigInt = BigInteger(1, digest)
            var hashtext: String = bigInt.toString(16)
            while (hashtext.length < 32) {
                hashtext = "0$hashtext"
            }
            return hashtext
        }
    }

    val users = mapOf(
        "Vova" to "1234",
        "Dima" to "6346",
        "Danya" to "0000"
    )

}

object TlsRawSocket {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val selectorManager = ActorSelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect("www.google.com", port = 443).tls(coroutineContext = coroutineContext)
            val write = socket.openWriteChannel()
            val EOL = "\r\n"
            write.writeStringUtf8("GET / HTTP/1.1${EOL}Host: www.google.com${EOL}Connection: close${EOL}${EOL}")
            write.flush()
            println(socket.openReadChannel().readRemaining().readBytes().toString(Charsets.UTF_8))
        }
    }
}

