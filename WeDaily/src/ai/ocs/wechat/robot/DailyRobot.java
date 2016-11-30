package ai.ocs.wechat.robot;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import com.blade.kit.http.HttpRequest;
import com.blade.kit.json.JSONKit;
import com.blade.kit.json.JSONObject;

import ai.ocs.db.DBHelper;

public class DailyRobot implements Robot {

  private DBHelper db;
  private SimpleDateFormat sdf = null;
  private HashMap<String, MStaff> Staff;
  private HashMap<String, String> Domain;
  private String CommandWrite = "填写";
  private String CommandWarn = "提醒";
  private String CommandProject = "项目";
  private String CommandPlace = "地点";
  private String CommandHelp = "帮助";
  private String CommandHelpProvince = "省份";
  private String CommandQuery = "查询";
  private String CommandLoad = "加载";
  private String CommandTalk = "聊天";
  private String InfoHelp;
  private String InfoHelpProvince = null;
  private String Splitline = "\n----------------------\n";
  private final int JobMinLength = 30;
  private final static String ENCODE = "UTF-8";

  public DailyRobot() {
    db = new DBHelper();
    sdf = new SimpleDateFormat("yyyyMMdd");
    Staff = new HashMap<String, MStaff>();
    Domain = new HashMap<String, String>();
    PreparedStatement pst = null;

    InfoHelp = "填写格式如下：<br/>填写/时间(不写默认今天)/内容/进度/省份1/工时1/省份2/工时2...<br/>"
        + "分隔符：“|”、“/”，任选一种<br/>时间格式：YYYYMMDD、今天、空格<br/>"
        + "项目设置：项目/XX，例：项目/四川<br/>地点设置：地点/XX，例：地点/福州<br/>"
        + "指令：填写[w]、提醒[r]、项目[p]、地点[l]、帮助[h]、省份[o]、查询[q]、聊天[t，@填写助手]<br/>"
        + "[]里字母为指令简写，指令与内容之间要有分隔符<br/>项目和地点要先设置才能填写<br/>"
        + "聊天[t]：与机器人聊天的指令，可以问天气，例：t/福州天气、t/笑话等";
    InfoHelp = InfoHelp.replace("<br/>", "\n");

    try {
      pst = db.getConn().prepareStatement(
          "select STAFF_CODE,STAFF_DESC,PROJECT,PLACE,"
              + "MAIL_ADDRESS,NICK_NAME,DONT_WARN from m_staff_wechat");
      ResultSet rs = pst.executeQuery();
      while (rs.next()) {
        MStaff sta = new MStaff();
        sta.setStaffID(rs.getString("STAFF_CODE"));
        sta.setStaffName(rs.getString("STAFF_DESC"));
        sta.setProject(rs.getString("PROJECT"));
        sta.setPlace(rs.getString("PLACE"));
        sta.setMailAddress(rs.getString("MAIL_ADDRESS"));
        sta.setDontWarn(rs.getString("DONT_WARN"));

        Staff.put(rs.getString("STAFF_DESC"), sta);
      }
      rs.close();
      pst.close();

      pst = db.getConn().prepareStatement(
          "select TEXT,CODE from m_domain "
              + "where TABLE_NAME = 'M_REPORT' and TABLE_COL = 'WORK_TIME'");
      rs = pst.executeQuery();
      while (rs.next()) {
        Domain.put(rs.getString("TEXT"), rs.getString("CODE"));
        if (InfoHelpProvince == null)
          InfoHelpProvince = rs.getString("TEXT");
        else
          InfoHelpProvince = InfoHelpProvince + "，" + rs.getString("TEXT");
      }
      rs.close();
      pst.close();
      InfoHelp = InfoHelp + "\n回复[省份]两个字显示省份清单";
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static String Unicode2String(String unicode) {
    if (unicode == null || unicode.equals(""))
      return "";
    StringBuffer string = new StringBuffer();
    String[] hex = unicode.split("\\\\u");
    for (int i = 1; i < hex.length; i++) {
      // 转换出每一个代码点
      int data = Integer.parseInt(hex[i], 16);
      // 追加成string
      string.append((char) data);
    }
    return string.toString();
  }

  /**
   * URL 解码
   * 
   * @return String
   * @date 2015-3-17 下午04:09:51
   */
  public static String getURLDecoderString(String str) {
    String result = "";
    if (null == str) {
      return "";
    }
    try {
      result = java.net.URLDecoder.decode(str, ENCODE);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * URL 转码
   * 
   * @return String
   * @date 2015-3-17 下午04:10:28
   */
  public static String getURLEncoderString(String str) {
    String result = "";
    if (null == str) {
      return "";
    }
    try {
      result = java.net.URLEncoder.encode(str, ENCODE);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return result;
  }

  public String talk(String msg) {
    String result = "";
    try {
      String url = "http://i.itpk.cn/api.php?question="
          + getURLEncoderString(msg.trim());
      result = HttpRequest.get(url).connectTimeout(3000).body();
      if (result.indexOf("{\"title\"") != -1) {
        JSONObject jsonObject = JSONKit.parseObject(result.substring(result
            .indexOf("{\"title\"")));
        result = "[" + jsonObject.getString("title") + "]\n"
            + jsonObject.getString("content");
      } else
        result = result.replace("[cqname]", "我").replace("[name]", "你");
    } catch (Exception e) {
    }
    return result;
  }

  @Override
  public String talk(String user, String msg, String group,
      HashMap<String, JSONObject> member) {
    try {
      if (this.db.getConn() == null)
        return "";
      try {
        this.db.getConn().prepareStatement("select 1 conn from dual")
            .executeQuery().close();
      } catch (Exception ec) {
      }
      MStaff msta = null;
      MStaff sta = null;
      StringTokenizer token = null;
      PreparedStatement pst = null;
      if (group != null) {
        msta = Staff.get(group);
        if (msta == null)
          return "";
        else if (msta.getDontWarn().equals("3")) { // 特殊群{
          if (msg.startsWith("填写助手") || msg.startsWith("@填写助手")
              || msg.startsWith("t/")) {
            if (!msg.startsWith("t/"))
              token = new StringTokenizer("t/"
                  + msg.substring(msg.indexOf("填写助手") + "填写助手".length()), "/");
            else
              token = new StringTokenizer(msg, "/");
            if (token.hasMoreTokens())
              token.nextToken();
            if (token.hasMoreTokens())
              return talk(token.nextToken());
            else
              return "";
          } else
            return "";
        }
      }
      sta = Staff.get(user);
      if (msta != null && sta == null)
        return "@" + user + " 请实名";
      if (sta == null)
        return "";

      MReport report = new MReport();
      String result = null;
      int Action = 0;
      int iPos = 0;
      String to_user = "";
      if (member != null)
        to_user = "@" + user + " \n";

      if (msg.indexOf('|') != -1) {
        token = new StringTokenizer(msg, "|");
      } else if (msg.indexOf('/') != -1) {
        token = new StringTokenizer(msg, "/");
      } else if (msg.indexOf(' ') != -1) {
        token = new StringTokenizer(msg, " ");
      } else if (msg.indexOf("填写助手") != -1) {
        token = new StringTokenizer("t/"
            + msg.substring(msg.indexOf("填写助手") + "填写助手".length()), "/");
      } else if (msg.equals(CommandHelp) || msg.equals("h")
          || msg.equals("CommandQuery") || msg.equals("q")
          || msg.equals("CommandWarn") || msg.equals("r")
          || msg.equals("CommandLoad") || msg.equals("a")
          || msg.equals("CommandProject") || msg.equals("p")
          || msg.equals("CommandPlace") || msg.equals("l")
          || msg.equals("CommandHelpProvince") || msg.equals("o"))
        token = new StringTokenizer(msg + "/", "/");

      String sField = "", sFieldValue = "";
      if (token != null) {
        iPos = 0;
        if (token.hasMoreTokens()) {
          iPos++;
          sFieldValue = token.nextToken();

          // 第三方机器人消息回复
          if (sFieldValue.equals(CommandTalk) || sFieldValue.equals("t")) {
            result = "";
            if (token.hasMoreTokens()) {
              result = talk(token.nextToken());
            }
            return result;
          } else if (sFieldValue.equals(CommandHelp) || sFieldValue.equals("h"))
            Action = 1;
          else if (sFieldValue.equals(CommandWrite) || sFieldValue.equals("w"))
            Action = 2;
          else if (sFieldValue.equals(CommandWarn) || sFieldValue.equals("r"))
            Action = 3;
          else if (sFieldValue.equals(CommandProject)
              || sFieldValue.equals("p"))
            Action = 4;
          else if (sFieldValue.equals(CommandPlace) || sFieldValue.equals("l")) {
            Action = 5;
          } else if (sFieldValue.equals(CommandLoad) || sFieldValue.equals("a")) {
            Action = 6;
          } else if (sFieldValue.equals(CommandHelpProvince)
              || sFieldValue.equals("o"))
            Action = 7;
          else if (sFieldValue.equals(CommandQuery) || sFieldValue.equals("q"))
            Action = 8;
        }
        if (Action > 0) {
          sFieldValue = sta == null ? null : sta.getStaffID();
          if (sFieldValue != null) {
            report.setStaffID(Integer.parseInt(sFieldValue));
            if (Action == 2) {
              if (sta.getPlace() == null || sta.getPlace().equals(""))
                return to_user + "您未设置地点\n格式：地点/XX\n如：地点/福州\n分隔符不要省略哦。";
              else
                report.setPlace(sta.getPlace());
              if (sta.getProject() == null || sta.getProject().equals(""))
                return to_user + "您未设置项目\n格式：项目/XX\n如：项目/四川\n分隔符不要省略哦。";
              else
                report.setProject(sta.getProject());
            }
          } else
            return to_user + "查询不到您的工号信息";
        }
      }

      switch (Action) {
      case 1: // 帮助
        return to_user + InfoHelp;
      case 2: // 工时填写
        try {
          for (iPos = 1; token.hasMoreTokens() && iPos < 8; iPos++) {
            sFieldValue = token.nextToken();
            switch (iPos) {
            case 1:
              sFieldValue = sFieldValue.replace("\n", "");
              if (sFieldValue.equals("今天") || sFieldValue.trim().equals(""))
                report.setRptDate(new Date());
              else
                try {
                  report.setRptDate(sdf.parse(sFieldValue));
                } catch (ParseException pe) {
                  report.setJob(sFieldValue);
                  report.setRptDate(new Date());
                  iPos++;
                }
              report.setFirstTime(Integer.parseInt(sdf.format(report
                  .getRptDate())));
              report.setLastTime(report.getFirstTime());
              break;
            case 2:
              report.setJob(sFieldValue);
              break;
            case 3:
              report.setIFDone(sFieldValue);
              break;
            default:
              if (iPos % 2 == 0)
                report.setText(sFieldValue);
              else {
                try {
                  report.setCode(sFieldValue);
                } catch (NumberFormatException ne) {
                  return to_user + "格式错误，工时必须为数字，最后一个[工时]不写默认为8\n" + InfoHelp;
                }
              }
            }
          }
          if (iPos == 1)
            return to_user + "格式错误\n" + InfoHelp;

          if (report.getTextCount() == 0)
            return to_user + "格式错误，[省份1]不能为空，最后一个[工时]不写默认为8\n" + InfoHelp;

          if (report.getTextCount() == report.getCodeCount() + 1)
            report.setCode("8");

          sFieldValue = report.getJob();
          while (sFieldValue.indexOf("  ") != -1)
            sFieldValue = sFieldValue.replace("  ", " ");
          report.setJob(sFieldValue);
          if (sFieldValue.length() < JobMinLength)
            return to_user + "您填写内容过于简单，请重新填写不少于"
                + String.valueOf(JobMinLength) + "个字，1字母、1汉字同为1个字。";

          if (!report.getIFDone().equals("完成")
              && report.getIFDone().indexOf('%') == -1)
            return to_user + "[完成情况]格式错误，正确格式为：百分比(例：1%..10%..99%)、完成";

          sField = "";
          sFieldValue = "";
          for (iPos = 0; iPos < report.getTextCount(); iPos++) {
            String code = Domain.get(report.getText(iPos));
            if (code != null) {
              sField = sField + "," + code;
              sFieldValue = sFieldValue + ",?";
            }
          }

          result = null;
          String sWorkTime = "";
          Set<Entry<String, String>> entrySet = Domain.entrySet();
          pst = db.getConn()
              .prepareStatement(
                  "select * from m_report "
                      + "where RPT_DATE = ? and STAFF_ID = ?");
          pst.setDate(1, new java.sql.Date(report.getRptDate().getTime()));
          pst.setInt(2, report.getStaffID());
          ResultSet rs = pst.executeQuery();
          if (rs.next()) {
            result = Splitline;
            result = result + "日报内容：" + rs.getString("JOB") + "\n";
            result = result + "完成情况：" + rs.getString("IF_DONE") + "\n";
            result = result + "归属项目：" + rs.getString("PROJECT") + "\n";
            result = result + "工作地点：" + rs.getString("PLACE");
            sWorkTime = "";
            Iterator<Entry<String, String>> iter = entrySet.iterator();
            while (iter.hasNext()) {
              Entry<String, String> entry = (Entry<String, String>) iter.next();
              String Text = entry.getKey();
              String Code = entry.getValue();
              if (rs.getInt(Code) > 0) {
                if (!sWorkTime.equals(""))
                  sWorkTime = sWorkTime + ",";
                sWorkTime = sWorkTime + Text + ":"
                    + String.valueOf(rs.getInt(Code));
              }
            }
            if (!sWorkTime.equals(""))
              result = result + "\n工时分配：" + sWorkTime;
            rs.close();
            pst.close();

            pst = db.getConn().prepareStatement(
                "delete from m_report where RPT_DATE = ? and STAFF_ID = ?");
            pst.setDate(1, new java.sql.Date(report.getRptDate().getTime()));
            pst.setInt(2, report.getStaffID());
            pst.executeUpdate();
            pst.close();
          }

          pst = db.getConn().prepareStatement(
              "insert into m_report(STAFF_ID,RPT_DATE,FIRST_TIME,LAST_TIME,JOB"
                  + ",PLACE,PROJECT,IF_DONE,NEXT_PLAN" + sField + ") "
                  + "values (?,?,?,?,?,?,?,?,?" + sFieldValue + ")");
          pst.setInt(1, report.getStaffID());
          pst.setDate(2, new java.sql.Date(report.getRptDate().getTime()));
          pst.setInt(3, report.getFirstTime());
          pst.setInt(4, report.getLastTime());
          pst.setString(5, report.getJob());

          if (report.getPlace() == null || report.getPlace().equals(""))
            pst.setNull(6, Types.VARCHAR);
          else
            pst.setString(6, report.getPlace());

          if (report.getProject() == null || report.getProject().equals(""))
            pst.setNull(7, Types.VARCHAR);
          else
            pst.setString(7, report.getProject());

          if (report.getIFDone() == null || report.getIFDone().equals(""))
            pst.setNull(8, Types.VARCHAR);
          else
            pst.setString(8, report.getIFDone());

          if (report.getNextPlan() == null || report.getNextPlan().equals(""))
            pst.setNull(9, Types.VARCHAR);
          else
            pst.setString(9, report.getNextPlan());

          iPos = 0;
          for (int i = 0; i < report.getTextCount(); i++) {
            if (Domain.get(report.getText(i)) != null) {
              pst.setInt(iPos + 10, report.getCode(i));
              iPos++;
            }
          }
          pst.executeUpdate();
          pst.close();
        } catch (Exception e) {
          // e.printStackTrace();
          return to_user + "日报填写失败，数据库错误！\n" + e.getMessage();
        }
        if (result != null)
          return to_user + "更新成功,感谢您的辛勤付出!\n以下为更新前填写的内容：" + result;
        else
          return to_user + "填写成功,感谢您的辛勤付出!";
      case 3: // 工时填写提醒
        try {
          result = null;
          Date BeginDate = new Date();
          int DateCount = 1;
          if (token.hasMoreTokens()) {
            sFieldValue = token.nextToken();
            if (!sFieldValue.equals("今天") && !sFieldValue.trim().equals(""))
              try {
                BeginDate = sdf.parse(sFieldValue);
              } catch (ParseException pe) {
              }
          }
          if (token.hasMoreTokens()) {
            sFieldValue = token.nextToken();
            try {
              DateCount = Integer.valueOf(sFieldValue);
            } catch (NumberFormatException ne) {
              DateCount = 1;
            }
          }
          if (DateCount > 7)
            DateCount = 7;
          if (DateCount < 1)
            DateCount = 1;
          report.setRptDate(BeginDate);
          Calendar cal = Calendar.getInstance();
          sField = member == null ? "" : "@";
          for (int i = 0; i < DateCount; i++) {
            iPos = 0;
            sFieldValue = null;
            if (sta.getDontWarn().equals("0")) { // 普天员工只能查询自己填写情况
              pst = db
                  .getConn()
                  .prepareStatement(
                      "select RPT_DATE from m_report where RPT_DATE = ? and STAFF_ID = ?");
              pst.setDate(1, new java.sql.Date(BeginDate.getTime()));
              pst.setInt(2, report.getStaffID());
              ResultSet rs = pst.executeQuery();
              if (!rs.next()) {
                sFieldValue = " 日报未填写\n";
              }
              rs.close();
              pst.close();
              if (sFieldValue != null) {
                if (result == null)
                  result = sdf.format(BeginDate) + sFieldValue;
                else
                  result = result + sdf.format(BeginDate) + sFieldValue;
              }
            } else {
              pst = db.getConn().prepareStatement(
                  "select a.STAFF_DESC from m_staff_wechat a LEFT JOIN m_report b "
                      + "on a.STAFF_CODE=b.STAFF_ID and b.RPT_DATE = ? "
                      + "where b.STAFF_ID is NULL and a.DONT_WARN = 0");
              pst.setDate(1, new java.sql.Date(BeginDate.getTime()));
              ResultSet rs = pst.executeQuery();
              while (rs.next()) {
                if (member != null
                    && member.get(rs.getString("STAFF_DESC")) == null)
                  continue;
                iPos++;
                if (sFieldValue == null)
                  sFieldValue = sField + rs.getString("STAFF_DESC");
                else
                  sFieldValue = sFieldValue + " ，" + sField
                      + rs.getString("STAFF_DESC");
              }
              rs.close();
              pst.close();
              if (sFieldValue != null) {
                if (result == null)
                  result = sdf.format(BeginDate) + " 有[" + String.valueOf(iPos)
                      + "]人未填写：\n" + sFieldValue;
                else
                  result = result + Splitline + sdf.format(BeginDate) + " 有["
                      + String.valueOf(iPos) + "]人未填写：\n" + sFieldValue;
              }
            }

            cal.setTime(BeginDate);
            cal.add(Calendar.DATE, 1);
            BeginDate = cal.getTime();
          }
          if (sta.getDontWarn().equals("0") && result != null)
            result = to_user + result;
          sFieldValue = sdf.format(report.getRptDate());
          if (DateCount > 1 && !report.getRptDate().equals(BeginDate))
            sFieldValue = sFieldValue + "至" + sdf.format(BeginDate);
          if (result == null)
            result = sFieldValue + "日报已填写";
          else if (DateCount > 1)
            result = sFieldValue + "未填写" + Splitline + result;

        } catch (Exception e) {
          // e.printStackTrace();
          return to_user + "日报提醒失败，数据库错误！\n" + e.getMessage();
        }
        return result;
      case 4: // 项目设置
        try {
          if (!token.hasMoreTokens())
            return to_user + "格式错误\n项目/XX 或p/XX\n例：项目/四川，或p/四川";
          sFieldValue = token.nextToken();
          pst = db.getConn().prepareStatement(
              "update m_staff_wechat set PROJECT = ? where STAFF_CODE = ?");
          pst.setString(1, sFieldValue);
          pst.setInt(2, report.getStaffID());
          pst.executeUpdate();
          pst.close();
        } catch (Exception e) {
          // e.printStackTrace();
          return to_user + "项目设置失败，数据库错误！\n" + e.getMessage();
        }
        sta = Staff.get(user);
        if (sta != null)
          sta.setProject(sFieldValue);
        if (sFieldValue.indexOf("项目") == -1)
          sFieldValue = sFieldValue + "项目";
        return to_user + sFieldValue + "将因您的参与而成功。";
      case 5: // 工作地点设置
        try {
          if (!token.hasMoreTokens())
            return to_user + "格式错误\n地点/XX 或l/XX\n例：地点/福州，或l/福州";
          sFieldValue = token.nextToken();
          pst = db.getConn().prepareStatement(
              "update m_staff_wechat set PLACE = ? where STAFF_CODE = ?");
          pst.setString(1, sFieldValue);
          pst.setInt(2, report.getStaffID());
          pst.executeUpdate();
          pst.close();
        } catch (Exception e) {
          // e.printStackTrace();
          return to_user + "项目设置失败，数据库错误！\n" + e.getMessage();
        }
        sta = Staff.get(user);
        if (sta != null)
          sta.setPlace(sFieldValue);
        return to_user + "地点设置成功";
      case 6: // 员工数据加载
        try {
          pst = db.getConn().prepareStatement(
              "select max(STAFF_ID) STAFF_ID from m_staff_wechat");
          ResultSet rs = pst.executeQuery();
          if (rs.next()) {
            iPos = rs.getInt("STAFF_ID");
            rs.close();
            pst.close();
            if (iPos > 0) {
              pst = db.getConn().prepareStatement(
                  "insert into m_staff_wechat"
                      + "(STAFF_ID,STAFF_DESC,STAFF_CODE,MAIL_ADDRESS) "
                      + "select STAFF_ID,STAFF_DESC,STAFF_CODE,MAIL_ADDRESS "
                      + "from m_staff_mail where STAFF_ID > ?");
              pst.setInt(1, iPos);
              pst.executeUpdate();
              pst.close();
            }
          }

          pst = db.getConn().prepareStatement(
              "select STAFF_CODE,STAFF_DESC,PROJECT,PLACE,"
                  + "MAIL_ADDRESS,NICK_NAME,DONT_WARN from m_staff_wechat");
          rs = pst.executeQuery();
          Staff.clear();
          while (rs.next()) {
            sta = new MStaff();
            sta.setStaffID(rs.getString("STAFF_CODE"));
            sta.setStaffName(rs.getString("STAFF_DESC"));
            sta.setProject(rs.getString("PROJECT"));
            sta.setPlace(rs.getString("PLACE"));
            sta.setMailAddress(rs.getString("MAIL_ADDRESS"));
            sta.setDontWarn(rs.getString("DONT_WARN"));

            Staff.put(rs.getString("STAFF_DESC"), sta);
          }
          rs.close();
          pst.close();
        } catch (Exception e) {
          // e.printStackTrace();
          return to_user + "员工数据加载失败，数据库错误！\n" + e.getMessage();
        }
        return to_user + "员工数据加载成功。";
      case 7: // 省份清单显示
        return to_user + "省份：[" + InfoHelpProvince + "]";
      case 8: // 工时查询
        try {
          Date BeginDate = new Date(), EndDate = BeginDate;
          msta = null;
          if (token.hasMoreTokens()) {
            sFieldValue = token.nextToken();
            if (!sFieldValue.equals("今天") && !sFieldValue.trim().equals(""))
              try {
                BeginDate = sdf.parse(sFieldValue);
              } catch (ParseException pe) {
                msta = Staff.get(sFieldValue);
              }
          }
          if (token.hasMoreTokens()) {
            sFieldValue = token.nextToken();
            if (!sFieldValue.equals("今天") && !sFieldValue.trim().equals(""))
              try {
                EndDate = sdf.parse(sFieldValue);
              } catch (ParseException pe) {
                msta = Staff.get(sFieldValue);
              }
          } else
            EndDate = BeginDate;

          sFieldValue = sdf.format(BeginDate);
          if (BeginDate != EndDate)
            sFieldValue = sFieldValue + "至" + sdf.format(EndDate);

          // 查询员工
          if (sta.getDontWarn().equals("2") && token.hasMoreTokens()) {
            msta = Staff.get(token.nextToken());
          }

          if (msta != null && !msta.getStaffID().equals("")
              && !msta.getStaffID().equals("0")) {
            report.setStaffID(Integer.valueOf(msta.getStaffID()));
            sFieldValue = msta.getStaffName() + sFieldValue;
          }

          pst = db.getConn().prepareStatement(
              "select * from m_report "
                  + "where RPT_DATE between ? and ? and STAFF_ID = ?");
          pst.setDate(1, new java.sql.Date(BeginDate.getTime()));
          pst.setDate(2, new java.sql.Date(EndDate.getTime()));
          pst.setInt(3, report.getStaffID());
          ResultSet rs = pst.executeQuery();
          result = "";
          String sWorkTime = "";
          Set<Entry<String, String>> entrySet = Domain.entrySet();
          while (rs.next()) {
            if (!result.equals(""))
              result = result + Splitline;
            result = result + "日报日期："
                + sdf.format(new Date(rs.getDate("RPT_DATE").getTime())) + "\n";
            result = result + "日报内容：" + rs.getString("JOB") + "\n";
            result = result + "完成情况：" + rs.getString("IF_DONE") + "\n";
            result = result + "归属项目：" + rs.getString("PROJECT") + "\n";
            result = result + "工作地点：" + rs.getString("PLACE");
            sWorkTime = "";
            Iterator<Entry<String, String>> iter = entrySet.iterator();
            while (iter.hasNext()) {
              Entry<String, String> entry = (Entry<String, String>) iter.next();
              String Text = entry.getKey();
              String Code = entry.getValue();
              if (rs.getInt(Code) > 0) {
                if (!sWorkTime.equals(""))
                  sWorkTime = sWorkTime + ",";
                sWorkTime = sWorkTime + Text + ":"
                    + String.valueOf(rs.getInt(Code));
              }
            }
            if (!sWorkTime.equals(""))
              result = result + "\n工时分配：" + sWorkTime;
          }
          if (result.equals(""))
            result = "未填写";
          else
            result = Splitline + result;
          rs.close();
          pst.close();

        } catch (Exception e) {
          // e.printStackTrace();
          return to_user + "日报查询失败，数据库错误！\n" + e.getMessage();
        }

        return to_user + sFieldValue + "日报：" + result;
      default:
        return "";
      }
    } catch (Exception ex) {
      return "";
    }
  }
}
