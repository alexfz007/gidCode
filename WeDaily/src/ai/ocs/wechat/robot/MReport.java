package ai.ocs.wechat.robot;

import java.util.Date;
import java.util.Vector;

public class MReport {
  private int StaffID;
  private Date RptDate;
  private int FirstTime;
  private int LastTime;
  private String Job;
  private String Place;
  private String Project;
  private String IFDone;
  private String NextPlan;
  private Vector<String> Text;
  private Vector<String> Code;

  public MReport() {
    Text = new Vector<String>();
    Code = new Vector<String>();
    RptDate = new Date();
  }

  public int getStaffID() {
    return StaffID;
  }

  public Date getRptDate() {
    return RptDate;
  }

  public int getFirstTime() {
    return FirstTime;
  }

  public int getLastTime() {
    return LastTime;
  }

  public String getJob() {
    return Job;
  }

  public String getPlace() {
    return Place;
  }

  public String getProject() {
    return Project;
  }

  public String getIFDone() {
    return IFDone;
  }

  public String getNextPlan() {
    return NextPlan;
  }

  public int getTextCount() {
    return Text.size();
  }
  
  public int getCodeCount() {
    return Code.size();
  }

  public String getText(int i) {
    return Text.get(i);
  }

  public int getCode(int i) {
    return Integer.parseInt(Code.get(i));
  }

  public void setStaffID(int StaffID) {
    this.StaffID = StaffID;
  }

  public void setRptDate(Date RptDate) {
    this.RptDate = RptDate;
  }

  public void setFirstTime(int FirstTime) {
    this.FirstTime = FirstTime;
  }

  public void setLastTime(int LastTime) {
    this.LastTime = LastTime;
  }

  public void setJob(String Job) {
    this.Job = Job;
  }

  public void setPlace(String Place) {
    this.Place = Place;
  }

  public void setProject(String Project) {
    this.Project = Project;
  }

  public void setIFDone(String IFDone) {
    this.IFDone = IFDone;
  }

  public void setNextPlan(String NextPlan) {
    this.NextPlan = NextPlan;
  }

  public void setText(String Text) {
    this.Text.add(Text);
  }

  public void setCode(String Code) {
    this.Code.add(Code);
  }
}
