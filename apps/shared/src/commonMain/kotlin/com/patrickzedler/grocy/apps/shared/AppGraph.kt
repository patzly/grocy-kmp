package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.settings.CertificateRepository
import com.patrickzedler.grocy.core.data.settings.CertificateTrustGate
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.log.LogReader
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.notifications.NotificationRepository
import com.patrickzedler.grocy.core.data.notifications.RepositoryNotificationDataSource
import com.patrickzedler.grocy.core.data.productfacts.ProductFactsRepository
import com.patrickzedler.grocy.core.data.scanner.ScannerRepository
import com.patrickzedler.grocy.core.data.server.GROCY_GITHUB_REPOSITORY
import com.patrickzedler.grocy.core.data.server.ServerRepository
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.settings.AppearanceRepository
import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
import com.patrickzedler.grocy.core.data.equipment.EquipmentRepository
import com.patrickzedler.grocy.core.data.settings.BatteriesJournalOptionsRepository
import com.patrickzedler.grocy.core.data.settings.BatteriesOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.settings.EquipmentOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ChoresJournalOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ChoresOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.calendar.CalendarRepository
import com.patrickzedler.grocy.core.data.settings.CalendarOptionsRepository
import com.patrickzedler.grocy.core.data.settings.TasksOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.tasks.TasksRepository
import com.patrickzedler.grocy.core.data.settings.BehaviorSettingsRepository
import com.patrickzedler.grocy.core.data.settings.NetworkSettingsRepository
import com.patrickzedler.grocy.core.data.settings.ScannerFormsRepository
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.recipes.MealPlanRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.settings.RecipesOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingModeSettingsRepository
import com.patrickzedler.grocy.core.data.settings.StartLayoutRepository
import com.patrickzedler.grocy.core.data.settings.StockOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.notifications.ShoppingListNotificationRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.widget.WidgetDataRefresher
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.data.update.UpdateRepository
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.network.NetworkProxy
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.navigation.Route
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.LocalNetworkGate
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import com.patrickzedler.grocy.core.network.TlsOptions
import com.patrickzedler.grocy.core.network.areCertificatesSupported
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.ProductFactsApi
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory
import com.patrickzedler.grocy.core.scanner.TextRecognizerFactory
import com.patrickzedler.grocy.feature.about.impl.AboutDependencies
import com.patrickzedler.grocy.feature.batteries.impl.BatteriesDependencies
import com.patrickzedler.grocy.feature.chores.impl.ChoresDependencies
import com.patrickzedler.grocy.feature.equipment.impl.EquipmentDependencies
import com.patrickzedler.grocy.feature.calendar.impl.CalendarDependencies
import com.patrickzedler.grocy.feature.tasks.impl.TasksDependencies
import com.patrickzedler.grocy.feature.login.impl.LoginDependencies
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataDependencies
import com.patrickzedler.grocy.feature.settings.impl.SettingsDependencies
import com.patrickzedler.grocy.feature.recipes.impl.RecipesDependencies
import com.patrickzedler.grocy.feature.shoppinglist.impl.ShoppingListDependencies
import com.patrickzedler.grocy.feature.start.impl.StartDependencies
import com.patrickzedler.grocy.feature.stock.impl.StockDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manual dependency injection: creates every shared object once and hands it to the features.
 *
 * Created once per process, on Android in the Application class, on the web in main().
 * Only the constructor is public, everything else is wired inside this module.
 */
class AppGraph(
    private val platform: PlatformDependencies,
) {
    // Lives as long as the process, for state the whole app shares
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Tor and the HTTP proxy, read right at app start; every request waits for them. */
    private val networkSettingsRepository =
        NetworkSettingsRepository(platform.settingsStore, appScope)

    /** Holds requests into the local network while the user is asked to allow them. */
    internal val localNetworkGate: LocalNetworkGate? =
        platform.localNetworkPermission?.let(::LocalNetworkGate)

    /** The client certificate and the trusted servers, only where the app may decide them. */
    private val certificateRepository = CertificateRepository(platform.settingsStore, appScope)

    /** How the user arranged the start screen, read at app start for its first frame. */
    private val startLayoutRepository = StartLayoutRepository(platform.settingsStore, appScope)

    /** The servers' certificates the user trusts, for the Play app, which hands them to a watch. */
    val trustedCertificates: Flow<List<TrustedCertificate>>
        get() = certificateRepository.settings.filterNotNull().map { it.trusted }

    /** Asks about a server's certificate during a login, see [CertificatePrompt]. */
    internal val certificateGate: CertificateTrustGate? =
        CertificateTrustGate(certificateRepository).takeIf { areCertificatesSupported }

    private val tlsOptions = TlsOptions(
        clientKeys = platform.clientKeys,
        settings = { certificateRepository.settings.value },
        awaitSettings = { certificateRepository.awaitSettings() },
    )

    private val httpClient by lazy {
        val client = createGrocyHttpClient(localNetworkGate, tlsOptions) {
            networkSettingsRepository.proxy()
        }
        appScope.launch {
            certificateRepository.settings.filterNotNull().drop(1).collect {
                tlsOptions.notifyChanged()
            }
        }
        client
    }

    private val grocyApi by lazy { GrocyApi(httpClient) }

    /** The server logged in to, for the Play app, which hands it to a paired watch. */
    val connection: Flow<ServerConnection?>
        get() = platform.credentialStore.connection

    internal val authRepository by lazy {
        AuthRepository(
            credentialStore = platform.credentialStore,
            grocyApi = grocyApi,
            certificateGate = certificateGate,
        ) { masterDataRepository.clear() }
    }

    private val masterDataRepository by lazy {
        MasterDataRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
        )
    }

    private val stockRepository by lazy {
        StockRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
        )
    }

    /** Shopping lists, whose items can be checked off offline. */
    internal val shoppingListRepository by lazy {
        ShoppingListRepository(
            api = grocyApi,
            database = platform.database,
            scope = appScope,
            connection = { platform.credentialStore.connection.first() },
            networkAvailable = platform.networkAvailable,
        )
    }

    /** Recipes and what the server computes for them. */
    private val recipesRepository by lazy {
        RecipesRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
        )
    }

    /** Grocy's meal plan, its sections and what the server computes for it. */
    private val mealPlanRepository by lazy {
        MealPlanRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
            recipes = recipesRepository,
        )
    }

    /** Grocy's batteries, what the server computes for them and their journal. */
    private val batteriesRepository by lazy {
        BatteriesRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
        )
    }

    /** Grocy's equipment; its files are downloaded when shown. */
    private val equipmentRepository by lazy {
        EquipmentRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
        )
    }

    /** Grocy's calendar, made of what the other repositories stored. */
    private val calendarRepository by lazy {
        CalendarRepository(
            api = grocyApi,
            connection = { platform.credentialStore.connection.first() },
            masterData = masterDataRepository,
            stock = stockRepository,
            chores = choresRepository,
            tasks = tasksRepository,
            batteries = batteriesRepository,
            recipes = recipesRepository,
            mealPlan = mealPlanRepository,
        )
    }

    /** Grocy's tasks, also the done ones. */
    private val tasksRepository by lazy {
        TasksRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
        )
    }

    /** Grocy's chores, what the server computes for them and their journal. */
    private val choresRepository by lazy {
        ChoresRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
        )
    }

    /** The logged in server and its version, read right at app start. */
    internal val serverRepository = ServerRepository(
        connection = platform.credentialStore.connection,
        masterDataRepository = masterDataRepository,
        grocyReleases = GitHubReleases(httpClient, GROCY_GITHUB_REPOSITORY),
        scope = appScope,
    )

    internal val unlockRepository by lazy {
        UnlockRepository(platform.unlockChecker, platform.settingsStore, appScope)
    }

    private val scannerRepository by lazy {
        ScannerRepository(
            settingsStore = platform.settingsStore,
            unlockRepository = unlockRepository,
            availableEngines = platform.barcodeDecoders.keys,
            scope = appScope,
            canReadText = platform.textRecognizer != null,
        )
    }

    private val gitHubReleases by lazy { GitHubReleases(httpClient, GITHUB_REPOSITORY) }

    internal val updateRepository by lazy {
        UpdateRepository(platform.createAppUpdater(gitHubReleases))
    }

    /** The language of the app, which the Android activity applies before Android 13. */
    internal val appLanguageStore: AppLanguageStore
        get() = platform.appLanguageStore

    /** How the app looks, from the general settings; read right at app start. */
    internal val appearanceRepository = AppearanceRepository(platform.settingsStore, appScope)

    private val stockOverviewOptionsRepository =
        StockOverviewOptionsRepository(platform.settingsStore, appScope)

    private val shoppingListOptionsRepository =
        ShoppingListOptionsRepository(platform.settingsStore, appScope)

    private val shoppingModeSettingsRepository =
        ShoppingModeSettingsRepository(platform.settingsStore, appScope)

    /** Product databases and date input; read right at app start, date fields show it. */
    internal val behaviorSettingsRepository =
        BehaviorSettingsRepository(platform.settingsStore, appScope)

    private val productFactsRepository by lazy {
        ProductFactsRepository(
            api = ProductFactsApi(
                httpClient = httpClient,
                userAgent = "GrocyKMP/${platform.versionName} " +
                    "(https://github.com/$GITHUB_REPOSITORY)",
            ),
            enabledSources = {
                behaviorSettingsRepository.settings.value?.productFactsSources.orEmpty()
            },
        )
    }

    private val scannerFormsRepository = ScannerFormsRepository(platform.settingsStore, appScope)

    private val recipesOptionsRepository =
        RecipesOptionsRepository(platform.settingsStore, appScope)

    private val batteriesOverviewOptionsRepository =
        BatteriesOverviewOptionsRepository(platform.settingsStore, appScope)

    private val batteriesJournalOptionsRepository =
        BatteriesJournalOptionsRepository(platform.settingsStore, appScope)

    private val calendarOptionsRepository =
        CalendarOptionsRepository(platform.settingsStore, appScope)

    private val equipmentOptionsRepository =
        EquipmentOptionsRepository(platform.settingsStore, appScope)

    private val tasksOverviewOptionsRepository =
        TasksOverviewOptionsRepository(platform.settingsStore, appScope)

    private val choresOverviewOptionsRepository =
        ChoresOverviewOptionsRepository(platform.settingsStore, appScope)

    private val choresJournalOptionsRepository =
        ChoresJournalOptionsRepository(platform.settingsStore, appScope)

    /** The daily notifications; the alarm and the app starting check what is due. */
    private val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(
            settingsStore = platform.settingsStore,
            connection = platform.credentialStore.connection,
            serverConfig = masterDataRepository.serverConfig,
            dataSource = RepositoryNotificationDataSource(
                masterData = masterDataRepository,
                stock = stockRepository,
                chores = choresRepository,
                tasks = tasksRepository,
                batteries = batteriesRepository,
                recipes = recipesRepository,
                mealPlan = mealPlanRepository,
                timeout = NOTIFICATION_REFRESH_TIMEOUT,
            ),
            notifier = platform.notifier,
            alarm = platform.createNotificationAlarm(appScope) {
                notificationRepository.check()
            },
            scope = appScope,
        )
    }

    /** What others do on the shopping lists; WorkManager or the open page asks it to look. */
    private val shoppingListNotifications: ShoppingListNotificationRepository by lazy {
        ShoppingListNotificationRepository(
            settingsStore = platform.settingsStore,
            connection = platform.credentialStore.connection,
            serverConfig = masterDataRepository.serverConfig,
            api = grocyApi,
            shopping = shoppingListRepository,
            masterData = masterDataRepository,
            notifier = platform.notifier,
            poller = platform.createShoppingListPoller(appScope) {
                shoppingListNotifications.check()
            },
            scope = appScope,
        )
    }

    /** Looks whether others changed a shopping list, see [ShoppingListNotificationRepository]. */
    internal suspend fun checkShoppingListChanges() = shoppingListNotifications.check()

    private val mutableOpenedNotification = MutableStateFlow<NotificationKind?>(null)

    /** The notification the user opened, until the navigation showed its feature. */
    internal val openedNotification = mutableOpenedNotification.asStateFlow()

    /** The user opened the notification of [kind]: shows its feature. */
    fun openNotification(kind: NotificationKind) {
        mutableOpenedNotification.value = kind
    }

    internal fun notificationOpened() {
        mutableOpenedNotification.value = null
    }

    private val mutableOpenedRoute = MutableStateFlow<Route?>(null)

    /** The screen a widget or link opened, until the navigation showed it. */
    internal val openedRoute = mutableOpenedRoute.asStateFlow()

    /** The user tapped something of a widget that leads to [link] ([WidgetLinks]). */
    internal fun openWidgetLink(link: String) {
        WidgetLinks.routeOfLink(link)?.let { mutableOpenedRoute.value = it }
    }

    /** An NFC tag, QR code or another app opened [link] ([DeepLinks]); others are ignored. */
    internal fun openDeepLink(link: String) {
        DeepLinks.routeOf(link)?.let { mutableOpenedRoute.value = it }
    }

    /** Android's settings of the app opened one of its pages, see [systemSettingsRouteOf]. */
    internal fun openRoute(route: Route) {
        mutableOpenedRoute.value = route
    }

    internal fun routeOpened() {
        mutableOpenedRoute.value = null
    }

    /** What the alarm of the system runs; [onFinished] once the app may go back to sleep. */
    internal fun checkNotifications(onFinished: () -> Unit) {
        appScope.launch {
            try {
                notificationRepository.check()
            } finally {
                onFinished()
            }
        }
    }

    /** Decoders of the engine that scans right now, provided to every scanner by GrocyApp. */
    internal val barcodeDecoderFactory: Flow<BarcodeDecoderFactory> by lazy {
        scannerRepository.activeEngine.filterNotNull().map {
            platform.barcodeDecoders[it] ?: BarcodeDecoderFactory.Zxing
        }
    }

    /** Whether due date fields offer their scanner, read at app start. */
    internal val scansDueDates: StateFlow<Boolean> by lazy { scannerRepository.scansDueDates }

    /** Reads due dates where [scansDueDates], provided to every date scanner by GrocyApp. */
    internal val textRecognizer: TextRecognizerFactory? = platform.textRecognizer

    internal val loginDependencies: LoginDependencies = object : LoginDependencies {
        override val authRepository: AuthRepository
            get() = this@AppGraph.authRepository
    }

    internal val settingsDependencies: SettingsDependencies = object : SettingsDependencies {
        override val startLayoutRepository: StartLayoutRepository
            get() = this@AppGraph.startLayoutRepository
        override val settingsStore: SettingsStore
            get() = platform.settingsStore
        override val appearanceRepository: AppearanceRepository
            get() = this@AppGraph.appearanceRepository
        override val appLanguageStore: AppLanguageStore
            get() = platform.appLanguageStore
        override val unlockRepository: UnlockRepository
            get() = this@AppGraph.unlockRepository
        override val scannerRepository: ScannerRepository
            get() = this@AppGraph.scannerRepository
        override val logReader: LogReader
            get() = platform.logReader
        override val serverRepository: ServerRepository
            get() = this@AppGraph.serverRepository
        override val certificateRepository: CertificateRepository
            get() = this@AppGraph.certificateRepository
        override val authRepository: AuthRepository
            get() = this@AppGraph.authRepository
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val behaviorSettings: BehaviorSettingsRepository
            get() = behaviorSettingsRepository

        override val shoppingModeSettings: ShoppingModeSettingsRepository
            get() = shoppingModeSettingsRepository
        override val notificationRepository: NotificationRepository
            get() = this@AppGraph.notificationRepository
        override val shoppingListNotifications: ShoppingListNotificationRepository
            get() = this@AppGraph.shoppingListNotifications

        override fun restartApp() = platform.restartApp()

        override val networkSettings: NetworkSettingsRepository
            get() = this@AppGraph.networkSettingsRepository
    }

    internal val startDependencies: StartDependencies = object : StartDependencies {
        override val startLayoutRepository: StartLayoutRepository
            get() = this@AppGraph.startLayoutRepository
        override val authRepository: AuthRepository
            get() = this@AppGraph.authRepository
        override val calendarRepository: CalendarRepository
            get() = this@AppGraph.calendarRepository
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val recipesRepository: RecipesRepository
            get() = this@AppGraph.recipesRepository
        override val mealPlanRepository: MealPlanRepository
            get() = this@AppGraph.mealPlanRepository
        override val choresRepository: ChoresRepository
            get() = this@AppGraph.choresRepository
        override val tasksRepository: TasksRepository
            get() = this@AppGraph.tasksRepository
        override val batteriesRepository: BatteriesRepository
            get() = this@AppGraph.batteriesRepository
        override val equipmentRepository: EquipmentRepository
            get() = this@AppGraph.equipmentRepository
        override val stockRepository: StockRepository
            get() = this@AppGraph.stockRepository
        override val shoppingListRepository: ShoppingListRepository
            get() = this@AppGraph.shoppingListRepository
        override val serverConfig: StateFlow<ServerConfig?>
            get() = serverRepository.serverConfig
        override val server: StateFlow<ServerState?>
            get() = serverRepository.server
    }

    internal val masterDataDependencies: MasterDataDependencies = object : MasterDataDependencies {
        override val productFactsRepository: ProductFactsRepository
            get() = this@AppGraph.productFactsRepository

        override val scannerForms: ScannerFormsRepository
            get() = scannerFormsRepository
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val stockRepository: StockRepository
            get() = this@AppGraph.stockRepository
        override val serverConfig: StateFlow<ServerConfig?>
            get() = serverRepository.serverConfig
        override val shoppingListRepository: ShoppingListRepository
            get() = this@AppGraph.shoppingListRepository
    }

    internal val shoppingListDependencies: ShoppingListDependencies =
        object : ShoppingListDependencies {
            override val scannerForms: ScannerFormsRepository
                get() = scannerFormsRepository
            override val masterDataRepository: MasterDataRepository
                get() = this@AppGraph.masterDataRepository
            override val stockRepository: StockRepository
                get() = this@AppGraph.stockRepository
            override val shoppingListRepository: ShoppingListRepository
                get() = this@AppGraph.shoppingListRepository
            override val shoppingListOptions: ShoppingListOptionsRepository
                get() = shoppingListOptionsRepository
            override val shoppingModeSettings: ShoppingModeSettingsRepository
                get() = shoppingModeSettingsRepository
        }

    internal val stockDependencies: StockDependencies = object : StockDependencies {
        override val behaviorSettings: BehaviorSettingsRepository
            get() = behaviorSettingsRepository

        override val scannerForms: ScannerFormsRepository
            get() = scannerFormsRepository
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val stockRepository: StockRepository
            get() = this@AppGraph.stockRepository
        override val stockOverviewOptions: StockOverviewOptionsRepository
            get() = stockOverviewOptionsRepository
        override val shoppingListRepository: ShoppingListRepository
            get() = this@AppGraph.shoppingListRepository
    }

    internal val recipesDependencies: RecipesDependencies = object : RecipesDependencies {
        override val scannerForms: ScannerFormsRepository
            get() = scannerFormsRepository
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val recipesRepository: RecipesRepository
            get() = this@AppGraph.recipesRepository
        override val stockRepository: StockRepository
            get() = this@AppGraph.stockRepository
        override val shoppingListRepository: ShoppingListRepository
            get() = this@AppGraph.shoppingListRepository
        override val recipesOptions: RecipesOptionsRepository
            get() = recipesOptionsRepository
        override val mealPlanRepository: MealPlanRepository
            get() = this@AppGraph.mealPlanRepository
    }

    internal val batteriesDependencies: BatteriesDependencies = object : BatteriesDependencies {
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val batteriesRepository: BatteriesRepository
            get() = this@AppGraph.batteriesRepository
        override val batteriesOverviewOptions: BatteriesOverviewOptionsRepository
            get() = batteriesOverviewOptionsRepository
        override val batteriesJournalOptions: BatteriesJournalOptionsRepository
            get() = batteriesJournalOptionsRepository
        override val scannerForms: ScannerFormsRepository
            get() = scannerFormsRepository
    }

    internal val equipmentDependencies: EquipmentDependencies = object : EquipmentDependencies {
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val equipmentRepository: EquipmentRepository
            get() = this@AppGraph.equipmentRepository
        override val equipmentOptions: EquipmentOptionsRepository
            get() = equipmentOptionsRepository
    }

    internal val calendarDependencies: CalendarDependencies = object : CalendarDependencies {
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val calendarRepository: CalendarRepository
            get() = this@AppGraph.calendarRepository
        override val calendarOptions: CalendarOptionsRepository
            get() = calendarOptionsRepository
        override val stockRepository: StockRepository
            get() = this@AppGraph.stockRepository
        override val shoppingListRepository: ShoppingListRepository
            get() = this@AppGraph.shoppingListRepository
    }

    internal val tasksDependencies: TasksDependencies = object : TasksDependencies {
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val tasksRepository: TasksRepository
            get() = this@AppGraph.tasksRepository
        override val tasksOverviewOptions: TasksOverviewOptionsRepository
            get() = tasksOverviewOptionsRepository
    }

    internal val choresDependencies: ChoresDependencies = object : ChoresDependencies {
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val choresRepository: ChoresRepository
            get() = this@AppGraph.choresRepository
        override val choresOverviewOptions: ChoresOverviewOptionsRepository
            get() = choresOverviewOptionsRepository
        override val choresJournalOptions: ChoresJournalOptionsRepository
            get() = choresJournalOptionsRepository
        override val scannerForms: ScannerFormsRepository
            get() = scannerFormsRepository
    }

    /** What the home screen widgets show and change. */
    internal val widgetDependencies: WidgetDependencies = object : WidgetDependencies {
        override val shoppingListRepository: ShoppingListRepository
            get() = this@AppGraph.shoppingListRepository
        override val widgetDataRefresher: WidgetDataRefresher by lazy {
            WidgetDataRefresher(
                api = grocyApi,
                connection = { platform.credentialStore.connection.first() },
                masterData = masterDataRepository,
                calendar = calendarRepository,
                shoppingLists = this@AppGraph.shoppingListRepository,
            )
        }
        override val connection: Flow<ServerConnection?>
            get() = platform.credentialStore.connection
        override val appearance: Flow<AppearanceSettings>
            get() = platform.settingsStore.appearance
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val tasksRepository: TasksRepository
            get() = this@AppGraph.tasksRepository
        override val calendarRepository: CalendarRepository
            get() = this@AppGraph.calendarRepository
    }

    init {
        // Items checked off and notes edited offline when the app was last used; creating the
        // repository also makes it send them whenever the device gets a network
        shoppingListRepository.sendPendingInBackground()
        // Catches up what was missed while the app could not run and keeps the alarm set
        notificationRepository.start()
        shoppingListNotifications.start()
        platform.startWidgetUpdates(widgetDependencies, appScope)
        // Orbot starts Tor by itself if the user allows it there, else requests fail until then
        appScope.launch {
            if (networkSettingsRepository.proxy() == NetworkProxy.Tor) platform.requestTorStart()
        }
    }

    internal val aboutDependencies: AboutDependencies = object : AboutDependencies {
        override val unlockRepository: UnlockRepository
            get() = this@AppGraph.unlockRepository
        override val updateRepository: UpdateRepository
            get() = this@AppGraph.updateRepository
        override val versionName: String
            get() = platform.versionName
    }
}

/** Android keeps the app running for about this long after an alarm without trouble. */
private val NOTIFICATION_REFRESH_TIMEOUT = 20.seconds

/** Where the app's releases are published, also for update checks outside Google Play. */
private const val GITHUB_REPOSITORY = "patzly/grocy-kmp"
