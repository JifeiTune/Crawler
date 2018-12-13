import requests
import time
import random
import tkinter.messagebox

"""
scuinfo虽说所爬信息都是纯文本的，但它是个动态网页，信息不在源代码里，需要通过抓包获取请求信息的数据包格式
经分析，返回数据都是json格式的，每个帖子的网址为“http://www.scuinfo.com/api/post?id=帖号”
帖号从10000000到最新，最新帖号可以从访问首页时的response中获取
每次爬取时，先获取最新帖号，然后一个个爬直到帖号10000000
将结果写入文本文件中，每1000个帖子保存于一个文本
保存格式如下：
{
发帖人昵称  发帖人性别  发帖日期
帖子内容
    回复人  回复人性别  回复日期
    ……
}
网站设置了反爬机制，经测试，即使控制1秒爬一个帖子，ip都会被网站封禁，所以本爬虫没有用上多线程加速爬取，设置为2到5秒爬一个帖子
"""
home="http://www.scuinfo.com/api/posts?pageSize=15"#首页网址
post="http://www.scuinfo.com/api/post?id="#帖子网址，后需拼凑帖号
respon="http://www.scuinfo.com/api/comments/?postId=%d&pageSize="#回复网址，中间需拼凑帖号
header={"Referer": "http://www.scuinfo.com/",
        "User-Agent": "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0"}#必要的请求头，缺失将返回错误码
format = "%Y.%m.%d %H:%M:%S"#时间格式

all=0#当前爬的帖子数量
nums=1#当前保存的文本文件编号


def getNewest():
    """获取最新帖子号"""
    r = requests.get(home, headers=header)
    return r.json()['data'][2]["id"] #最新帖子号


def TransTime(value):
    """Unix时间戳转换成正常时间"""
    value = time.localtime(value)
    return time.strftime(format, value)


def TransG(number):
    """性别码转性别字符串"""
    if (number == 0):
        gender = "未知"
    elif (number == 1):
        gender = "男"
    else:
        gender = "女"
    return gender


def save(s):
    """将数据保存至文件"""
    #print(s,end="")
    with open(str(nums) + ".txt", 'a+', encoding="utf-8") as f:
        f.write(s)


def getOne(Num):
    """获取某个帖子号对应的帖子和回复，并保存于文本文件"""
    global all
    global nums
    start = time.time()#开始计时，用于控制爬取时间

    data=requests.get(post+str(Num),headers=header).json()["data"]#返回数据为json格式
    if(data!=[]):#先确认是否存在这个帖子，有些帖子可能被删除了
        all+=1#爬取的帖子数加一
        if(all%1000==0):#每爬1000个帖子创建一个新文件
            nums+=1
        gender=TransG(data["gender"])#将性别码转换成字符串
        #构造帖子数据并写入文本文件
        save(data["nickname"]+" "+gender+"  "+TransTime(data["date"])+"\n"+data["content"]+"\n\n")
        data=requests.get(respon % (Num),headers=header).json()["data"]#请求回复数据
        if(data!=[]):#该帖存在回复
            save("----------------------------------------\n")
            Len=len(data)#回复的条数
            for i in range(0,Len):
                gender=TransG(data[i]["gender"])#将性别码转换成字符串
                #构造该帖子的回复数据并写入文本文件
                save("\t\t"+data[i]["nickname"]+" "+gender+"  "+TransTime(data[i]["date"])+"\n\t\t"+data[i]["content"]+"\n\n")
    end=time.time()#结束计时
    time.sleep(max(0,random.uniform(5,10)-(end-start)/1000))#确保爬取一个帖子的时间不会低于2秒

try:
    N=getNewest()
    while(N>=10000000):#从最新的帖子号爬到10000000号
        getOne(N)
        N=N-1
        """
        当IP被对方封禁时，返回的json数据内容将发生变化
        如果还将之当做正确数据来处理，将产生KeyError，此时程序弹出警告框并终止
        """
except KeyError:
    tkinter.messagebox.showerror('错误', 'IP已被对方封禁！')