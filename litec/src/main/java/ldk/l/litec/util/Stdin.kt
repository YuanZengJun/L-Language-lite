package ldk.l.litec.util

import java.io.*
import java.nio.charset.StandardCharsets

object Stdin {
    private val ENCODING = StandardCharsets.UTF_8
    val reader: BufferedReader = createReader()

    private fun createReader(): BufferedReader {
        return try {
            // 跨平台统一 UTF-8 输入流
            BufferedReader(InputStreamReader(System.`in`, ENCODING))
        } catch (e: IOException) {
            BufferedReader(ByteArrayInputStream(ByteArray(0)).reader(ENCODING))
        }
    }

    fun readLine(): CharStream? {
        return try {
            reader.readLine()?.let { input ->
                val charStream = CharStream(input)
                charStream
            }
        } catch (e: IOException) {
            handleError(e)
            null
        }
    }

    private fun handleError(e: IOException) {
        when (e) {
            is InterruptedIOException ->
                println("\u001B[33m输入超时，请重试")
            else ->
                println("\u001B[31m输入流错误: ${e.message ?: "未知异常"}")
        }
    }
}