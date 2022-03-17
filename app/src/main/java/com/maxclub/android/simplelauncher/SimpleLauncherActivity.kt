package com.maxclub.android.simplelauncher

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File

private const val LOG_TAG = "SimpleLauncherActivity"

class SimpleLauncherActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_launcher)

        recyclerView = findViewById(R.id.app_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupAdapter()
    }

    private fun setupAdapter() {
        val startupIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val activities = packageManager.queryIntentActivities(startupIntent, 0)
        activities.sortBy { it.loadLabel(packageManager).toString().lowercase() }

        supportActionBar?.subtitle = resources.getQuantityString(
            R.plurals.subtitle_app_quantity,
            activities.size,
            activities.size
        )
        recyclerView.adapter = ActivityAdapter(activities)
    }

    private class ActivityHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val appNameTextView: TextView = itemView.findViewById(R.id.app_name_text_view)
        private val appSizeTextView: TextView = itemView.findViewById(R.id.app_size_text_view)
        private val appIconImageView: ImageView = itemView.findViewById(R.id.app_icon_image_view)

        private lateinit var resolveInfo: ResolveInfo

        init {
            itemView.setOnClickListener(this)
        }

        @DelicateCoroutinesApi
        fun bind(resolveInfo: ResolveInfo) {
            this.resolveInfo = resolveInfo
            val packageManager = itemView.context.packageManager

            appNameTextView.text = resolveInfo.loadLabel(packageManager).toString()

            val size = File(resolveInfo.activityInfo.applicationInfo.publicSourceDir).length()
            appSizeTextView.text = Formatter.formatFileSize(itemView.context, size)

            GlobalScope.launch {
                val appIcon = withContext(Dispatchers.Default) {
                    resolveInfo.loadIcon(packageManager)
                }
                launch(Dispatchers.Main) {
                    appIconImageView.setImageDrawable(appIcon)
                }
            }
        }

        override fun onClick(view: View) {
            val activityInfo = resolveInfo.activityInfo

            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(activityInfo.applicationInfo.packageName, activityInfo.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val context = view.context
            context.startActivity(intent)
        }
    }

    private class ActivityAdapter(val activities: List<ResolveInfo>) :
        RecyclerView.Adapter<ActivityHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.list_item_application, parent, false)
            return ActivityHolder(view)
        }

        override fun onBindViewHolder(holder: ActivityHolder, position: Int) {
            val resolveInfo = activities[position]
            holder.bind(resolveInfo)
        }

        override fun getItemCount(): Int = activities.size

    }
}