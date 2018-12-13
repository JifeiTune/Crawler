package main;

import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;



public class Main 
{
	public static void main(String[] args) 
	{
		Scanner input=new Scanner(System.in);
		try
		{
			//第一步，创建Crawler对象，这个过程中会获取一张教务处的验证码图片
			Crawler crawler=new Crawler();
			
			
			//第二步，将获取到的验证码传给前端显示，有两种方法
			
			//法一，将内存中的图片保存为文件，返回文件路径
			/*
			 传入保存路径，这里传空值就保存在类加载路径下
			 注意，如果传入特定路径，末尾别漏了'/'，如传入"res/img/"
			*/
			String path=crawler.saveAsFile("res/");
			System.out.println("验证码文件路径为："+path);
			
			//法二，不保存为文件，直接转为base64编码的字符串（推荐此法）
			String bs4=crawler.getBase64();
			System.out.println("base64字符串："+bs4);
			/*
			得到base64字符串后，前端显示方法为（将bs4替换）：
			<img src="data:image/jpeg;base64,bs4"/>
			*/
			
			//第三步，前端得到用户输入的学号、密码和验证码后，传入参数，执行getInfo方法，返回一个Hashtable
			//测试时先从控制台获取输入
			System.out.println("依次输入学号、密码和验证码，验证码图片保存在本项目类加载路径：");
			String snum,password,checkNum;
			snum=input.next();
			password=input.next();
			checkNum=input.next();
			Hashtable<String, String> ans=crawler.getInfo(snum, password, checkNum);
			//先遍历一下看看有哪些东西吧
			for(Map.Entry<String, String> entry : ans.entrySet()) 
			{	
				System.out.println(entry.getKey()+"\t"+entry.getValue());
			}
			/*
			接下来解释字段
			这个Hashtable存的是字符串与字符串的映射，key和value都是字符串
			访问时像这样ans.get("籍贯");
			有哪些字段根据访问判断
			在不发生断网等异常的情况下，首先"login"字段一定是有的，其值与意义如下
			"0" 验证码错误
			"1" 学号或密码错误
			"2" 登录成功
			"3" 未知
			
			当且仅当成功登录，即ans.get("login").equals("2")时，
			才会有"getInfo"字段，其值与意义如下：
			"0" 可能是因为未评教，无法查看
			"1" 成功获取
			
			当前仅当成功获取，即ans.get("getInfo").equals("1")时，才会有各自信息字段，key值有：
			电子邮件
			手机
			备注2
			学号
			姓名
			英文姓名
			身份证号
			年级
			院系
			专业
			班级
			校区
			是否有学籍
			是否有国家学籍
			学生类别
			学籍状态
			入学日期
			异动否
			入学年级
			学制类型
			性别
			民族
			政治面貌
			国籍
			出生日期
			籍贯
			外语语种
			通讯地址
			高考考生号
			高考总分
			毕业中学
			录取类型
			想要什么获取什么就是
			*/
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
