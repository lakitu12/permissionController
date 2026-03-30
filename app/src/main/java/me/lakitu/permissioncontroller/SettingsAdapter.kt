package me.lakitu.permissioncontroller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView

class SettingsAdapter(
    private val settings: List<SystemSettingsController.SettingItem>,
    private val onToggle: (SystemSettingsController.SettingItem, Boolean) -> SystemSettingsController.SettingResult
) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_setting_name)
        val key: TextView = view.findViewById(R.id.tv_setting_key)
        val switch: SwitchCompat = view.findViewById(R.id.switch_setting)
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
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = setting.getValue() as Boolean

        lateinit var listener: CompoundButton.OnCheckedChangeListener
        listener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            onToggle(setting, isChecked)
            holder.switch.setOnCheckedChangeListener(null)
            holder.switch.isChecked = setting.getValue() as Boolean
            holder.switch.setOnCheckedChangeListener(listener)
        }
        holder.switch.setOnCheckedChangeListener(listener)
    }

    override fun getItemCount(): Int = settings.size
}
