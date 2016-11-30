package ai.ocs.wechat.model;

import java.util.HashMap;

import com.blade.kit.json.JSONArray;
import com.blade.kit.json.JSONObject;

public class WechatContact {

  // 微信联系人列表，可聊天的联系人列表
  private JSONArray memberList;
  private JSONArray contactList;
  private JSONArray groupList;
  private WechatGroup wechatGroup;
  private HashMap<String, JSONObject> contactIndex;

  public WechatContact() {
    // TODO Auto-generated constructor stub
  }

  public JSONArray getMemberList() {
    return memberList;
  }

  public void setMemberList(JSONArray memberList) {
    this.memberList = memberList;
  }

  public JSONArray getContactList() {
    return contactList;
  }

  public void setContactList(JSONArray contactList) {
    this.contactList = contactList;
  }

  public JSONArray getGroupList() {
    return groupList;
  }

  public void setGroupList(JSONArray groupList) {
    this.groupList = groupList;
  }

  public WechatGroup getWechatGroup() {
    return wechatGroup;
  }

  public void setWechatGroup(WechatGroup wechatGroup) {
    this.wechatGroup = wechatGroup;
  }

  public void setContactIndex(HashMap<String, JSONObject> contactIndex) {
    this.contactIndex = contactIndex;
  }
  
  public HashMap<String, JSONObject> getContactIndex() {
    return contactIndex;
  }

  public JSONObject getContact(String username) {
    return contactIndex == null ? null : contactIndex.get(username);
  }
  
  public JSONObject getContact(String groupname, String username) {
    return wechatGroup == null ? null : wechatGroup.getContact(groupname, username);
  } 
  
/*  public JSONObject getGroup(String groupname) {
    return wechatGroup == null ? null : wechatGroup.getGroup(groupname);
  }*/
}
