package models;

import cw_models.Student;
import play.db.ebean.Model;
import javax.persistence.*;
import java.util.Date;

@Entity
public class Chat extends Model{
    @Id
    @Column(name = "chat_id")
    private Integer chatId;
	private boolean fromStudent;
    private Integer senderId;
    private String message;
    private Date time;
    @ManyToOne
    @JoinColumn(name = "report_id")
    private Report report;

    public static Finder<Integer, Chat> find = new Finder<Integer, Chat>(
            Integer.class, Chat.class
    );

    public Chat(){ }

    public Integer getChatId(){
        return chatId;
    }
	public boolean isFromStudent(){
	    return fromStudent;
	}
    public Integer getSenderId(){
        return senderId;
    }
    public String getMessage(){
        return message;
    }
    public Date getTime(){
        return time;
    }
    public Report getReport(){
        return report;
    }
    public Student getStudent(){
        if(fromStudent){
            return Student.byId(senderId);
        }
        return null;
    }
    public Invigilator getInvigilator(){
        if(!fromStudent){
            return Invigilator.byId(senderId);
        }
        return null;
    }
}
