/*
 * Copyright (c) 2015 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.irccloud.android.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.net.Uri
import android.net.http.HttpResponseCache
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.damnhandy.uri.template.UriTemplate
import com.google.android.material.snackbar.Snackbar
import com.irccloud.android.AsyncTaskEx
import com.irccloud.android.ColorScheme
import com.irccloud.android.IRCCloudLog
import com.irccloud.android.NetworkConnection
import com.irccloud.android.R
import com.irccloud.android.data.collection.ImageList
import com.irccloud.android.data.collection.ImageList.OnImageFetchedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

class UploadsActivity : BaseActivity() {
    private var page = 0
    private val adapter = FilesAdapter()
    private var canLoadMore = true
    private var footer: View? = null

    private var to: String? = null
    private var cid = -1
    private var msg: String? = null

    private var template: UriTemplate? = null

    private data class File(
        val id: String,
        val name: String?,
        val mime_type: String,
        var extension: String?,
        val size: Int,
        val date: Date,
    ) : Serializable {

        @Transient
        var image: Bitmap? = null
        var image_failed: Boolean = false
        var deleting: Boolean = false
        var position: Int = 0
        var date_formatted: String? = null
        var metadata: String? = null
        var url: String? = null

        companion object {
            private const val serialVersionUID = 0L
        }
    }

    private inner class FilesAdapter : BaseAdapter() {
        private inner class ViewHolder {
            var date: TextView? = null
            var image: ImageView? = null
            var extension: TextView? = null
            var name: TextView? = null
            var metadata: TextView? = null
            var progress: ProgressBar? = null
            var delete: ImageButton? = null
            var delete_progress: ProgressBar? = null
        }

        private val files = ArrayList<File>()
        private val dateFormat: DateFormat = DateFormat.getDateTimeInstance()

        fun clear() {
            for (f in files) {
                f.image = null
            }
            files.clear()
            notifyDataSetInvalidated()
        }

        fun saveInstanceState(state: Bundle) {
            state.putSerializable("adapter", files.toTypedArray<File>())
        }

        fun addFile(
            id: String,
            name: String,
            mime_type: String,
            extension: String?,
            size: Int,
            date: Date
        ) {
            val f = File(id, name, mime_type, extension, size, date)

            if (f.extension?.length ?: 0 > 1 && f.extension?.startsWith(".") ?: false )
                f.extension = f.extension?.substring(1)?.uppercase(
                    Locale.getDefault()
                )

            addFile(f)
        }

        fun addFile(f: File) {
            f.position = files.size
            files.add(f)

            if (f.image == null && f.mime_type.startsWith("image/")) {
                try {
                    if (NetworkConnection.file_uri_template != null) {
                        f.url = template!!.set("id", f.id).set("name", f.name).expand()
                        f.image = ImageList.getInstance().getImage(f.id, 320)
                        if (f.image == null) ImageList.getInstance()
                            .fetchImage(f.id, 320, object : OnImageFetchedListener() {
                                override fun onImageFetched(image: Bitmap?) {
                                    f.image = image
                                    if (f.image == null) f.image_failed = true
                                    runOnUiThread { notifyDataSetChanged() }
                                }
                            })
                    }
                } catch (e: OutOfMemoryError) {
                    f.image_failed = true
                } catch (e: Exception) {
                    f.image_failed = true
                    NetworkConnection.printStackTraceToCrashlytics(e)
                }
                if (f.image_failed && (f.extension == null || f.extension!!.length == 0)) f.extension =
                    "IMAGE"
                runOnUiThread { notifyDataSetChanged() }
            } else {
                if (NetworkConnection.file_uri_template != null) {
                    f.url = template!!.set("id", f.id).set("name", f.name).expand()
                }
            }
        }

        fun update_positions() {
            var p = 0
            for (file in files) {
                file.position = p++
            }
        }

        fun restoreFile(f: File) {
            f.deleting = false
            files.add(f.position, f)
            update_positions()
            notifyDataSetChanged()
        }

        fun removeFile(f: File) {
            files.remove(f)
            update_positions()
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return files.size
        }

        override fun getItem(i: Int): Any {
            return files[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View? {
            var row = view
            val holder: ViewHolder

            if (row == null) {
                val inflater = layoutInflater
                row = inflater.inflate(R.layout.row_file, viewGroup, false)

                holder = ViewHolder()
                holder.date = row.findViewById(R.id.date)
                holder.image = row.findViewById(R.id.image)
                holder.extension = row.findViewById(R.id.extension)
                holder.name = row.findViewById(R.id.name)
                holder.metadata = row.findViewById(R.id.metadata)
                holder.progress = row.findViewById(R.id.progress)
                holder.delete = row.findViewById(R.id.delete)
                holder.delete_progress = row.findViewById(R.id.deleteProgress)

                row.tag = holder
            } else {
                holder = row.tag as ViewHolder
            }

            try {
                val f = files[i]
                if (f.date_formatted == null) f.date_formatted = dateFormat.format(f.date)
                holder.date!!.text = f.date_formatted
                if (f.extension != null && f.extension!!.length > 1) holder.extension!!.text =
                    f.extension
                else holder.extension!!.text = "???"
                holder.name!!.text = f.name
                if (f.metadata == null) {
                    if (f.size < 1024) {
                        f.metadata = f.size.toString() + " B"
                    } else {
                        val exp = (ln(f.size.toDouble()) / ln(1024.0)).toInt()
                        f.metadata = String.format(
                            "%.1f ",
                            f.size / (1024.0).pow(exp.toDouble())
                        ) + ("KMGTPE"[exp - 1]) + "B"
                    }
                    f.metadata += " • " + f.mime_type
                }
                holder.metadata!!.text = f.metadata
                if (!f.image_failed && f.mime_type.startsWith("image/")) {
                    if (f.image != null) {
                        holder.progress!!.visibility = View.GONE
                        holder.extension!!.visibility = View.GONE
                        holder.image!!.visibility = View.VISIBLE
                        holder.image!!.setImageBitmap(f.image)
                    } else {
                        holder.extension!!.visibility = View.GONE
                        holder.image!!.visibility = View.GONE
                        holder.image!!.setImageBitmap(null)
                        holder.progress!!.visibility = View.VISIBLE
                    }
                } else {
                    holder.extension!!.visibility = View.VISIBLE
                    holder.image!!.visibility = View.GONE
                    holder.progress!!.visibility = View.GONE
                }
                holder.delete!!.setOnClickListener(deleteClickListener)
                holder.delete!!.setColorFilter(
                    ColorScheme.getInstance().colorControlNormal,
                    PorterDuff.Mode.SRC_ATOP
                )
                holder.delete!!.tag = i
                if (f.deleting) {
                    holder.delete!!.visibility = View.GONE
                    holder.delete_progress!!.visibility = View.VISIBLE
                } else {
                    holder.delete!!.visibility = View.VISIBLE
                    holder.delete_progress!!.visibility = View.GONE
                }
            } catch (e: Exception) {
                // TODO Auto-generated catch block
                NetworkConnection.printStackTraceToCrashlytics(e)
            }

            return row
        }
    }

    private suspend fun fetchFiles() {
        withContext(Dispatchers.Main) {
            canLoadMore = false
        }
        withContext(Dispatchers.IO) {
            try {
                val jsonObject = NetworkConnection.getInstance().files(++page)

                withContext(Dispatchers.Main) {
                    if (jsonObject != null) {
                        try {
                            if (jsonObject.getBoolean("success")) {
                                val files = jsonObject.getJSONArray("files")
                                Log.e(
                                    "IRCCloud",
                                    "Got " + files.length() + " files for page " + page
                                )
                                for (i in 0 until files.length()) {
                                    val file = files.getJSONObject(i)
                                    adapter.addFile(
                                        file.getString("id"),
                                        file.getString("name"),
                                        file.getString("mime_type"),
                                        file.getString("extension"),
                                        file.getInt("size"),
                                        Date(file.getLong("date") * 1000L)
                                    )
                                }
                                adapter.notifyDataSetChanged()
                                canLoadMore =
                                    files.length() > 0 && adapter.count < jsonObject.getInt("total")
                                if (!canLoadMore) {
                                    footer!!.findViewById<View>(R.id.progress).visibility =
                                        View.GONE
                                }
                            } else {
                                page--
                                Log.e("IRCCloud", "Failed: $jsonObject")
                                if (jsonObject.has("message") && jsonObject.getString("message") == "server_error") {
                                    canLoadMore = true
                                    launch {
                                        fetchFiles()
                                    }
                                } else {
                                    canLoadMore = false
                                }
                            }
                        } catch (e: JSONException) {
                            page--
                            NetworkConnection.printStackTraceToCrashlytics(e)
                        }
                    } else {
                        page--
                        canLoadMore = true
                        launch {
                            fetchFiles()
                        }
                    }
                    checkEmpty()
                }
            } catch (e: IOException) {
                NetworkConnection.printStackTraceToCrashlytics(e)
            }
        }
    }

    private val deleteClickListener =
        View.OnClickListener { view ->
            val f = adapter.getItem(view.tag as Int) as File
            val builder =
                AlertDialog.Builder(this@UploadsActivity)
            builder.setTitle("Delete File")
            if (f.name != null && f.name.length > 0) {
                builder.setMessage("Are you sure you want to delete '" + f.name + "'?")
            } else {
                builder.setMessage("Are you sure you want to delete this file?")
            }
            builder.setPositiveButton(
                "Delete"
            ) { _, _ ->
                NetworkConnection.getInstance().deleteFile(
                    f.id
                ) { result ->
                    if (result.getBoolean("success")) {
                        Log.d("IRCCloud", "File deleted successfully")
                        runOnUiThread {
                            adapter.removeFile(f)
                            val undo =
                                View.OnClickListener {
                                    NetworkConnection.getInstance()
                                        .restoreFile(f.id, null)
                                    adapter.restoreFile(f)
                                }
                            if (f.name != null && f.name.length > 0) Snackbar.make(
                                findViewById(android.R.id.list),
                                f.name + " was deleted.",
                                Snackbar.LENGTH_LONG
                            ).setAction("UNDO", undo).show()
                            else Snackbar.make(
                                findViewById(android.R.id.list),
                                "File was deleted.",
                                Snackbar.LENGTH_LONG
                            ).setAction("UNDO", undo).show()
                        }
                    } else {
                        IRCCloudLog.Log(
                            Log.ERROR, "IRCCloud",
                            "Delete failed: $result"
                        )
                        runOnUiThread {
                            val builder =
                                AlertDialog.Builder(
                                    this@UploadsActivity
                                )
                            builder.setTitle("Error")
                            if (f.name != null && f.name.length > 0) builder.setMessage(
                                "Unable to delete '" + f.name + "'.  Please try again shortly."
                            )
                            else builder.setMessage("Unable to delete file.  Please try again shortly.")
                            builder.setPositiveButton("Close", null)
                            builder.show()
                            f.deleting = false
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
                f.deleting = true
                adapter.notifyDataSetChanged()
                checkEmpty()
            }
            builder.setNegativeButton(
                "Cancel"
            ) { dialogInterface, _ -> dialogInterface.dismiss() }
            val d = builder.create()
            d.setOwnerActivity(this@UploadsActivity)
            d.show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (NetworkConnection.file_uri_template != null) template =
            UriTemplate.fromTemplate(NetworkConnection.file_uri_template)
        super.onCreate(savedInstanceState)
        setTheme(ColorScheme.getDialogWhenLargeTheme(ColorScheme.getUserTheme()))
        onMultiWindowModeChanged(isMultiWindow)

        try {
            val httpCacheDir = File(cacheDir, "http")
            val httpCacheSize = (10 * 1024 * 1024).toLong() // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        } catch (e: IOException) {
            Log.i("IRCCloud", "HTTP response cache installation failed:$e")
        }
        setContentView(R.layout.listview)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        setSupportActionBar(toolbar)

        if (supportActionBar != null) {
            supportActionBar!!.elevation = 0f
        }

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState != null && savedInstanceState.containsKey("cid")) {
            cid = savedInstanceState.getInt("cid")
            to = savedInstanceState.getString("to")
            msg = savedInstanceState.getString("msg")
            page = savedInstanceState.getInt("page")
            try {
                val files = savedInstanceState.getSerializable("adapter") as Array<File>?
                for (f in files!!) {
                    adapter.addFile(f)
                }
                adapter.notifyDataSetChanged()
            } catch (e: ClassCastException) {
                adapter.clear()
                adapter.notifyDataSetInvalidated()
                page = 0
            }
        }

        footer = layoutInflater.inflate(R.layout.messageview_header, null)
        val listView = findViewById<ListView>(android.R.id.list)
        listView.adapter = adapter
        listView.addFooterView(footer)
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(absListView: AbsListView, i: Int) {
            }

            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                if (canLoadMore && firstVisibleItem + visibleItemCount > totalItemCount - 4) {
                    canLoadMore = false
                    lifecycleScope.launch {
                        fetchFiles()
                    }
                }
            }
        })
        listView.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View,
                    i: Int,
                    l: Long
                ) {
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                }
            }
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, i, _ ->
                if (i < adapter.count) {
                    val f =
                        adapter.getItem(i) as File

                    val builder =
                        AlertDialog.Builder(this@UploadsActivity)
                    val v =
                        layoutInflater.inflate(R.layout.dialog_upload, null)
                    val messageinput =
                        v.findViewById<EditText>(R.id.message)
                    messageinput.setText(msg)
                    val thumbnail =
                        v.findViewById<ImageView>(R.id.thumbnail)

                    v.viewTreeObserver.addOnGlobalLayoutListener {
                        if (messageinput.hasFocus()) {
                            v.post { v.scrollTo(0, v.bottom) }
                        }
                    }

                    if (f.mime_type.startsWith("image/")) {
                        try {
                            thumbnail.setImageBitmap(f.image)
                            thumbnail.visibility = View.VISIBLE
                            thumbnail.setOnClickListener {
                                val i =
                                    Intent(
                                        this@UploadsActivity,
                                        ImageViewerActivity::class.java
                                    )
                                i.setData(Uri.parse(f.url))
                                startActivity(i)
                            }
                            thumbnail.isClickable = true
                        } catch (e: Exception) {
                            NetworkConnection.printStackTraceToCrashlytics(e)
                        }
                    } else {
                        thumbnail.visibility = View.GONE
                    }

                    (v.findViewById<View>(R.id.filesize) as TextView).text =
                        f.metadata
                    v.findViewById<View>(R.id.filename).visibility =
                        View.GONE
                    v.findViewById<View>(R.id.filename_heading).visibility =
                        View.GONE

                    builder.setTitle("Send A File To $to")
                    builder.setView(v)
                    builder.setPositiveButton(
                        "Send"
                    ) { dialog, _ ->
                        var message: String? = messageinput.text.toString()
                        if (message!!.length > 0) message += " "
                        message += f.url

                        dialog.dismiss()
                        if (parent == null) {
                            setResult(RESULT_OK)
                        } else {
                            parent.setResult(RESULT_OK)
                        }
                        finish()
                        NetworkConnection.getInstance().say(cid, to, message, null)
                    }
                    builder.setNegativeButton(
                        "Cancel"
                    ) { dialog, _ -> dialog.cancel() }
                    val d = builder.create()
                    d.setOwnerActivity(this@UploadsActivity)
                    d.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    d.show()
                }
            }
        NetworkConnection.getInstance().addHandler(this)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        val params = window.attributes
        if (windowManager.defaultDisplay.width > TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                800f,
                resources.displayMetrics
            ) && !isMultiWindow
        ) {
            params.width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                800f,
                resources.displayMetrics
            ).toInt()
            params.height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                800f,
                resources.displayMetrics
            ).toInt()
        } else {
            params.width = -1
            params.height = -1
        }
        window.attributes = params
    }

    override fun onResume() {
        if (NetworkConnection.file_uri_template != null) template =
            UriTemplate.fromTemplate(NetworkConnection.file_uri_template)
        super.onResume()

        if (cid == -1) {
            cid = intent.getIntExtra("cid", -1)
            to = intent.getStringExtra("to")
            msg = intent.getStringExtra("msg")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.clear()

        val cache = HttpResponseCache.getInstalled()
        cache?.flush()
        NetworkConnection.getInstance().removeHandler(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt("cid", cid)
        outState.putString("to", to)
        outState.putString("msg", msg)
        outState.putInt("page", page)
        adapter.saveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkEmpty() {
        if (adapter.count == 0 && !canLoadMore) {
            findViewById<View>(android.R.id.list).visibility = View.GONE
            val empty = findViewById<TextView>(android.R.id.empty)
            empty.visibility = View.VISIBLE
            empty.text = "You haven't uploaded any files to IRCCloud yet."
        } else {
            findViewById<View>(android.R.id.list).visibility =
                View.VISIBLE
            findViewById<View>(android.R.id.empty).visibility = View.GONE
        }
    }
}