package ldk.l.litec.util

import java.io.*
import java.nio.charset.StandardCharsets

object Stdout {
    private val ENCODING = StandardCharsets.UTF_8

    private val realOut = createSecureStream(System.out)
    private val realErr = createSecureStream(System.err)

    init {
        if (isPowerShell()) configurePowerShellEnvironment()
        // printBanner()
    }

    private fun createSecureStream(out: OutputStream): PrintStream {
        return object : PrintStream(out, true, ENCODING.name()) {
            override fun print(x: String?) {
                val content = x ?: "null"
                // 直接写入原始 UTF-8 字符串
                super.write(content.toByteArray(ENCODING))
                super.flush()
            }
        }
    }

    private fun configurePowerShellEnvironment() {
        System.setProperty("powershell.encoding", "utf8")
        System.setProperty("user.language", "zh")
        System.setProperty("user.country", "CN")
    }

//    private fun printBanner() {
//        val banner = """
//            |========================================
//            | [SYSTEM] Output Processor Initialized
//            |  Encoding: ${ENCODING.displayName()}
//            |  Test: 中文测试 😊
//            |========================================
//        """.trimMargin()
//
//        println(banner)
//    }

    private fun isPowerShell(): Boolean {
        return System.getenv()["powershell"] != null ||
                System.console()?.reader()?.javaClass?.name?.contains("ConsoleReader") == true
    }

    fun println(s: Any?) = realOut.println(s)
    fun print(s: Any?) = realOut.print(s)
    fun printlnErr(s: Any?) = realErr.println(s)
    fun flush() {
        realOut.flush()
        realErr.flush()
    }
}