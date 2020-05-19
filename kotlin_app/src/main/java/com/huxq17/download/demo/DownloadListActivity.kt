package com.huxq17.download.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.huxq17.download.Pump
import com.huxq17.download.core.DownloadInfo
import com.huxq17.download.demo.installapk.APK
import com.huxq17.download.core.DownloadListener
import com.huxq17.download.utils.LogUtil
import kotlinx.android.synthetic.main.activity_download_list.*
import kotlinx.android.synthetic.main.item_download_list.view.*
import java.util.*

class DownloadListActivity : AppCompatActivity() {

    private var downloadObserver: DownloadListener = object : DownloadListener() {
        override fun onProgress(progress: Int) {
            val downloadInfo = downloadInfo
            val viewHolder = downloadInfo.extraData as DownloadViewHolder
            val tag = map[viewHolder]
            if (tag != null && tag.id == downloadInfo.id) {
                viewHolder.bindData(downloadInfo, status)
            }
        }

        override fun onFailed() {
            super.onFailed()
            LogUtil.e("onFailed code=" + downloadInfo.errorCode)
        }
    }
    private val map = HashMap<DownloadViewHolder, DownloadInfo>()
    private var downloadAdapter: DownloadAdapter? = null
    private lateinit var downloadInfoList: MutableList<DownloadInfo>
    private var tag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tag = intent.getStringExtra("tag")
        setContentView(R.layout.activity_download_list)
        downloadObserver.enable()
        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        //Get all download list.
        downloadInfoList = if (TextUtils.isEmpty(tag)) Pump.getAllDownloadList() else Pump.getDownloadListByTag(tag)

        //Sort download list if need.
        Collections.sort(downloadInfoList) { o1, o2 -> (o1.createTime - o2.createTime).toInt() }
        rvDownloadList.layoutManager = linearLayoutManager
        downloadAdapter = DownloadAdapter(map, downloadInfoList)
        rvDownloadList.adapter = downloadAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadObserver.disable()
        for (downloadInfo in downloadInfoList) {
            Pump.stop(downloadInfo.id)
        }
        Pump.shutdown()
    }

    class DownloadAdapter(var map: HashMap<DownloadViewHolder, DownloadInfo>, var downloadInfoList: MutableList<DownloadInfo>) : androidx.recyclerview.widget.RecyclerView.Adapter<DownloadViewHolder>() {

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): DownloadViewHolder {
            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_download_list, viewGroup, false)
            return DownloadViewHolder(v, this)
        }

        override fun onBindViewHolder(viewHolder: DownloadViewHolder, i: Int) {
            val downloadInfo = downloadInfoList[i]
            viewHolder.bindData(downloadInfo, downloadInfo.status)

            downloadInfo.extraData = viewHolder
            map[viewHolder] = downloadInfo
        }

        fun delete(viewHolder: DownloadViewHolder) {
            val position = viewHolder.adapterPosition
            downloadInfoList.removeAt(position)
            notifyItemRemoved(position)
            map.remove(viewHolder)
        }

        override fun getItemCount(): Int {
            return downloadInfoList.size
        }
    }

    class DownloadViewHolder(itemView: View, adapter: DownloadAdapter) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        lateinit var downloadInfo: DownloadInfo
        lateinit var status: DownloadInfo.Status
        private var totalSizeString: String? = null
        var totalSize: Long = 0
        var dialog: AlertDialog

        init {
            itemView.bt_status.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            dialog = AlertDialog.Builder(itemView.context)
                    .setTitle("Confirm delete?")
                    .setPositiveButton("Yes") { _, _ ->
                        adapter.delete(this@DownloadViewHolder)
                        Pump.deleteById(downloadInfo.id)
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .create()
        }

        fun bindData(downloadInfo: DownloadInfo, status: DownloadInfo.Status) {
            this.downloadInfo = downloadInfo
            this.status = status
            itemView.tv_name.text = downloadInfo.name
            var speed = ""
            val progress = downloadInfo.progress
            itemView.pb_progress.progress = progress
            when (status) {
                DownloadInfo.Status.STOPPED -> itemView.bt_status.text = "Start"
                DownloadInfo.Status.PAUSING -> itemView.bt_status.text = "Pausing"
                DownloadInfo.Status.PAUSED -> itemView.bt_status.text = "Continue"
                DownloadInfo.Status.WAIT -> itemView.bt_status.text = "Waiting"
                DownloadInfo.Status.RUNNING -> {
                    itemView.bt_status.text = "Pause"
                    speed = downloadInfo.speed
                }
                DownloadInfo.Status.FINISHED -> itemView.bt_status.text = "Install"
                DownloadInfo.Status.FAILED -> itemView.bt_status.text = "Retry"
            }
            itemView.tv_speed.text = speed
            val completedSize = downloadInfo.completedSize
            if (totalSize == 0L) {
                val totalSize = downloadInfo.contentLength
                totalSizeString = "/" + Util.getDataSize(totalSize)
            }
            itemView.tv_download.text = Util.getDataSize(completedSize) + totalSizeString!!
        }

        override fun onClick(v: View) {
            if (v === itemView.bt_status) {
                when (status) {
                    DownloadInfo.Status.STOPPED -> Pump.newRequest(downloadInfo.url, downloadInfo.filePath)
                            .setId(downloadInfo.id)
                            .submit()
                    DownloadInfo.Status.PAUSED -> Pump.resume(downloadInfo.id)
                    DownloadInfo.Status.WAIT -> {
                    }
                    DownloadInfo.Status.RUNNING -> Pump.pause(downloadInfo.id)
                    DownloadInfo.Status.FINISHED -> APK.with(itemView.context)
                            .from(downloadInfo.filePath)
                            .install()
                    DownloadInfo.Status.FAILED -> Pump.resume(downloadInfo.id)
                }//do nothing.
            }

        }

        override fun onLongClick(v: View): Boolean {
            dialog.show()
            return true
        }
    }

    companion object {
        fun start(context: Context, tag: String) {
            val intent = Intent(context, DownloadListActivity::class.java)
            intent.putExtra("tag", tag)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
    }

}