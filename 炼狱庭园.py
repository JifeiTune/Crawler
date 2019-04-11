import requests
import hashlib
from bs4 import BeautifulSoup
import json
from json import JSONDecodeError
import threading
import time
import winsound
import requests
import sqlite3
import os
from queue import Queue
import threading
import socket
import socks
import zipfile

home="http://www.rengoku-teien.com/mp3/"#主页
allPages=[]#所有类型音乐的主页
db=sqlite3.connect('music.db',check_same_thread=False)
cur=db.cursor()
num=0#曲编号，作为主键

px={
  "http": "socks5://127.0.0.1:1080"
}

#获取所有类型音乐的主页
def getAllPage():
    r=requests.get("http://www.rengoku-teien.com/mp3/pop.html")
    r.encoding="UTF-8"
    soup=BeautifulSoup(r.text,'html.parser')
    pages=soup.find_all(class_="music_left")[0]
    pages=pages.find_all("a")
    pages=pages[0:12]+pages[14:15]
    for i in pages:
        kind=i["href"].split('.')[0]
        temp=(kind,home+i["href"])
        allPages.append(temp)
        print(temp)
        if not os.path.exists("Music/"+kind+"/"):
            os.makedirs("Music/"+kind+"/")#创建文件夹

#获取所有音乐的名字、类型、下载链接，写入数据库
def getAllMusic():
    for k in allPages:#所有种类
        kind=k[0]
        link=k[1]
        r=requests.get(link)
        r.encoding="UTF-8"
        soup=BeautifulSoup(r.text,'html.parser')
        songs=soup.find_all(class_="song")
        for song in songs:
            name=song.find_all(class_="songtext")[0].p.string
            link=song.find_all(class_="songicon")[0].a["href"]
            link=home+link
            print(name,kind)
            global num
            num+=1
            cur.execute("INSERT into Music(key,name,kind,link,ok) values (?,?,?,?,?)",(num,name,kind,link,False))
    db.commit()

#getAllPage()
#getAllMusic()
que=Queue()#待下载队列，线程安全的
lock = threading.Lock()

#下载线程
def DL():
    while(True):
        if not que.empty():
            data=que.get()
            key=data[0]
            name=data[1]
            kind=data[2]
            link=data[3]
            print("开始下载"+name)
            tout=5#最多尝试5次
            while(tout>0):
                con=requests.get(link,proxies=px)
                if(con.status_code==200):
                    path="Music/"+kind+"/"
                    with open(path+name,"wb") as f:
                        f.write(con.content)
                    z=zipfile.ZipFile(path+name,"r")
                    for f in z.namelist():
                        z.extract(f,path)
                        if(not os.path.exists(path+name+".mp3")):
                            os.rename(path+f,path+name+".mp3")
                    z.close()
                    os.remove(path+name)
                    break
                else:
                    tout-=1
            if(tout==0):
                print(name+"下载失败，请稍后重试")
            else:#下载成功，写入数据库
                print(name+"下载成功")
                lock.acquire()
                cur.execute("update Music set ok=? where key=?",(True,key))
                db.commit()
                lock.release()
        else:
            break

def start():
    #getAllPage()
    cur.execute("SELECT key,name,kind,link from Music where ok=0")
    all=cur.fetchall()
    for i in all:
        que.put(i)
    #5个下载线程
    for i in range(20):
        threading.Thread(target=DL).start()

start()










