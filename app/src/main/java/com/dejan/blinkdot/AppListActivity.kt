package com.dejan.blinkdot

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListActivity : AppCompatActivity() {

    private class Row(val pkg: String, val label: String, val icon: Drawable)

    private lateinit var prefs: Prefs
    private lateinit var adapter: Adapter
    private lateinit var progress: ProgressBar

    private var all: List<Row> = emptyList()
    private val shown = ArrayList<Row>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applist)
        prefs = Prefs(this)

        progress = findViewById(R.id.progress)
        adapter = Adapter()

        val list = findViewById<RecyclerView>(R.id.rv)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                filter(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        load()
    }

    private fun load() {
        progress.visibility = View.VISIBLE
        Thread {
            val pm = packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val found = LinkedHashMap<String, Row>()

            for (info in pm.queryIntentActivities(launcher, 0)) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (pkg == packageName || found.containsKey(pkg)) continue
                found[pkg] = Row(pkg, info.loadLabel(pm).toString(), info.loadIcon(pm))
            }

            // Keep already-selected apps visible even without a launcher icon.
            for (pkg in prefs.enabledApps()) {
                if (found.containsKey(pkg)) continue
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    found[pkg] = Row(
                        pkg,
                        pm.getApplicationLabel(appInfo).toString(),
                        pm.getApplicationIcon(appInfo)
                    )
                } catch (ignored: Throwable) {
                }
            }

            val sorted = found.values.sortedWith(
                compareByDescending<Row> { prefs.isAppEnabled(it.pkg) }
                    .thenBy { it.label.lowercase() }
            )

            runOnUiThread {
                all = sorted
                progress.visibility = View.GONE
                filter("")
            }
        }.start()
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        shown.clear()
        for (row in all) {
            if (q.isEmpty() || row.label.lowercase().contains(q) || row.pkg.lowercase().contains(q)) {
                shown.add(row)
            }
        }
        adapter.notifyDataSetChanged()
    }

    private inner class Adapter : RecyclerView.Adapter<Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int = shown.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(shown[position])
        }
    }

    private inner class Holder(view: View) : RecyclerView.ViewHolder(view) {

        private val icon: ImageView = view.findViewById(R.id.icon)
        private val label: TextView = view.findViewById(R.id.label)
        private val pkgName: TextView = view.findViewById(R.id.pkg)
        private val swatch: View = view.findViewById(R.id.swatch)
        private val toggle: SwitchCompat = view.findViewById(R.id.toggle)

        fun bind(row: Row) {
            icon.setImageDrawable(row.icon)
            label.text = row.label
            pkgName.text = row.pkg
            swatch.background = Ui.circle(this@AppListActivity, prefs.colorFor(row.pkg))

            toggle.setOnCheckedChangeListener(null)
            toggle.isChecked = prefs.isAppEnabled(row.pkg)
            toggle.setOnCheckedChangeListener { _, checked ->
                prefs.setAppEnabled(row.pkg, checked)
                swatch.background = Ui.circle(this@AppListActivity, prefs.colorFor(row.pkg))
            }

            swatch.setOnClickListener {
                ColorPicker.show(this@AppListActivity, prefs.colorFor(row.pkg)) { picked ->
                    prefs.setColor(row.pkg, picked)
                    swatch.background = Ui.circle(this@AppListActivity, picked)
                    if (!prefs.isAppEnabled(row.pkg)) {
                        prefs.setAppEnabled(row.pkg, true)
                        toggle.isChecked = true
                    }
                }
            }

            itemView.setOnClickListener { toggle.toggle() }
        }
    }
}
