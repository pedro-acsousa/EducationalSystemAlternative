package epic.lms;

import ch.qos.logback.core.pattern.util.IEscapeUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import org.apache.catalina.connector.Response;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.lang.Integer.valueOf;

@RestController
public class DBController {
    ModelAndView mv = new ModelAndView();
    int found = 0;
    String firstname;
    @Autowired
    FirebaseService firebaseService;

    @PostMapping(value = "/login")
    public ModelAndView login( HttpServletResponse response, HttpSession session, @RequestParam("login") String username, @RequestParam("password") String password, Model model) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException, JSONException, InvalidKeySpecException, NoSuchAlgorithmException {
        found = 0;
        Hash hash = new Hash();
        String HashedPassword = hash.HashString(password)[1];
        List<User> users = firebaseService.getUser();
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(HashedPassword)) {
                firstname = user.getFirstname();
                found = 1;
                session.setAttribute("userid", user.getUsername());
                session.setAttribute("userrole", user.getRole());
                break;
            } else {
                found = 0;
            }
        }




        if (found == 1) {
            //set the rest of the session dont forget
            session.setAttribute("firstName", firstname);



            //Gets Modules to session
            getModules(session, response, model);
            //get Users to session
            session.setAttribute("usersList", users);

            if(session.getAttribute("userrole").equals("Admin")){
                mv.setViewName("StudentDashboard.html"); //ChangeURL
            } else if (session.getAttribute("userrole").equals("Student")){
                mv.setViewName("StudentDashboard.html");
            }else if (session.getAttribute("userrole").equals("Lecturer")){
                mv.setViewName("Lecturer Dashboard.html");
            }

        } else {

            mv.setViewName("errorPage.html");
        }

        return mv;
    }

    @PostMapping(value = "/createAccount")
    public ModelAndView createAccount( @RequestParam("username") String username, @RequestParam("psw") String password, @RequestParam("email") String email, @RequestParam("firstname") String firstname,@RequestParam("lastname") String lastname , @RequestParam("role") String role) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException, InvalidKeySpecException, NoSuchAlgorithmException {
        User newUser = new User();
        Hash hash = new Hash();
        String HashedPassword = hash.HashString(password)[1];
        mv.setViewName("userAccountPage.html"); //CHANGE URL
        newUser.setFirstname(firstname);
        newUser.setSurname(lastname);
        newUser.setRole(role);
        newUser.setPassword(HashedPassword);
        newUser.setUsername(username);
        newUser.setEmail(email);

        firebaseService.addUser(newUser);
    return mv;
    }

    @GetMapping(value= "/dismissNotification")
    public ModelAndView dismissNotifs(HttpSession session, @RequestParam("id") String id) throws ExecutionException, InterruptedException {

        System.out.println(id);
        List<Notifications> notifs = firebaseService.getAllNotifications(session);
        List<Notifications> newListNotifs;

        newListNotifs = firebaseService.deleteNotification(notifs, Integer.parseInt(id), session);
        mv.setViewName("StudentDashboard.html");

        session.setAttribute("notifications", newListNotifs);
        return mv;
    }




    @PostMapping("/getNotifications")
    public ModelAndView getNotifs(HttpSession session) throws ExecutionException, InterruptedException {
        session.setAttribute("notifications",firebaseService.getAllNotifications(session));
        return mv;
    }

    @PostMapping("/sendNotificationAll")
    public ModelAndView SendNotificationAll(HttpSession session, @RequestParam("AllStudentsTextArea") String text) throws ExecutionException, InterruptedException, IOException {
        mv.setViewName("LecturerContactStudents.html");
        String sent = firebaseService.sendNotificationAll(text, session);
        return mv;
    }

    @PostMapping("/sendNotificationModule")
    public ModelAndView SendNotificationModule(HttpSession session, @RequestParam("ClassWideTextArea") String text, @RequestParam("Classname") String module) throws ExecutionException, InterruptedException, IOException {
        mv.setViewName("LecturerContactStudents.html");
        String sent = firebaseService.sendNotificationModule(text, session, module);
        return mv;
    }

    @PostMapping("/sendNotificationStudent")
    public ModelAndView SendNotificationStudent(HttpSession session, @RequestParam("SpecStudTextArea") String text, @RequestParam("StudName") String user) throws ExecutionException, InterruptedException, IOException {
        mv.setViewName("LecturerContactStudents.html");
        String sent = firebaseService.sendNotificationStudent(text, session, user);
        return mv;
    }

    @PostMapping("/getModules")
    public void getModules(HttpSession session, HttpServletResponse response, Model model ) throws ExecutionException, InterruptedException, JSONException, IOException {
        List<Modules> moduleList = firebaseService.getModules();
        session.setAttribute("moduleList", moduleList);
        getStudents(session);
        getAssessments(session);

    }

    @PostMapping("/getStudents")
    public void getStudents(HttpSession session) throws ExecutionException, InterruptedException {
        List<Modules> moduleList = (List<Modules>) session.getAttribute("moduleList");
        Multimap<String, String> studentsInModule = ArrayListMultimap.create();

        for(Modules modules: moduleList){
            List<String>students = modules.getStudents();
            for (String student: students){
                studentsInModule.put(modules.getId(),student);
            }
        }
        Gson gson = new Gson();
        String unPrepared = gson.toJson(studentsInModule.asMap());
        String replaceString=unPrepared.replace('\"','#');
        String replaceString1=replaceString.replace(',','~');

        mv.setViewName("LecturerMarkAssessment.html");

        session.setAttribute("studentsInModule", replaceString1);

    }



    @RequestMapping("/redirect-contactStudents")
    public ModelAndView redirectContactStudents() {
        mv.setViewName("LecturerContactStudents.html");
        return mv;
    }

    @RequestMapping("/redirect-setupAssessments")
    public ModelAndView redirectSetupAssessments() {
        mv.setViewName("LecturerSetUpAssessment.html");
        return mv;
    }

    @RequestMapping("/redirect-markAssessments")
    public ModelAndView redirectMarkAssessments(Model model,HttpSession session) throws ExecutionException, InterruptedException, IOException, JSONException {
        mv.setViewName("LecturerMarkAssessment.html");
        getStudents(session);
        getAssessments(session);

        model.addAttribute("studentsInModule",session.getAttribute("studentsInModule"));
        model.addAttribute("assessmentsInModule",session.getAttribute("assessmentsInModule"));
        return mv;
    }

    @RequestMapping("/redirect-login")
    public ModelAndView loginRedirect() {
        mv.setViewName("login.html");
        return mv;
    }
    @RequestMapping("/redirect-signUp")
    public ModelAndView singUpRedirect() {
        mv.setViewName("CreateAccount.html");
        return mv;
    }
    @RequestMapping("/redirect-enrollStudents")
    public ModelAndView enrollStudentsRedirect(HttpSession session, HttpServletResponse response, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {

        List<User> studentList = firebaseService.usersStudent();
        model.addAttribute("students", studentList);
        mv.setViewName("LecturerEnrollStudents.html");
        return mv;
    }
    @RequestMapping("/redirect-studentViewAssignment")
    public ModelAndView studendentViewAssignmentRedirect(HttpSession session, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {
        mv.setViewName("StudentAssignmentView.html");
        getAssessmentStudent(session);
        model.addAttribute("studentAssessments", session.getAttribute("studentAssessments"));
        return mv;
    }

    @RequestMapping("/allStudents")
    public void allStudents(Model model) throws ExecutionException, InterruptedException {
        List<User> studentList = firebaseService.usersStudent();
        model.addAttribute("students", studentList);
    }



    @PostMapping("/createAssessment")
    public ModelAndView createAssessment(HttpSession session, @RequestParam("AssessmentName") String name,
                                 @RequestParam("DueDate") String dueDate, @RequestParam("Class") String module,
                                 @RequestParam("Type") String assessmentType,
                                 @RequestParam("AssessmentTextArea") String spec) throws InterruptedException, ExecutionException, IOException {
        firebaseService.createAssessment(session, name,dueDate, module, assessmentType, spec);
        mv.setViewName("LecturerSetUpAssessment.html");
        return mv;
    }

    public void getAssessments(HttpSession session) throws ExecutionException, InterruptedException, JSONException, IOException {
        Map<String,List<String>> assessmentMap= firebaseService.getAssessmentsList(session);

        Gson gson = new Gson();
        String unPrepared = gson.toJson(assessmentMap);
        String replaceString=unPrepared.replace('\"','#');
        String replaceString1=replaceString.replace(',','~');

        session.setAttribute("assessmentsInModule", replaceString1);
    }

    @PostMapping("/gradeAssessment")
    public ModelAndView gradeAssessment(HttpSession session, @RequestParam("Feedback") String feedback,
                                         @RequestParam("Grade") String grade, @RequestParam("Assessment") String assessment,
                                        @RequestParam("Student") String student, @RequestParam("Class") String module) throws InterruptedException, ExecutionException, IOException, JSONException {
        String success= firebaseService.gradeAssessment(session, feedback,grade, assessment, student, module);
        mv.setViewName("Success.html");
        return mv;
    }
    @PostMapping("/enroll")
    public ModelAndView enroll(HttpSession session,@RequestParam("StudentSelect") String student,
                                        @RequestParam("CourseSelect") String course) throws InterruptedException, ExecutionException, IOException, JSONException {
        ModelAndView x= firebaseService.enroll(session, student,course);

        return x;
    }

    @RequestMapping("/getAssessmentStudent")
    public void getAssessmentStudent(HttpSession session) throws InterruptedException, ExecutionException, JSONException, IOException {
        Multimap<String, Map<String,Assessment>> studentAssessments= firebaseService.getAssessmentsStudent(session);
        Gson gson = new Gson();
        String unPrepared = gson.toJson(studentAssessments.asMap());
        String replaceString=unPrepared.replace('\"','#');
        String replaceString1=replaceString.replace(',','~');
        session.setAttribute("studentAssessments", replaceString1);

    }





}
