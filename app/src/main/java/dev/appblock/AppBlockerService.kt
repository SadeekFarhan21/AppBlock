package dev.appblock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AppBlockerService : AccessibilityService() {

    companion object {
        // Browser package -> resource id of its URL bar.
        private val BROWSER_URL_BAR_IDS = mapOf(
            "com.android.chrome" to "url_bar",
            "com.chrome.beta" to "url_bar",
            "com.chrome.dev" to "url_bar",
            "com.chrome.canary" to "url_bar",
            "com.brave.browser" to "url_bar",
            "com.vivaldi.browser" to "url_bar",
            "com.kiwibrowser.browser" to "url_bar",
            "com.microsoft.emmx" to "url_bar",
            "com.opera.browser" to "url_field",
            "com.opera.mini.native" to "url_field",
            "com.sec.android.app.sbrowser" to "location_bar_edit_text",
            "org.mozilla.firefox" to "mozac_browser_toolbar_url_view",
            "org.mozilla.firefox_beta" to "mozac_browser_toolbar_url_view",
            "org.mozilla.fenix" to "mozac_browser_toolbar_url_view",
            "com.duckduckgo.mobile.android" to "omnibarTextInput",
        )

        private const val BLOCK_COOLDOWN_MS = 1500L
    }

    private var lastBlockedKey: String? = null
    private var lastBlockedAt = 0L

    override fun onServiceConnected() {
        BlockRepository.init(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val reason = BlockRepository.blockReasonForApp(pkg)
            if (reason != null) {
                block(key = pkg, name = appLabel(pkg), reason = reason, isApp = true)
                return
            }
        }

        val urlBarId = BROWSER_URL_BAR_IDS[pkg] ?: return
        val url = readUrlBar(pkg, urlBarId) ?: return
        val host = hostOf(url) ?: return
        val reason = BlockRepository.blockReasonForSite(host) ?: return

        // Back out of the page first so dismissing the block screen
        // doesn't drop the user straight back onto the blocked site.
        performGlobalAction(GLOBAL_ACTION_BACK)
        block(key = host, name = host, reason = reason, isApp = false)
    }

    override fun onInterrupt() = Unit

    private fun block(key: String, name: String, reason: String, isApp: Boolean) {
        val now = System.currentTimeMillis()
        if (key == lastBlockedKey && now - lastBlockedAt < BLOCK_COOLDOWN_MS) return
        lastBlockedKey = key
        lastBlockedAt = now

        BlockRepository.recordBlocked()
        startActivity(
            Intent(this, BlockedActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                .putExtra(BlockedActivity.EXTRA_NAME, name)
                .putExtra(BlockedActivity.EXTRA_REASON, reason)
                .putExtra(BlockedActivity.EXTRA_IS_APP, isApp)
        )
    }

    private fun readUrlBar(pkg: String, urlBarId: String): String? {
        val root = rootInActiveWindow ?: return null
        val nodes: List<AccessibilityNodeInfo> =
            root.findAccessibilityNodeInfosByViewId("$pkg:id/$urlBarId") ?: return null
        val node = nodes.firstOrNull() ?: return null
        // While the user is typing, the URL bar is focused and shows history/
        // autocomplete suggestions. Only trust its text after navigation, when
        // focus has left the bar — otherwise we'd block on a suggestion the
        // user never committed to.
        if (node.isFocused) return null
        val text = node.text?.toString()?.trim()
        return if (text.isNullOrEmpty()) null else text
    }

    private fun hostOf(url: String): String? {
        // URL bars often show "example.com/path" without a scheme, or just a
        // search query. Only treat it as a host if it parses like one.
        val candidate = url.substringAfter("://").substringBefore('/').substringBefore(':')
        if (candidate.isEmpty() || candidate.contains(' ') || !candidate.contains('.')) return null
        return candidate
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }
}
