package ai.ocs.wechat.robot;

import java.util.HashMap;

import com.blade.kit.json.JSONObject;

public interface Robot {

	String talk(String user, String msg, String group, HashMap<String, JSONObject> member);
	
}
