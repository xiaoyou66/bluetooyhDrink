#include <ESP8266WiFi.h> 

// 注意，这个烧录完后必须拔掉串口，然后重新插入电脑才有用，要不然会报错

// 设置热点的名字
#define AP_SSID "dirnkControl"
// 设置热点的密码
#define AP_PSW  "12345678"
// 设置wifi模块的最大连接数
#define MAX_CONNECT 5
// 设置wifi服务的IP地址
const int SERVER_PORT = 80;

// 设置wifi的ip地址(wifi的ip地址好像不能和网关地址在同一个网段)
IPAddress local_IP(192, 168, 1, 1); 
// 设置网关
IPAddress gateway(192, 168, 0, 1);
// 设置子网掩码
IPAddress subnet(255, 255, 255, 0); 
// wifiServer服务
WiFiServer WiFi_Server(SERVER_PORT);
// wificlient对象，因为我们要想办法实现多个连接，所以我们创建一个指针数组来存储连接对象
WiFiClient *WiFi_Client[MAX_CONNECT] = {0} ;
// 临时wifi连接
WiFiClient WiFi_Client_Tmp;

void setup() {  
  // 设置波特率
  Serial.begin(115200);
  // 设置wifi模块为AP模式 
  WiFi.mode(WIFI_AP);
  // 设置IP地址网关和子网掩码  
  WiFi.softAPConfig(local_IP, gateway, subnet);  
  // 设置wifi的名字和密码
  WiFi.softAP(AP_SSID,AP_PSW);
  // 打印wifi模块的ip地址
  //Serial.println("IP address = ");
  //Serial.print(WiFi.softAPIP());
  // 启动wifiserver服务
  WiFi_Server.begin();
  //Serial.println("Server online.");
}  
  
void loop() {
  // 判断是否有新的连接
  WiFi_Client_Tmp = WiFi_Server.available();
  // 判断这个连接是否有效
  if(WiFi_Client_Tmp.connected()){
    //Serial.println("new connect");
    // 如果有效，那么设置连接发送数据不延时
    WiFi_Client_Tmp.setNoDelay(true);
    // 使用for循环来遍历我们的连接池
    for(int i =0; i< MAX_CONNECT; i++){
      // 因为连接池里面可能会有连接，所以我们需要找到一个空的位置放入连接对象
      if (WiFi_Client[i] == 0 || !WiFi_Client[i]->connected()){
        // 我们新建一个TCP连接，然后把这个连接放入我们的连接池，放入后跳出循环
        WiFi_Client[i] = new WiFiClient(WiFi_Client_Tmp);
        break;
      }
    }
  }
  // 我们遍历连接池来接收数据
  for(int i =0; i< MAX_CONNECT; i++){
     // 先判断连接池里面的连接是否有效
     if (WiFi_Client[i] != 0 && WiFi_Client[i]->connected()){
        // 如果有效，尝试获取tcp发送的数据
        if (WiFi_Client[i]->available() > 0){
          // 这里说明有数据，我们直接读取tcp连接发送的数据
          String data = WiFi_Client[i]->readStringUntil('e');
          // 我们给单片机发送数据
          Serial.print(data);
          // 打印发送的数据
          //Serial.println(data);
        }
    }
  }
}
