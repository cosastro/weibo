package org.robby;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class indexAction extends baseAction{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Set<String> follow;
	Set<String> unfollow;
	List<Post> posts;
	
	public List<Post> getPosts() {
		return posts;
	}

	public void setPosts(List<Post> posts) {
		this.posts = posts;
	}

	public Set<String> getFollow() {
		return follow;
	}

	public void setFollow(Set<String> follow) {
		this.follow = follow;
	}

	public Set<String> getUnfollow() {
		return unfollow;
	}

	public void setUnfollow(Set<String> unfollow) {
		this.unfollow = unfollow;
	}

	public indexAction(){
		follow = new HashSet<String>();
		unfollow = new HashSet<String>();
		posts = new ArrayList<Post>();
	}
	
	public String execute() throws Exception{
		if(login_user.equals(""))
			return SUCCESS;
		Hbaself hbase = Hbaself.getInstance();
		follow = hbase.getFollow(login_user);
		Set<String> all = hbase.getAllUser();
		
		for(String s : all)
		{
			if(!follow.contains(s) && !s.equals(login_user))
				unfollow.add(s);
		}
		
		posts = hbase.getPost(login_user);
		System.out.println("sign->" + posts.size() + " " + login_user);
		return SUCCESS;
	}
}
