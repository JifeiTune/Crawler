import requests

data=requests.get("http://202.115.47.141/login").headers['Set-Cookie']
cookie=data

header={"User-Agent": "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0"
        ,"Cookie": cookie
        ,"Referer": "http://202.115.47.141/login"
        }

img=requests.get("http://202.115.47.141/img/captcha.jpg",headers=header).content
print(img)
with open("img.jpg","wb") as f:
    f.write(img)


xh=input()
mm=input()

yz=input("输入验证码：")



para = {"j_username" :xh,#学号改成你的
        "j_password": mm,#密码改成你的
        "j_captcha": yz}


respon=requests.post("http://202.115.47.141/j_spring_security_check",data=para,headers=header)
print(respon.url)

data=requests.get("http://202.115.47.141/index.jsp",headers=header)
with open("index.html","wb") as f:
    f.write(data.content)#登进的界面写入文件
