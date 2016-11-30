package ai.ocs.wechat.model;

import java.util.HashMap;

import com.blade.kit.json.JSONArray;
import com.blade.kit.json.JSONObject;

public class WechatGroup {
  // 微信联系人列表，可聊天的联系人列表
  private JSONArray memberList;
  private JSONArray groupList;
  //private HashMap<String, JSONObject> groupIndex;
  private HashMap<String, HashMap<String, JSONObject>> contactIndex;
  private HashMap<String, HashMap<String, JSONObject>> remarkIndex;

  public WechatGroup() {
    // TODO Auto-generated constructor stub
  }

  public JSONArray getMemberList() {
    return memberList;
  }

  public void setMemberList(JSONArray memberList) {
    this.memberList = memberList;
  }

  public JSONArray getGroupList() {
    return groupList;
  }

  public void setGroupList(JSONArray groupList) {
    this.groupList = groupList;
  }

/*  public void setGroupIndex(HashMap<String, JSONObject> groupIndex) {
    this.groupIndex = groupIndex;
  }

  public JSONObject getGroup(String groupname) {
    return groupIndex == null ? null : groupIndex.get(groupname);
  }*/

  public void setContactIndex(
      HashMap<String, HashMap<String, JSONObject>> contactIndex) {
    this.contactIndex = contactIndex;
  }

  public JSONObject getContact(String groupname, String username) {
    if (contactIndex != null) {
      HashMap<String, JSONObject> group = contactIndex.get(groupname);
      return group == null ? null : group.get(username);
    } else
      return null;
  }

  public void setRemarkIndex(
      HashMap<String, HashMap<String, JSONObject>> remarkIndex) {
    this.remarkIndex = remarkIndex;
  }

  public JSONObject getContactByRemark(String groupname, String username) {
    if (remarkIndex != null) {
      HashMap<String, JSONObject> group = remarkIndex.get(groupname);
      return group == null ? null : group.get(username);
    } else
      return null;
  }

  public HashMap<String, JSONObject> getContactByRemark(String groupname) {
    return remarkIndex == null ? null : remarkIndex.get(groupname);
  }

/*  public HashMap<String, JSONObject> getGroupIndex() {
    return groupIndex;
  }*/

  public HashMap<String, HashMap<String, JSONObject>> getContactIndex() {
    return contactIndex;
  }

  public HashMap<String, HashMap<String, JSONObject>> getRemarkIndex() {
    return remarkIndex;
  }

}
