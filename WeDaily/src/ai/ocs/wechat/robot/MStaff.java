package ai.ocs.wechat.robot;

public class MStaff {
  private String StaffID;
  private String StaffName;
  private String Place;
  private String Project;
  private String MailAddress;
  private String DontWarn;

  MStaff() {

  }

  public void setStaffID(String StaffID) {
    this.StaffID = StaffID;
  }

  public void setStaffName(String StaffName) {
    this.StaffName = StaffName;
  }

  public void setPlace(String Place) {
    this.Place = Place;
  }

  public void setProject(String Project) {
    this.Project = Project;
  }
  
  public void setMailAddress(String MailAddress) {
    this.MailAddress = MailAddress;
  }
  
  public void setDontWarn(String DontWarn) {
    this.DontWarn = DontWarn;
  }

  public String getStaffID() {
    return this.StaffID;
  }

  public String getStaffName() {
    return this.StaffName;
  }

  public String getPlace() {
    return this.Place;
  }

  public String getProject() {
    return this.Project;
  }
  
  public String getMailAddress() {
    return this.MailAddress;
  }
  
  public String getDontWarn() {
    return this.DontWarn;
  }

}
