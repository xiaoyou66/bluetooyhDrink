package com.xiaoyou.bluetoothdrink

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xuexiang.xui.XUI
import com.xuexiang.xui.widget.picker.RulerView
import org.w3c.dom.Text
import world.shanya.serialport.SerialPort
import world.shanya.serialport.SerialPortBuilder
import java.io.IOException
import java.lang.Exception
import java.util.*


class MainActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1

    // 控件
    private val blueOpen: Button by lazy { findViewById(R.id.bluetoothOpen) }
    private val blueClose: Button by lazy { findViewById(R.id.bluetoothClose) }
    // 状态
    private val drinkStatus: TextView by lazy { findViewById(R.id.status) }

    var serialPort: SerialPort? = null

    private var handle = object: Handler(Looper.getMainLooper()){
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
            initBluetooth()
        }
        blueClose.setOnClickListener{
            serialPort?.disconnect()
        }
    }

    // 初始化蓝牙
    // https://www.shanya.world/archives/serialport.html
    private fun initBluetooth(){
        serialPort = SerialPortBuilder
                //是否开启Debug模式（Debug模式在Logcat打印一些信息，便于调试）
                .isDebug(true)
                //是否开启自动连接
                .autoConnect(true)
                //是否在未连接设备时自动打开默认的搜索页面
                .autoOpenDiscoveryActivity(true)
                //设置接收数据格式（SerialPort.READ_HEX 为十六进制，SerialPort.READ_STRING 为字符串）
                .setReadDataType(SerialPort.READ_STRING)
                //设置接收数据格式（SerialPort.SEND_HEX 为十六进制，SerialPort.SEND_STRING 为字符串）
                .setSendDataType(SerialPort.SEND_STRING)
                //设置接收 消息监听
                .setReceivedDataListener {
                    Log.d("SerialPortDebug", "received: ${it}")
                }
                //设置连接状态监听 （status 为连接状态，device 为当前连接设备）
                .setConnectStatusCallback { status, device ->
                    if (status) {
                        handle.sendEmptyMessage(0)
                        Log.d("SerialPortDebug", "连接: ${device.address}")
                    } else {
                        handle.sendEmptyMessage(1)
                        Log.d("SerialPortDebug", "断开")
                    }
                }
                //创建实例（需要传入上下文）
                .build(this)
        // 打开搜索页面
        serialPort?.openDiscoveryActivity()
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
        waterControl.setTemp(16, 37, 20)
        //设置旋钮是否可旋转
        waterControl.canRotate = true
        // 出水量修改触发
        waterControl.setOnTempChangeListener { temp ->
            // 发送 出水量
            serialPort?.sendData("w${temp}")
        }
        // 温度修改触发
        tempControl.setOnChooseResultListener(object : RulerView.OnChooseResultListener {
            override fun onEndResult(result: String?) {
                Log.e("xiaoyou", result ?: "")
                // 发送温度
                serialPort?.sendData("t${result}")
            }
            override fun onScrollResult(result: String?) {

            }
        })
    }
}