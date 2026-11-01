package com.footballai.analyzer

/**
 * MainActivity onCreate végére tedd (inject FcmRegistrar):
 *
 * @AndroidEntryPoint
 * class MainActivity : ComponentActivity() {
 *   @Inject lateinit var fcmRegistrar: FcmRegistrar
 *   override fun onCreate(...) {
 *     ...
 *     fcmRegistrar.registerInBackground(lifecycleScope)
 *   }
 * }
 */
