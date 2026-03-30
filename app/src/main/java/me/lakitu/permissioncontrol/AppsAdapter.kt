package me.lakitu.permissioncontrol

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView

class AppsAdapter(
    private val apps: List<AccessibilityAppManager.AppInfo>,
    private val onToggle: (AccessibilityAppManager.AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_app_icon)
        val name: TextView = view.findViewById(R.id.tv_app_name)
        val packageName: TextView = view.findViewById(R.id.tv_package_name)
        val switch: SwitchCompat = view.findViewById(R.id.switch_accessibility)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.name.text = app.appName
        holder.packageName.text = app.packageName
        holder.icon.setImageDrawable(app.icon)
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = app.getIsAccessibilityEnabled()

        lateinit var listener: CompoundButton.OnCheckedChangeListener
        listener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            onToggle(app, isChecked)
            holder.switch.setOnCheckedChangeListener(null)
            holder.switch.isChecked = app.getIsAccessibilityEnabled()
            holder.switch.setOnCheckedChangeListener(listener)
        }
        holder.switch.setOnCheckedChangeListener(listener)
    }

    override fun getItemCount(): Int = apps.size
}
