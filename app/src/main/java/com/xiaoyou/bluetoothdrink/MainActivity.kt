package com.xiaoyou.bluetoothdrink

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xuexiang.xui.XUI
import com.xuexiang.xui.widget.picker.RulerView
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    // 获取蓝牙
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val REQUEST_ENABLE_BT = 1
    // 使用管道
    private var mHandlerThread: Handler? = null

    private lateinit var conn : ConnectThread

    override fun onCreate(savedInstanceState: Bundle?) {
        XUI.initTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        initBluetooth()
    }

    // 初始化蓝牙
    // https://developer.android.com/guide/topics/connectivity/bluetooth?hl=zh-cn
    private fun initBluetooth(){
        // 打开蓝牙
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        // 查询已经配对的设备
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            // 如果找到这个设备，我们就发起连接
            if ("HC-06" == device.name){
                Log.e("xiaoyou", "发起连接")
                // 发起连接
                conn = ConnectThread(device)
                conn.start()
            }
        }
    }

    // 连接蓝牙
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            // 连接设备
            // https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/41627149
            device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()
            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()
                Log.e("xiaoyou", "连接成功")
                // The connection attempt succeeded. Perform work associated with

                mHandlerThread = object : Handler() {
                    override fun handleMessage(msg: Message) {
                        super.handleMessage(msg)
                        Log.e("sub thread", "---------> msg.what = " + msg.what)
                        val bundle = msg.data
                        val write = socket.outputStream
                        write!!.write(bundle.getString("send")?.toByteArray())
                        write.close()
                    }
                }

            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket.use { socket ->
                    socket?.close()
                }
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("xiaoyou", "Could not close the client socket", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        conn.cancel()
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
        waterControl.setTemp(16, 37, 20)
        //设置旋钮是否可旋转
        waterControl.canRotate = true
        // 出水量修改触发
        waterControl.setOnTempChangeListener { temp ->
            Toast.makeText(
                this@MainActivity,
                "$temp°",
                Toast.LENGTH_SHORT
            ).show()
        }
        // 温度修改触发
        tempControl.setOnChooseResultListener(object : RulerView.OnChooseResultListener {
            override fun onEndResult(result: String?) {
                Log.e("xiaoyou", result ?: "")
                val msg = Message()
                val bundle = Bundle()
                bundle.putString("send", result)
                msg.what = 1
                msg.data = bundle
                mHandlerThread?.sendMessage(msg)
            }

            override fun onScrollResult(result: String?) {

            }
        })

//        tempControl.setOnClickListener(TempControlView.OnClickListener { temp -> Toast.makeText(this@MainActivity, "$temp°", Toast.LENGTH_SHORT).show() })
    }
}