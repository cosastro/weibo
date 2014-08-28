package org.robby;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

public class Hbaself {
	Configuration conf;
	public static Hbaself ghbase = null;
	
	public static Hbaself getInstance(){
		if(ghbase == null)
			ghbase = new Hbaself();
		return ghbase;
	}
	Hbaself(){
		conf = HBaseConfiguration.create();
	}
	
	public void create_table(String name, String col, int version) throws Exception{
		HBaseAdmin admin = new HBaseAdmin(conf);
		
		if(admin.tableExists(name)){
			admin.disableTable(name);
			admin.deleteTable(name);
		}
		
		HTableDescriptor tableDes = new HTableDescriptor(name);
		HColumnDescriptor hd = new HColumnDescriptor(col);
		hd.setMaxVersions(version);
		tableDes.addFamily(hd);
		
		admin.createTable(tableDes);
	}
	
	/*
	 * tab_global	row_userid	param:userid
	 * tab_user2id	username	info:id
	 * tab_id2user	id			info:username	  info:password
	 */
	
	public void createTables() throws Exception{
		create_table("tab_global", "param", 1);
		
		Put put = new Put(Bytes.toBytes("row_userid"));
		long id = 0;
		put.add(Bytes.toBytes("param"), 
				Bytes.toBytes("userid"), 
				Bytes.toBytes(id));
		HTable ht = new HTable(conf, "tab_global");
		ht.put(put);
		
		create_table("tab_user2id", "info", 1);
		create_table("tab_id2user", "info", 1);
		
		//table_follow	{userid}	name:{userid}
		create_table("tab_follow", "name", 1);
		
		//table_followed	rowkey:{userid}_{userid}   CF:userid
		create_table("tab_followed", "userid", 1);
		
		//tab_post	rowkey:postid	CF:post:username post:content post:ts
		create_table("tab_post", "post", 1);
		put = new Put(Bytes.toBytes("row_postid"));
		id = 0;
		put.add(Bytes.toBytes("param"), 
				Bytes.toBytes("postid"), 
				Bytes.toBytes(id));
		ht.put(put);
		
		//tab_inbox		rowkey:userid+postid	CF:postid
		create_table("tab_inbox", "postid", 1);
		ht.close();
	}
	
	public boolean post(String username, String content) throws Exception{
		HTable tab_global = new HTable(conf, "tab_global");
		HTable tab_post = new HTable(conf, "tab_post");
		
		long id = tab_global.incrementColumnValue(Bytes.toBytes("row_postid"),
				Bytes.toBytes("param"), 
				Bytes.toBytes("postid"), 1);

		byte[] postid = Bytes.toBytes(id);
		Put put = new Put(postid);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String ts = df.format(new Date());
		
		put.add(Bytes.toBytes("post"), Bytes.toBytes("username"), username.getBytes());
		put.add(Bytes.toBytes("post"), Bytes.toBytes("content"), content.getBytes());
		put.add(Bytes.toBytes("post"), Bytes.toBytes("ts"), ts.getBytes());
		tab_post.put(put);
		tab_post.close();
		tab_global.close();
		
		//step2: deliver the post
		long senderid = this.getIdByUsername(username);
		
		byte[] begin = Bytes.add(Bytes.toBytes(senderid), Bytes.toBytes(Long.MAX_VALUE - Long.MAX_VALUE));
		byte[] end = Bytes.add(Bytes.toBytes(senderid), Bytes.toBytes(Long.MAX_VALUE));
		Scan s = new Scan();
		s.setStartRow(begin);
		s.setStopRow(end);
		
		HTable tab_followed = new HTable(conf, "tab_followed");
		HTable tab_inbox = new HTable(conf, "tab_inbox");
		
		ResultScanner rs = tab_followed.getScanner(s);
		
		//tab_inbox
		List<Put> lst = new ArrayList<Put>();
 		put = new Put(Bytes.add(Bytes.toBytes(senderid), postid));
		put.add(Bytes.toBytes("postid"), null, postid);
		lst.add(put);

		for(Result r : rs){
			byte[] did = r.getValue(Bytes.toBytes("userid"), null);
			put = new Put(Bytes.add(did, postid));
			put.add(Bytes.toBytes("postid"), null, postid);
			lst.add(put);
		}
		tab_inbox.put(lst);
		tab_inbox.close();
		tab_followed.close();
		
		return true;
	}
	
	public List<Post> getPost(String username) throws Exception{
		List<Post> list = new ArrayList<Post>();
		
		long id = this.getIdByUsername(username);
		byte[] begin = Bytes.add(Bytes.toBytes(id), Bytes.toBytes(Long.MAX_VALUE - Long.MAX_VALUE));
		byte[] end = Bytes.add(Bytes.toBytes(id), Bytes.toBytes(Long.MAX_VALUE));
		
		Scan s = new Scan();
		s.setStartRow(begin);
		s.setStopRow(end);
		HTable tab_post = new HTable(conf, "tab_post");
		HTable tab_inbox = new HTable(conf, "tab_inbox");
		
		ResultScanner rs = tab_inbox.getScanner(s);
		Get get = null;
		Post p = null;
		for(Result r : rs){
			byte[] postid = r.getValue(Bytes.toBytes("postid"), null);
			get = new Get(postid);
			Result result = tab_post.get(get);
			
			String post_username = Bytes.toString(result.getValue(
					Bytes.toBytes("post"), Bytes.toBytes("username")));
			String post_content = Bytes.toString(result.getValue(
					Bytes.toBytes("post"), Bytes.toBytes("content")));
			String post_ts = Bytes.toString(result.getValue(
					Bytes.toBytes("post"), Bytes.toBytes("ts")));
			
			System.out.println(post_username + ":" + post_content + ":" + post_ts);
			
			p = new Post(post_username, post_content, post_ts);
			list.add(0, p);
		}
		return list;
	}
	
	public boolean createNewUser(String username, String password) throws Exception{
		HTable tab_user2id = new HTable(conf, "tab_user2id");
		HTable tab_global = new HTable(conf, "tab_global");
		HTable tab_id2user = new HTable(conf, "tab_id2user");
		if(tab_user2id.exists(new Get(username.getBytes()))){
			return false;
		}
		long id = tab_global.incrementColumnValue(
				Bytes.toBytes("row_userid"), 
				Bytes.toBytes("param"), 
				Bytes.toBytes("userid"), 1);
		Put put = new Put(username.getBytes());
		put.add(Bytes.toBytes("info"),
				Bytes.toBytes("id"),
				Bytes.toBytes(id));
		tab_user2id.put(put);
		
		put = new Put(Bytes.toBytes(id));
		put.add(Bytes.toBytes("info"), Bytes.toBytes("username"), username.getBytes());
		put.add(Bytes.toBytes("info"), Bytes.toBytes("password"), password.getBytes());
		tab_id2user.put(put);
		return true;
	}
	
	public long getIdByUsername(String username){
		try{
			HTable tab_user2id = new HTable(conf, "tab_user2id");
			Result rs = tab_user2id.get(new Get(username.getBytes()));
			KeyValue kv = rs.getColumnLatest(
					Bytes.toBytes("info"), 
					Bytes.toBytes("id"));
			byte[] by = kv.getValue();
			return Bytes.toLong(by);
		}catch(Exception e){
			return 0;
		}
	}
	
	public String getNameByUserId(long id){
		try{
			HTable tab_id2user = new HTable(conf, "tab_id2user");
			Result rs = tab_id2user.get(new Get(Bytes.toBytes(id)));
			KeyValue kv = rs.getColumnLatest(
					Bytes.toBytes("info"), 
					Bytes.toBytes("username"));
			return Bytes.toString(kv.getValue());
		}catch(Exception e){
			return "";
		}
	}
	
	public long checkPassword(String username, String password) throws Exception{
		long id = getIdByUsername(username);
		if(id == 0)
			return 0;
		
		HTable tab_id2user = new HTable(conf, "tab_id2user");
		Result rs = tab_id2user.get(new Get(Bytes.toBytes(id)));
		KeyValue kv = rs.getColumnLatest(
				Bytes.toBytes("info"), 
				Bytes.toBytes("password"));
		String passwordInDb = Bytes.toString(kv.getValue());
		
		if(passwordInDb.equals(password))
			return id;
		return 0;
	}
	
	public boolean follow(String oname, String dname) throws Exception{
		long oid = this.getIdByUsername(oname);
		long did = this.getIdByUsername(dname);
		if(oid == 0 || did == 0 || oid == did)
			return false;
		
		HTable tab_follow = new HTable(conf, "tab_follow");
		Put put = new Put(Bytes.toBytes(oid));
		put.add(Bytes.toBytes("name"), 
				Bytes.toBytes(did), 
				dname.getBytes());
		tab_follow.put(put);
		tab_follow.close();
		
		//tab_followed
		HTable tab_followed = new HTable(conf, "tab_followed");
		put = new Put(Bytes.add(Bytes.toBytes(did), 
				Bytes.toBytes(oid)));
		put.add(Bytes.toBytes("userid"), null, Bytes.toBytes(oid));
		tab_followed.put(put);
		tab_followed.close();
		return true;
	}
	
	public boolean unfollow(String oname, String dname) throws Exception{
		long oid = this.getIdByUsername(oname);
		long did = this.getIdByUsername(dname);
		if(oid == 0 || did == 0 || oid == did)
			return false;
		
		HTable tab_follow = new HTable(conf, "tab_follow");
		Delete del = new Delete(Bytes.toBytes(oid));
		del.deleteColumns(Bytes.toBytes("name"),
			Bytes.toBytes(did));
		tab_follow.delete(del);
		tab_follow.close();
		
		HTable tab_followed = new HTable(conf, "tab_followed");
		del = new Delete(Bytes.add(Bytes.toBytes(did), 
				Bytes.toBytes(oid)));
		tab_followed.delete(del);
		tab_followed.close();
		
		return true;
	}
	
	public Set<String> getAllUser() throws Exception{
		Set<String> set = new HashSet<String>();
		HTable tab_user2id = new HTable(conf, "tab_user2id");
		Scan s = new Scan();
		
		ResultScanner ss = tab_user2id.getScanner(s);
		for(Result r : ss){
			String name = new String(r.getRow());
			set.add(name);
			System.out.println(name);
		}
			
		return set;
	}
	
	public Set<String> getFollow(String username) throws Exception{
		Set<String> set = new HashSet<String>();
		
		long id = this.getIdByUsername(username);
		HTable tab_follow = new HTable(conf, "tab_follow");
		Get get = new Get(Bytes.toBytes(id));
		Result rs = tab_follow.get(get);
		for(KeyValue kv : rs.raw()){
			String s = new String(kv.getValue());
			set.add(s);
			System.out.println(s);
		}
		return set;
	}
	
	public static void main(String[] args) throws Exception{
		Hbaself h = new Hbaself();
		/*h.createTables();
		
		h.createNewUser("user1", "123");
		h.createNewUser("user2", "123");
		h.createNewUser("user3", "123");
		h.createNewUser("user4", "123");
		h.createNewUser("user5", "123");
		
		h.follow("user1", "user2");
		h.follow("user1", "user3");
		h.follow("user1", "user4");
		
		h.post("user2", "test");*/
		
		h.getPost("user1");
		
		
		/*h.follow("user1", "user2");
		h.follow("user2", "user3");
		h.unfollow("user1", "user2");
		
		h.getAllUser();*/
		
		//h.follow("user1", "user2");
		//h.follow("user1", "user3");
		
		//h.getFollow("user1");
	}
}
