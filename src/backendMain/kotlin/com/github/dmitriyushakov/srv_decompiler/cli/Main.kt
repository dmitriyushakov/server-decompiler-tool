package com.github.dmitriyushakov.srv_decompiler.cli

import com.github.dmitriyushakov.srv_decompiler.indexer.IndexType
import io.ktor.server.config.*
import io.ktor.server.config.ConfigLoader.Companion.load
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.util.logging.*
import kotlinx.cli.*
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

private lateinit var cliInternal: CommandLineArguments
val cli: CommandLineArguments get() = cliInternal

// Copy of io.ktor.server.engine.CommandLine.ConfigKeys
private object ConfigKeys {
    const val applicationIdPath = "ktor.application.id"
    const val hostConfigPath = "ktor.deployment.host"
    const val hostPortPath = "ktor.deployment.port"
    const val hostWatchPaths = "ktor.deployment.watch"

    const val rootPathPath = "ktor.deployment.rootPath"

    const val hostSslPortPath = "ktor.deployment.sslPort"
    const val hostSslKeyStore = "ktor.security.ssl.keyStore"
    const val hostSslKeyAlias = "ktor.security.ssl.keyAlias"
    const val hostSslKeyStorePassword = "ktor.security.ssl.keyStorePassword"
    const val hostSslPrivateKeyPassword = "ktor.security.ssl.privateKeyPassword"
    const val developmentModeKey = "ktor.development"
}

// Copy of io.ktor.server.engine.CommandLine.splitPair
private fun String.splitPair(ch: Char): Pair<String, String>? = indexOf(ch).let { idx ->
    when (idx) {
        -1 -> null
        else -> Pair(take(idx), drop(idx + 1))
    }
}

// Copy of io.ktor.server.engine.EnvironmentUtilsJvm.getConfigFromEnvironment
private fun getConfigFromEnvironment(): ApplicationConfig = System.getProperties()
    .toMap()
    .filterKeys { (it as String).startsWith("ktor.") }
    .let { env -> MapApplicationConfig(env.map { it.key as String to it.value as String }) }

// Modified copy of io.ktor.server.engine.CommandLine.buildApplicationConfig
private fun buildApplicationConfig(args: CommandLineArguments): ApplicationConfig {
    val configPaths = args.config
    val commandLineConfig = MapApplicationConfig(args.commandLineProperties)
    val environmentConfig = getConfigFromEnvironment()

    val fileConfig = when (configPaths.size) {
        0 -> ConfigLoader.load()
        1 -> ConfigLoader.load(configPaths.single())
        else -> configPaths.map { ConfigLoader.load(it) }.reduce { first, second -> first.mergeWith(second) }
    }

    return fileConfig.mergeWith(environmentConfig).mergeWith(commandLineConfig)
}

// Copy of io.ktor.server.engine.EnvironmentUtilsJvm.configureSSLConnectors
private fun ApplicationEngineEnvironmentBuilder.configureSSLConnectors(
    host: String,
    sslPort: String,
    sslKeyStorePath: String?,
    sslKeyStorePassword: String?,
    sslPrivateKeyPassword: String?,
    sslKeyAlias: String
) {
    if (sslKeyStorePath == null) {
        throw IllegalArgumentException(
            "SSL requires keystore: use -sslKeyStore=path or ${ConfigKeys.hostSslKeyStore} config"
        )
    }
    if (sslKeyStorePassword == null) {
        throw IllegalArgumentException(
            "SSL requires keystore password: use ${ConfigKeys.hostSslKeyStorePassword} config"
        )
    }
    if (sslPrivateKeyPassword == null) {
        throw IllegalArgumentException(
            "SSL requires certificate password: use ${ConfigKeys.hostSslPrivateKeyPassword} config"
        )
    }

    val keyStoreFile = File(sslKeyStorePath).let { file ->
        if (file.exists() || file.isAbsolute) file else File(".", sslKeyStorePath).absoluteFile
    }
    val keyStore = KeyStore.getInstance("JKS").apply {
        FileInputStream(keyStoreFile).use {
            load(it, sslKeyStorePassword.toCharArray())
        }

        requireNotNull(getKey(sslKeyAlias, sslPrivateKeyPassword.toCharArray())) {
            "The specified key $sslKeyAlias doesn't exist in the key store $sslKeyStorePath"
        }
    }

    sslConnector(
        keyStore,
        sslKeyAlias,
        { sslKeyStorePassword.toCharArray() },
        { sslPrivateKeyPassword.toCharArray() }
    ) {
        this.host = host
        this.port = sslPort.toInt()
        this.keyStorePath = keyStoreFile
    }
}

// Modified copy of io.ktor.server.engine.CommandLine.buildCommandLineEnvironment
internal fun buildCommandLineEnvironment(args: CommandLineArguments): ApplicationEngineEnvironment {
    val configuration = buildApplicationConfig(args)
    val applicationId = configuration.tryGetString(ConfigKeys.applicationIdPath) ?: "Server Decompiler Tool"
    val logger = KtorSimpleLogger(applicationId)

    val rootPath = configuration.tryGetString(ConfigKeys.rootPathPath) ?: ""

    val environment = applicationEngineEnvironment {
        log = logger
        config = configuration
        this.rootPath = rootPath

        val host = args.host ?: configuration.tryGetString(ConfigKeys.hostConfigPath) ?: "0.0.0.0"
        val port = args.port?.toString() ?: configuration.tryGetString(ConfigKeys.hostPortPath)
        val sslPort = args.sslPort?.toString() ?: configuration.tryGetString(ConfigKeys.hostSslPortPath)
        val sslKeyStorePath = args.sslKeyStore ?: configuration.tryGetString(ConfigKeys.hostSslKeyStore)
        val sslKeyStorePassword = configuration.tryGetString(ConfigKeys.hostSslKeyStorePassword)?.trim()
        val sslPrivateKeyPassword = configuration.tryGetString(ConfigKeys.hostSslPrivateKeyPassword)?.trim()
        val sslKeyAlias = configuration.tryGetString(ConfigKeys.hostSslKeyAlias) ?: "mykey"

        developmentMode = configuration.tryGetString(ConfigKeys.developmentModeKey)
            ?.let { it.toBoolean() } ?: PlatformUtils.IS_DEVELOPMENT_MODE

        if (port != null) {
            connector {
                this.host = host
                this.port = port.toInt()
            }
        }

        if (sslPort != null) {
            configureSSLConnectors(
                host,
                sslPort,
                sslKeyStorePath,
                sslKeyStorePassword,
                sslPrivateKeyPassword,
                sslKeyAlias
            )
        }

        if (port == null && sslPort == null) {
            throw IllegalArgumentException(
                "Neither port nor sslPort specified. Use command line options -port/-sslPort " +
                        "or configure connectors in application.conf"
            )
        }
    }

    return environment
}

// Copy of io.ktor.server.netty.EngineMain.loadConfiguration
private fun NettyApplicationEngine.Configuration.loadConfiguration(config: ApplicationConfig) {
    val deploymentConfig = config.config("ktor.deployment")
    loadCommonConfiguration(deploymentConfig)
    deploymentConfig.propertyOrNull("requestQueueLimit")?.getString()?.toInt()?.let {
        requestQueueLimit = it
    }
    deploymentConfig.propertyOrNull("runningLimit")?.getString()?.toInt()?.let {
        runningLimit = it
    }
    deploymentConfig.propertyOrNull("shareWorkGroup")?.getString()?.toBoolean()?.let {
        shareWorkGroup = it
    }
    deploymentConfig.propertyOrNull("responseWriteTimeoutSeconds")?.getString()?.toInt()?.let {
        responseWriteTimeoutSeconds = it
    }
    deploymentConfig.propertyOrNull("requestReadTimeoutSeconds")?.getString()?.toInt()?.let {
        requestReadTimeoutSeconds = it
    }
    deploymentConfig.propertyOrNull("tcpKeepAlive")?.getString()?.toBoolean()?.let {
        tcpKeepAlive = it
    }
    deploymentConfig.propertyOrNull("maxInitialLineLength")?.getString()?.toInt()?.let {
        maxInitialLineLength = it
    }
    deploymentConfig.propertyOrNull("maxHeaderSize")?.getString()?.toInt()?.let {
        maxHeaderSize = it
    }
    deploymentConfig.propertyOrNull("maxChunkSize")?.getString()?.toInt()?.let {
        maxChunkSize = it
    }
}

fun parseArgs(args: Array<String>): CommandLineArguments {
    val parser = ArgParser("server-decompiler-tool")

    val paths: List<String> by parser.argument(ArgType.String, description = "Classes paths to decompile").vararg().optional()
    val host: String? by parser.option(ArgType.String, fullName = "host", description = "A host address")
    val port: Int? by parser.option(ArgType.Int, shortName = "p", fullName = "port", description = "A listening port")
    val sslPort: Int? by parser.option(ArgType.Int, fullName = "sslPort", description = "A listening SSL port")
    val sslKeyStore: String? by parser.option(ArgType.String, fullName = "sslKeyStore", description = "An SSL key store")
    val config: List<String> by parser.option(ArgType.String, fullName = "config", description = "A path to a custom configuration file").multiple()
    val indexTypeStr: String? by parser.option(ArgType.String, fullName = "index", description = "Index implementation to use. Either - memory, file or temp-file.")
    val indexFilesPrefix: String? by parser.option(ArgType.String, fullName = "indexFilesPrefix", description = "Prefix for file based index files. Files with *.entities and *.tree extensions will be created.")
    val compressIndex: Boolean by parser.option(ArgType.Boolean, fullName = "compressIndex", description = "Enables GZIP compression for index *.entities file.").default(false)
    val commandLineProperties: MutableList<Pair<String, String>> = mutableListOf()

    val newArgs: MutableList<String> = mutableListOf()

    for (arg in args) {
        if (arg.startsWith("-P:") && arg.contains("=")) {
            val param = arg.splitPair('=')
            if (param != null) commandLineProperties.add(param.first.removePrefix("-P:") to param.second)
        } else {
            newArgs.add(arg)
        }
    }

    parser.parse(newArgs.toTypedArray())

    val indexType: IndexType = when(indexTypeStr) {
        "memory" -> IndexType.InMemory
        "file" -> IndexType.FileBased
        "temp-file" -> IndexType.FileBasedTemp
        null -> if (indexFilesPrefix == null) IndexType.InMemory else IndexType.FileBased
        else -> error("Unknown index type specified - $indexTypeStr")
    }

    return CommandLineArguments(
        paths = paths,
        host = host,
        port = port,
        sslPort = sslPort,
        sslKeyStore = sslKeyStore,
        config = config,
        commandLineProperties = commandLineProperties,
        indexType = indexType,
        indexFilesPrefix = indexFilesPrefix,
        compressIndex = compressIndex
    )
}

fun main(args: Array<String>) {
    val cli = parseArgs(args)
    cliInternal = cli

    val applicationEnvironment = buildCommandLineEnvironment(cli)
    val engine = NettyApplicationEngine(applicationEnvironment) { loadConfiguration(applicationEnvironment.config) }

    engine.start(true)
}