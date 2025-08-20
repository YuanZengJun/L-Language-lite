package ldk.l.litec.util

import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日志工具类：支持调试信息持久化到文件，同时控制终端打印
 */
object Logger {
    // 内置配置常量
    private const val DEFAULT_LOG_DIR = "litec-logs"
    private const val DEFAULT_LOG_PREFIX = "litec-debug-"
    private const val DEFAULT_LOG_SUFFIX = ".log"

    // 运行时状态
    private var logDir = DEFAULT_LOG_DIR
    private var logPrefix = DEFAULT_LOG_PREFIX
    private var printToConsole = false
    private var printLogEnabled = true

    @Volatile
    private lateinit var cachedLogFile: File
    private var isInitialized = false
    private var logWriter: PrintWriter? = null

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA)
    private val filenameFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA)

    /**
     * 初始化日志系统
     */
    @Synchronized
    fun init(
        logDir: String = DEFAULT_LOG_DIR,
        logPrefix: String = DEFAULT_LOG_PREFIX,
        printToConsole: Boolean = false,
        enableLogging: Boolean = true
    ) {
        this.logDir = logDir
        this.logPrefix = logPrefix
        this.printToConsole = printToConsole
        this.printLogEnabled = enableLogging

        ensureLogDirectory()
        cacheLogFile()

        logWriter = PrintWriter(FileWriter(cachedLogFile, true), true)
        isInitialized = true

        debug("Logger", "日志系统初始化完成（目录：$logDir）")
    }

    /**
     * 打印全部日志内容到控制台
     * @param maxLines 最大打印行数（默认全部打印）
     * @param filterLevel 筛选日志级别（null表示全部级别）
     * @param searchKeyword 搜索关键词（null表示不筛选）
     */
    @Synchronized
    fun printAllLogs(
        maxLines: Int = Int.MAX_VALUE,
        filterLevel: String? = null,
        searchKeyword: String? = null
    ) {
        checkInitialization()

        if (!cachedLogFile.exists()) {
            Stdout.printlnErr("⚠️ 日志文件不存在: ${cachedLogFile.absolutePath}")
            return
        }

        if (!cachedLogFile.isFile) {
            Stdout.printlnErr("⚠️ 路径不是文件: ${cachedLogFile.absolutePath}")
            return
        }

        try {
            val lines = cachedLogFile.readLines()
            val totalLines = lines.size
            var printedLines = 0

            Stdout.println("=".repeat(40))
            Stdout.println("📝 日志文件：${cachedLogFile.absolutePath}")
            Stdout.println("📊 总行数：$totalLines | 最大显示行数：${if (maxLines == Int.MAX_VALUE) "全部" else maxLines}")
            if (filterLevel != null) Stdout.println("🔍 筛选级别：$filterLevel")
            if (searchKeyword != null) Stdout.println("🔍 搜索关键词：$searchKeyword")
            Stdout.println("-".repeat(40))

            lines.forEachIndexed { index, line ->
                if (printedLines >= maxLines) return@forEachIndexed

                val shouldPrint = when {
                    filterLevel != null && searchKeyword != null ->
                        line.contains("[$filterLevel]") && line.contains(searchKeyword)
                    filterLevel != null ->
                        line.contains("[$filterLevel]")
                    searchKeyword != null ->
                        line.contains(searchKeyword)
                    else -> true
                }

                if (shouldPrint) {
                    Stdout.println("[${index + 1}] $line")
                    printedLines++
                }
            }

            if (printedLines >= maxLines && printedLines < totalLines) {
                Stdout.println("...（剩余 ${totalLines - printedLines} 行未显示）")
            }

            Stdout.println("=".repeat(40))
            Stdout.println("✅ 日志显示完成（共显示 $printedLines/$totalLines 行）")
        } catch (e: Exception) {
            Stdout.printlnErr("⚠️ 读取日志失败: ${e.message}")
        }
    }

    fun printAllLogsAsSingleOutput(maxLines: Int = Int.MAX_VALUE) {
        checkInitialization()

        return try {
            val content = cachedLogFile.readText() // 一次性读取全部内容
            val lines = content.lines()
            val filtered = lines.take(maxLines)

            Stdout.println("=".repeat(40))
            Stdout.println(filtered.joinToString("\n")) // 合并后一次性输出
            Stdout.println("=".repeat(40))
        } catch (e: Exception) {
            Stdout.printlnErr("读取失败: ${e.message}")
        }
    }

    /**
     * 快捷方法：打印全部ERROR级别日志
     */
    fun printErrorLogs(maxLines: Int = Int.MAX_VALUE) = printAllLogs(maxLines, "ERROR")

    /**
     * 快捷方法：搜索日志内容
     */
    fun searchLogs(keyword: String, maxLines: Int = Int.MAX_VALUE) = printAllLogs(maxLines, null, keyword)

    // 保留原有方法...
    @Synchronized
    fun log(module: String, content: String, level: String = "DEBUG") {
        if (!printLogEnabled) return

        checkInitialization()
        val entry = "[${timestampFormat.format(Date())}] [$level] [$module] $content"

        logWriter?.println(entry)
        if (printToConsole) {
            when (level) {
                "ERROR" -> Stdout.printlnErr(entry)
                else -> Stdout.println(entry)
            }
        }
    }

    fun debug(module: String, content: String) = log(module, content, "DEBUG")
    fun error(module: String, content: String) = log(module, content, "ERROR")
    fun warning(module: String, content: String) = log(module, content, "WARNING")

    @Synchronized
    fun close() {
        logWriter?.let { writer ->
            try {
                writer.flush()
                writer.close()
            } catch (e: IOException) {
                Stdout.printlnErr("[Logger错误] 关闭日志流失败: ${e.message}")
            } finally {
                logWriter = null
            }
        }
        isInitialized = false
    }

    // 私有辅助方法
    private fun checkInitialization() {
        if (!isInitialized) init()
    }

    private fun ensureLogDirectory() {
        val dir = File(logDir)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("无法创建日志目录: ${dir.absolutePath}")
        }
    }

    private fun cacheLogFile() {
        val filename = "$logPrefix${filenameFormat.format(Date())}$DEFAULT_LOG_SUFFIX"
        cachedLogFile = File(logDir, filename)
    }
}