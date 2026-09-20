package com.antigravity.droidconnect.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.antigravity.droidconnect.AntiGravityApp
import com.antigravity.droidconnect.R
import com.antigravity.droidconnect.data.FilterMode
import com.antigravity.droidconnect.databinding.ActivityAppFilterBinding
import com.antigravity.droidconnect.model.AppInfoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppFilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppFilterBinding
    private val prefs by lazy { AntiGravityApp.instance.preferences }
    private lateinit var adapter: AppFilterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFilterMode()
        setupSearch()
        loadInstalledApps()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = AppFilterAdapter { packageName, isEnabled ->
            prefs.setAppEnabled(packageName, isEnabled)
        }
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
    }

    private fun setupFilterMode() {
        updateFilterModeText()

        binding.btnToggleMode.setOnClickListener {
            prefs.filterMode = if (prefs.filterMode == FilterMode.BLACKLIST) {
                FilterMode.WHITELIST
            } else {
                FilterMode.BLACKLIST
            }
            updateFilterModeText()
            loadInstalledApps()
        }
    }

    private fun updateFilterModeText() {
        if (prefs.filterMode == FilterMode.BLACKLIST) {
            binding.tvFilterMode.setText(R.string.filter_mode_blacklist)
        } else {
            binding.tvFilterMode.setText(R.string.filter_mode_whitelist)
        }
    }

    private fun setupSearch() {
        binding.etSearchApps.doAfterTextChanged { text ->
            adapter.filter(text?.toString().orEmpty())
        }
    }

    private fun loadInstalledApps() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val appList = withContext(Dispatchers.IO) {
                val pm = packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(intent, 0)
                }

                val seenPackages = HashSet<String>()
                val items = mutableListOf<AppInfoItem>()

                for (info in resolveInfos) {
                    val pkgName = info.activityInfo.packageName
                    if (seenPackages.contains(pkgName) || pkgName == packageName) {
                        continue
                    }
                    seenPackages.add(pkgName)

                    val appName = try {
                        info.loadLabel(pm).toString()
                    } catch (e: Exception) {
                        pkgName
                    }

                    val icon = try {
                        info.loadIcon(pm)
                    } catch (e: Exception) {
                        null
                    }

                    val isEnabled = prefs.isAppSelected(pkgName)

                    items.add(
                        AppInfoItem(
                            packageName = pkgName,
                            appName = appName,
                            icon = icon,
                            isEnabled = isEnabled
                        )
                    )
                }

                items.sortBy { it.appName.lowercase() }
                items
            }

            binding.progressBar.visibility = View.GONE
            adapter.submitList(appList)
        }
    }
}
