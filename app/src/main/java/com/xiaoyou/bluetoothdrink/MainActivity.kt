package com.xiaoyou.bluetoothdrink

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xuexiang.xui.XUI
import com.xuexiang.xui.widget.picker.RulerView
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.Socket
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1

    // 控件
    private val blueOpen: Button by lazy { findViewById(R.id.bluetoothOpen) }
    private val blueClose: Button by lazy { findViewById(R.id.bluetoothClose) }
    // 状态
    private val drinkStatus: TextView by lazy { findViewById(R.id.status) }

    // 子线程handle
    private var handle: Handler? = null
    private var socket: Socket? = null
    private var conn:MyThread? = null
    // 父线程handle
    private var mainHandle = object: Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            when{
                msg.what == 0 -> {
                    drinkStatus.text = "已连接"
                    blueClose.visibility = View.VISIBLE
                    blueOpen.visibility = View.GONE
                }
                msg.what == 1 -> {
                    drinkStatus.text = "已断开"
                    blueClose.visibility = View.GONE
                    blueOpen.visibility = View.VISIBLE
                }
                msg.what == 3 -> {
                    Toast.makeText(this@MainActivity,"无法建立连接",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        XUI.initTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        // 连接蓝牙和断开蓝牙
        blueOpen.setOnClickListener{
            // 启动线程
            conn = MyThread()
            conn?.start()
        }
        blueClose.setOnClickListener{
            conn?.cancel()
        }
    }

    // 新建子线程来建立socket连接
    // https://blog.csdn.net/VNanyesheshou/article/details/74896575
    private inner class MyThread:Thread(){
        var ous :OutputStream? = null
        var ins :InputStream? = null
        override fun run() {
            try {
                socket = Socket("192.168.123.119", 5678)
                ous = socket?.getOutputStream()
                ins = socket?.getInputStream()
                val thread = HandlerThread("handler thread")
                thread.start()
                handle = object: Handler(thread.looper){
                    override fun handleMessage(msg: Message) {
                        when{
                            msg.what == 0 -> {
                                val data = msg.data
                                val ous = socket?.getOutputStream()
                                ous?.write(data.getString("data")?.toByteArray())
                                ous?.flush()
                            }
                            msg.what == 1 -> {

                            }
                        }
                    }
                }
                mainHandle.sendEmptyMessage(0)
            }catch (e:Exception){
                mainHandle.sendEmptyMessage(3)
            }
        }

        // 发送数据
        fun sendMessage(message:String){
            val msg = Message()
            val data = Bundle()
            data.putString("data",message)
            msg.what = 0
            msg.data = data
            handle?.sendMessage(msg)
        }

        // 取消线程关键socket
        fun cancel(){
            try {
                ous?.close()
                ins?.close()
                socket?.close()
                mainHandle.sendEmptyMessage(1)
            }catch (e:Exception){
                Toast.makeText(this@MainActivity,"关闭出现问题",Toast.LENGTH_SHORT)
            }
        }
    }


    // 控件销毁时触发
    override fun onDestroy() {
        super.onDestroy()
//        serialPort?.disconnect()
        Log.e("xiaoyou", "销毁")
    }


    @SuppressLint("ShowToast")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == REQUEST_ENABLE_BT){
            Toast.makeText(this, "蓝牙启动成功", Toast.LENGTH_SHORT)
        }
    }

    // 初始化视图
    private fun initView(){
        val waterControl: TempControlView = findViewById(R.id.temp_control)
        val tempControl: RulerView = findViewById(R.id.temp)
        // 设置三格代表温度1度
        waterControl.setAngleRate(1)
        waterControl.setTemp(0, 10, 0)
        //设置旋钮是否可旋转
        waterControl.canRotate = true
        // 出水量修改触发
        waterControl.setOnTempChangeListener { temp ->
            // 发送 出水量
            conn?.sendMessage("w${temp}")
//            serialPort?.sendData("w${temp}")
        }
        // 温度修改触发
        tempControl.setOnChooseResultListener(object : RulerView.OnChooseResultListener {
            override fun onEndResult(result: String?) {
                // 发送温度
                conn?.sendMessage("t${result}")
            }

            override fun onScrollResult(result: String?) {

            }
        })
    }
}