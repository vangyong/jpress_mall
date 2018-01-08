package jpress_mall;

import java.sql.Connection;
import java.sql.DriverManager;

public class Test1 {

	public static void main(String[] args) {
		{
			
			String url = "jdbc:mysql://localhost:3306/jpress_mall";
			String driver = "com.mysql.jdbc.Driver";
			try{
				Class.forName(driver);
			}catch(Exception e){
				System.out.println("无法加载驱动");
			}
			
	try {
			Connection con = DriverManager.getConnection(url,"root","root");
			if(!con.isClosed())
				System.out.println("success");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	}

}
