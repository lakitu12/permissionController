package me.lakitu.permissioncontrol

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SettingsAdapter(
    private val settings: List<SystemSettingsController.SettingItem>,
    private val onToggle: (SystemSettingsController.SettingItem, Boolean) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_setting_name)
        val key: TextView = view.findViewById(R.id.tv_setting_key)
        val switch: Switch = view.findViewById(R.id.switch_setting)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_setting, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val setting = settings[position]
        holder.name.text = setting.name
        holder.key.text = setting.key
        holder.switch.isChecked = setting.getValue() as Boolean

        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            onToggle(setting, isChecked)
        }
    }

    override fun getItemCount(): Int = settings.size
}
