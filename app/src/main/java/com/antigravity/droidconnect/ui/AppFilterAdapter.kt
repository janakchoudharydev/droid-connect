package com.antigravity.droidconnect.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.antigravity.droidconnect.databinding.ItemAppFilterBinding
import com.antigravity.droidconnect.model.AppInfoItem

class AppFilterAdapter(
    private val onAppToggled: (packageName: String, isEnabled: Boolean) -> Unit
) : RecyclerView.Adapter<AppFilterAdapter.AppViewHolder>() {

    private var allApps: List<AppInfoItem> = emptyList()
    private var displayedApps: List<AppInfoItem> = emptyList()

    fun submitList(apps: List<AppInfoItem>) {
        allApps = apps
        displayedApps = apps
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        displayedApps = if (query.isBlank()) {
            allApps
        } else {
            val lower = query.lowercase().trim()
            allApps.filter {
                it.appName.lowercase().contains(lower) || it.packageName.lowercase().contains(lower)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(displayedApps[position])
    }

    override fun getItemCount(): Int = displayedApps.size

    inner class AppViewHolder(
        private val binding: ItemAppFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppInfoItem) {
            binding.tvAppName.text = item.appName
            binding.tvPackageName.text = item.packageName

            if (item.icon != null) {
                binding.ivAppIcon.setImageDrawable(item.icon)
            } else {
                binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            // Remove listener before updating state to prevent unwanted callbacks
            binding.switchAppEnabled.setOnCheckedChangeListener(null)
            binding.switchAppEnabled.isChecked = item.isEnabled

            binding.switchAppEnabled.setOnCheckedChangeListener { _, isChecked ->
                item.isEnabled = isChecked
                onAppToggled(item.packageName, isChecked)
            }

            binding.root.setOnClickListener {
                binding.switchAppEnabled.toggle()
            }
        }
    }
}
