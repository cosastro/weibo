package org.robby;

public class Post {
	String username;
	String content;
	String ts;
	
	public Post(String username, String content, String ts){
		this.username = username;
		this.content = content;
		this.ts = ts;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getTs() {
		return ts;
	}
	public void setTs(String ts) {
		this.ts = ts;
	}
	
}
