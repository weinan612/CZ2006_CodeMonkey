package controllers.data;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utils.CMException;
import utils.StringGenerator;
import views.html.data.*;
import models.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DataController extends Controller {
    public static Result addData1(){
        List<Student> studentList = Student.find.all();
        List<Course> courseList = Course.find.all();
        return ok(stucourse.render(studentList,courseList));
    }

    public static Result addData2(){
        List<Student> studentList = Student.find.fetch("registrationList").fetch("registrationList.course").findList();
        List<Course> courseList = Course.find.fetch("questionSet").fetch("availableSlots").findList();
        return ok(regquestime.render(studentList,courseList));
    }

    public static Result addStudent(){
        //return an Json Object to the client containing error message or data to display
        ObjectNode result = Json.newObject();
        DynamicForm studentForm = Form.form().bindFromRequest();
        Http.MultipartFormData studentData = request().body().asMultipartFormData();
        try{
            //check whether the form has been uploaded correctly
            if(studentData==null || studentForm.hasErrors()){
                throw new CMException("Form submit error.");
            }
            Http.MultipartFormData.FilePart picture = studentData.getFile("photo");
            if(picture==null){
                throw new CMException("File upload error.");
            }

            //check whether matricNo matches certain format and has not been added (in setMatricNo method)
            //by now, we just set password equal to matricNo
            Student student = new Student();
            String matricNo = studentForm.get("matricNo").toUpperCase();
            student.setMatricNo(matricNo);
            student.setPassword(matricNo);

            //check the uploaded file type and store it to public/uploads
            String fileType = picture.getContentType();
            if(!(fileType.equals("image/jpeg") || fileType.equals("image/png"))){
                throw new CMException("Photo type should be jpeg/png");
            }
            String fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()).toString();
            fileName += StringGenerator.generate(10);
            fileName += "." + fileType.replace("image/","");

            File dictionary = new File("public/uploads");
            if(!dictionary.exists() || !dictionary.isDirectory()){
                dictionary.mkdir();
            }

            File photo = new File("public/uploads/"+fileName);
            picture.getFile().renameTo(photo);

            student.setPhoto(photo);
            student.setPhotoPath(photo.getPath());
            student.save();

            //matricNo and studentId are sent to client to display
            result.put("error",0);
            result.put("matricNo",matricNo);
            result.put("studentId",student.getStudentId());
        }catch (CMException e){
            result.put("error", e.getMessage());
        }catch (Exception e){
            result.put("error",e.getMessage());
        }
        return ok(result);
    }

    public static Result getPhoto(Integer studentId){
        Student student = Student.find.byId(studentId);
        try{
            return ok(new FileInputStream(student.getPhoto()));
        }catch(FileNotFoundException e){
            return ok("Error: photo not found");
        }
    }

    public static Result addCourse(){
        ObjectNode result = Json.newObject();
        DynamicForm courseForm = Form.form().bindFromRequest();
        try{
            if(courseForm.hasErrors()){
                throw new CMException("Form submit error.");
            }
            String courseCode = courseForm.get("courseCode");
            String title = courseForm.get("title");
            Course course = new Course();
            course.setCourseCode(courseCode);
            course.setTitle(title);
            course.save();

            result.put("error",0);
            result.put("courseCode",courseCode);
            result.put("title",title);
        }catch (CMException e){
            result.put("error", e.getMessage());
        }
        return ok(result);
    }

    public static Result addRegistration(){
        ObjectNode result = Json.newObject();
        DynamicForm registrationForm = Form.form().bindFromRequest();
        try{
            if(registrationForm.hasErrors()){
                throw new CMException("Form submit error.");
            }

            Integer studentId = Integer.parseInt(registrationForm.get("studentId"));
            Integer courseId = Integer.parseInt(registrationForm.get("courseId"));
            Student student = Student.byId(studentId);
            if(student==null){
                throw new CMException("Student does not exist!");
            }
            Course course = Course.byId(courseId);
            if(course==null){
                throw new CMException("Course does not exist!");
            }

            Registration registration = new Registration();
            registration.register(student, course);
            registration.save();

            result.put("error",0);
            result.put("studentId",studentId);
            result.put("courseCode",course.getCourseCode());
        }catch (CMException e){
            result.put("error", e.getMessage());
        }
        return ok(result);
    }

    public static Result addQuestion(){
        ObjectNode result = Json.newObject();
        DynamicForm questionForm = Form.form().bindFromRequest();
        try{
            if(questionForm.hasErrors()){
                throw new CMException("Form submit error.");
            }

            Integer courseId = Integer.parseInt(questionForm.get("courseId"));
            Course course = Course.byId(courseId);
            if(course==null){
                throw new CMException("Course does not exist.");
            }

            String content = questionForm.get("content");
            Question question = new Question();
            question.setContent(content);
            question.setCourse(course);
            question.save();
            result.put("courseId", courseId);
            result.put("content",content);
            result.put("error",0);
        }catch (CMException e){
            result.put("error", e.getMessage());
        }
        return ok(result);
    }

    public static Result addSlot(){
        ObjectNode result = Json.newObject();
        DynamicForm slotForm = Form.form().bindFromRequest();
        try{
            if(slotForm.hasErrors()){
                throw new CMException("Form submit error.");
            }

            Integer courseId = Integer.parseInt(slotForm.get("courseId"));
            Course course = Course.byId(courseId);
            if(course==null){
                throw new CMException("Course does not exist.");
            }
            Integer capacity = Integer.parseInt(slotForm.get("capacity"));
            if(capacity==0){
                throw new CMException("Please enter n valid capacity");
            }

            String date = slotForm.get("date");
            String start = slotForm.get("start");
            double period = Double.valueOf(slotForm.get("period"));
            SimpleDateFormat format = new SimpleDateFormat("d/MM/yyyy k:mm:ss");
            String dateString = date + " " + start;
            Date startTime = format.parse(dateString);
            long addition = (long) period*60*60*1000;
            Date endTime = new Date(startTime.getTime() + addition);

            TimeSlot slot = new TimeSlot();
            slot.setSlot(course,startTime,endTime);
            slot.setCapacity(capacity);
            slot.save();
            result.put("error",0);
            result.put("courseId",courseId);
            result.put("date",new SimpleDateFormat("dd/MM/yyyy").format(startTime));
            result.put("start",new SimpleDateFormat("hh:mm").format(startTime));
            result.put("end",new SimpleDateFormat("hh:mm").format(endTime));
            result.put("capacity",capacity);
        }catch (CMException e){
            result.put("error", e.getMessage());
        }catch (Exception e){
            result.put("error", e.getMessage());
        }
        return ok(result);
    }
}