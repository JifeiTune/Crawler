package main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;;

public class Crawler 
{
	private String homeUrl="http://202.115.47.141/login";//主页
	private String checkUrl="http://202.115.47.141/img/captcha.jpg";//验证码
	private String loginUrl="http://202.115.47.141/j_spring_security_check";//登录
	private String stuInfo="http://202.115.47.141/student/rollManagement/rollInfo/index";
	private CloseableHttpClient httpClient;
	private RequestConfig requestConfig;
	
	private byte[] checkImg;//验证码二进制数据
	
	public Crawler() 
	{
		httpClient = HttpClients.createDefault();
		//超时设置
		requestConfig = RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(5000).build();
		HttpGet getHome = new HttpGet(homeUrl);//先访问主页，得到cookie信息
		getHome.setConfig(requestConfig);
		getHome.addHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0");
		HttpGet getImg = new HttpGet(checkUrl);//带cookie访问验证码
		getImg.setConfig(requestConfig);
		getImg.addHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0");
		try 
		{
			CloseableHttpResponse response=httpClient.execute(getHome);
			response.close();
			response=httpClient.execute(getImg);
			HttpEntity entity=response.getEntity();
			checkImg=EntityUtils.toByteArray(entity);
			response.close();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * 这个方法会将验证码保存至本地，需提供一个文件路径（末尾带'/'），将验证码保存后返回文件路径
	 */
	public String saveAsFile(String path)
	{
		String filePath=null;
		try 
		{
			filePath=path+DigestUtils.md5Hex(checkImg);
			FileOutputStream fileOutputStream=new FileOutputStream(filePath+".jpg");
			fileOutputStream.write(checkImg);
			fileOutputStream.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return filePath;
	}
	
	/*
	 * 这个方法将图片转换为base64字符串，直接传给前端显示，不必再保存为文件
	 */
	public String getBase64()
	{
		return new String(Base64.encodeBase64(checkImg));
	}
	
	/*
	 * 传入学号、密码、验证码，尝试登录，若成功，返回爬取到的个人信息
	 */
	public Hashtable<String, String> getInfo(String snum,String password,String checknum)
	{
		Hashtable<String, String> ans=new Hashtable<String, String>();
		try 
		{
			//登录
			HttpPost login=new HttpPost(loginUrl);
			login.setConfig(requestConfig);
			login.addHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0");
			ArrayList<NameValuePair> para=new ArrayList<>();
			para.add(new BasicNameValuePair("j_username", snum));
			para.add(new BasicNameValuePair("j_password", password));
			para.add(new BasicNameValuePair("j_captcha", checknum));
			login.setEntity(new UrlEncodedFormEntity(para,"utf-8"));
			CloseableHttpResponse response=httpClient.execute(login);
			//根据重定向的网址判断
			String url=response.getFirstHeader("Location").getValue();
			if(url.equals("http://202.115.47.141/login?errorCode=badCaptcha"))
			{
				ans.put("login", "0");
			}
			else if (url.equals("http://202.115.47.141/login?errorCode=badCredentials")) 
			{
				ans.put("login", "1");
			}
			else if(url.equals("http://202.115.47.141/index.jsp")) 
			{
				//登录成功才去爬取
				ans.put("login", "2");
				response.close();//及时关闭，否则后续访问可能被阻塞
				//查看信息，注意，未评教看不了
				HttpGet info = new HttpGet(stuInfo);
				info.setConfig(requestConfig);
				info.addHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0");
				response=httpClient.execute(info);
				HttpEntity res=response.getEntity();
				String html=EntityUtils.toString(res);
				Parser(html, ans);
				response.close();
			}
			else 
			{
				//还可能重定向到什么网址我也不知道，先留着
				ans.put("login", "3");
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return ans;
	}
	
	/*
	 * HTML解析
	 */
	public void Parser(String html,Hashtable<String, String> ans)
	{
		Document doc=Jsoup.parse(html);
		Elements kind=doc.getElementsByClass("profile-info-name");
		Elements value=doc.getElementsByClass("profile-info-value");
		String k,v;
		int num=0;
		
		for(int i=0;i<kind.size();i++)
		{
			k=kind.get(i).text().trim();
			v=value.get(i).text().trim();
			if(!v.equals(""))//value可能为空，非空才存入 
			{
				num++;
				ans.put(k,v);
			}
		}
		if(num==0)
		{
			ans.put("getInfo", "0");
		}
		else 
		{
			ans.put("getInfo", "1");
		}
	}
}
