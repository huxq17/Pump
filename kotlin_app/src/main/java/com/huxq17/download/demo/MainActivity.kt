package com.huxq17.download.demo

import android.app.ProgressDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast

import com.huxq17.download.config.DownloadConfig
import com.huxq17.download.Pump
import com.huxq17.download.demo.installapk.APK
import com.huxq17.download.core.DownloadListener
import kotlinx.android.synthetic.main.activity_main.*

import java.io.File

class MainActivity : AppCompatActivity() {
    //        private String url = "http://dlied5.myapp.com/myapp/1104466820/sgame/2017_com.tencent.tmgp.sgame_h178_1.41.2.16_5a7ef8.apk";
    //    private String url = "http://down.youxifan.com/Q6ICeD";
    private val url = "http://down.youxifan.com/Q6ICeD"
    //    private String url = "http://xiazai.3733.com/pojie/game/podsctjpjb.apk";
    private val url2 = "https://file.izuiyou.com/download/package/zuiyou.apk?from=ixiaochuan"
    private var url4 = "http://v.nq6.com/xinqu.apk"
    //    String url5 = "http://t2.hddhhn.com/uploads/tu/201612/98/st93.png";
    private var url5 = "http://xiazai.3733.com/pojie/game/podsctjpjb.apk"
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initProgressDialog()
        //只要在第一次提交下载任务之前设置就可以。建议在application的onCreate里做
        DownloadConfig.newBuilder()
                //Optional,set the maximum number of tasks to run, default 3.
                .setMaxRunningTaskNum(2)
                //Optional,set the minimum available storage space size for downloading to avoid insufficient storage space during downloading, default is 4kb.
                .setMinUsableStorageSpace(4 * 1024L)
                .build()
        //        Pump.subscribe(downloadObserver);
        //        try {
        //            File httpCacheDir = new File(getCacheDir(), "http");
        //            long httpCacheSize = 50 * 1024 * 1024;
        //            Class.forName("android.net.http.HttpResponseCache")
        //                    .getMethod("install", File.class, long.class)
        //                    .invoke(null, httpCacheDir, httpCacheSize);
        //        } catch (Exception httpResponseCacheNotAvailable) {
        //        }
        add_task.setOnClickListener {
            progressDialog.progress = 0
            progressDialog.show()
            Pump.newRequest(url2)
                    .listener(object : DownloadListener() {

                        override fun onProgress(progress: Int) {
                            progressDialog.progress = progress
                        }

                        override fun onSuccess() {
                            progressDialog.dismiss()
                            val apkPath = downloadInfo.filePath
                            APK.with(this@MainActivity)
                                    .from(apkPath)
                                    //                                        .forceInstall();
                                    .install()
                            Toast.makeText(this@MainActivity, "Download Finished", Toast.LENGTH_SHORT).show()
                        }

                        override fun onFailed() {
                            progressDialog.dismiss()
                            Toast.makeText(this@MainActivity, "Download failed", Toast.LENGTH_SHORT).show()
                        }
                    })
                    //Optionally,Set whether to repeatedly download the downloaded file,default false.
                    .forceReDownload(true)
                    //Optionally,Set how many threads are used when downloading,default 3.
                    .threadNum(3)
                    .setRetry(3, 200)
                    .submit()
        }

        findViewById<View>(R.id.add_download_list).setOnClickListener {
            val file1 = File(externalCacheDir!!.absolutePath, "download1.apk")
            val file3 = File(externalCacheDir!!.absolutePath, "download2.apk")
            val file4 = File(externalCacheDir!!.absolutePath, "download3.apk")
            Pump.newRequest(url, file1.absolutePath)
                    .tag(TAG)
                    .forceReDownload(true)
                    .submit()
            Pump.newRequest(url4, file3.absolutePath)
                    .tag(TAG)
                    .forceReDownload(true)
                    .submit()
            Pump.newRequest(url5, file4.absolutePath)
                    .tag(TAG)
                    .forceReDownload(true)
                    .submit()
        }

        jump_download_list.setOnClickListener { v ->
            val groupByTag = false
            DownloadListActivity.start(v.context, if (groupByTag) TAG else "")
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        Pump.unSubscribe(url2)
    }

    private fun initProgressDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Downloading")
        //        progressDialog.setMessage("Downloading now...");
        progressDialog.progress = 0
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog.dismiss()
        //shutdown will stop all tasks and release some resource.
        Pump.shutdown()
    }

    companion object {
        private const val TAG = "groupA"
    }
}
