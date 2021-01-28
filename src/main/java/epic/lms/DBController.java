package epic.lms;

import ch.qos.logback.core.pattern.util.IEscapeUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import org.apache.catalina.Session;
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
import javax.servlet.http.HttpServletRequest;
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

    @RequestMapping (value = "/invalidate")
        public ModelAndView invalidate(HttpSession session, HttpServletRequest request){
        session.invalidate();
        mv.setViewName("index.html");
        return mv;
        }

    // validates user password and hashed value in the database and redirects to their respective pages with response to the user's role
    @PostMapping(value = "/login")
    public ModelAndView login( HttpServletResponse response, HttpSession session, @RequestParam("login") String username, @RequestParam("password") String password, Model model, HttpServletRequest request) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException, JSONException, InvalidKeySpecException, NoSuchAlgorithmException {
        found = 0; // resets login valid check
        Hash hash = new Hash();
        String HashedPassword = Hash.pbkdf2(password, "salt", 5000, 20); // hash entered password in login screen
        List<User> users = firebaseService.getUser(); // get users from firebase database
        // within the user list, if any username and password check is valid
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(HashedPassword)) {
                firstname = user.getFirstname(); // gets the firstname of the user
                found = 1; // mark login as valid
                // saves variables to http session
                session.setAttribute("userid", user.getUsername());
                session.setAttribute("userrole", user.getRole());
                break;
            } else {
                found = 0; // set login as not successful
                mv.setViewName("errorPage.html");
                session.setAttribute("error","Username not found!");
            }
        }
        if (found == 1) {
            //set the rest of the session dont forget
            session.setAttribute("firstName", firstname);
            // Gets Modules to session
            getModules(session, response, model);
            // get Users to session
            session.setAttribute("usersList", users);
            // page redirects according to user
            if (session.getAttribute("userrole").equals("Public")) {

                Map<String,News> newsMap= firebaseService.getNews();
                Gson gson = new Gson();
                String unPrepared = gson.toJson(newsMap);
                String replaceString=unPrepared.replace('\"','#');
                model.addAttribute("newsContent", replaceString);


                mv.setViewName("PublicUser.html");
            } else if (session.getAttribute("userrole").equals("Student")) {
                mv.setViewName("StudentDashboard.html");
            } else if (session.getAttribute("userrole").equals("Lecturer")) {
                mv.setViewName("Lecturer Dashboard.html");
            } else if (session.getAttribute("userrole").equals("Admin")){
                mv.setViewName("AdminCreateStudent.html");
            } else{
                // returns error page
                mv.setViewName("errorPage.html");
            }
        }

        return mv;
    }
    // Performs account creation credential inject to database and redirects to public page
    @PostMapping(value = "/createAccount")
    public ModelAndView createAccount( @RequestParam("username") String username, @RequestParam("psw") String password, @RequestParam("email") String email, @RequestParam("firstname") String firstname,@RequestParam("lastname") String lastname , @RequestParam("role") String role, HttpSession session, Model model) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException, InvalidKeySpecException, NoSuchAlgorithmException {

        User newUser = new User();
        Hash hash = new Hash();
        String HashedPassword = Hash.pbkdf2(password, "salt", 5000, 20);
        mv.setViewName("PublicUser.html"); // redirects to public user page
        // saves credentials
        newUser.setFirstname(firstname);
        newUser.setSurname(lastname);
        newUser.setRole(role);
        newUser.setPassword(HashedPassword);
        newUser.setUsername(username);
        newUser.setEmail(email);
        // add credentials to the database
        firebaseService.addUser(newUser);
        if (session.getAttribute("userrole") != null) {
            if(session.getAttribute("userrole").equals("Admin")){
                mv.setViewName("Success.html");
            }
        }

        Map<String,News> newsMap= firebaseService.getNews();
        Gson gson = new Gson();
        String unPrepared = gson.toJson(newsMap);
        String replaceString=unPrepared.replace('\"','#');
        model.addAttribute("newsContent", replaceString);


        return mv;
    }

    // removing read notifications from students
    @GetMapping(value= "/dismissNotification")
    public ModelAndView dismissNotifs(HttpSession session, @RequestParam("id") String id) throws ExecutionException, InterruptedException {

        System.out.println(id);
        List<Notifications> notifs = firebaseService.getAllNotifications(session);
        List<Notifications> newListNotifs;

        newListNotifs = firebaseService.deleteNotification(notifs, Integer.parseInt(id), session); // removes read notifications
        mv.setViewName("StudentDashboard.html");

        session.setAttribute("notifications", newListNotifs); // removes dismissed notification and setup a new notification list
        return mv;
    }
    // for students getting notifications
    @PostMapping("/getNotifications")
    public ModelAndView getNotifs(HttpSession session) throws ExecutionException, InterruptedException {
        session.setAttribute("notifications",firebaseService.getAllNotifications(session)); //gets notifications
        if(session.getAttribute("userrole").equals("Lecturer")){
            mv.setViewName("Lecturer Dashboard.html");
        }else if(session.getAttribute("userrole").equals("Student")){
            mv.setViewName("StudentDashboard.html");
        }
        return mv;
    }
    // sends notifications to all students
    @PostMapping("/sendNotificationAll")
    public ModelAndView SendNotificationAll(HttpSession session, @RequestParam("AllStudentsTextArea") String text) throws ExecutionException, InterruptedException, IOException {
        mv.setViewName("LecturerContactStudents.html");
        String sent = firebaseService.sendNotificationAll(text, session);
        return mv;
    }
    // sends notifications to whole class grouped by modules
    @PostMapping("/sendNotificationModule")
    public ModelAndView SendNotificationModule(HttpSession session, @RequestParam("ClassWideTextArea") String text, @RequestParam("Classname") String module) throws ExecutionException, InterruptedException, IOException {
        mv.setViewName("LecturerContactStudents.html");
        String sent = firebaseService.sendNotificationModule(text, session, module);
        return mv;
    }
    // sends notifications by individual students
    @PostMapping("/sendNotificationStudent")
    public ModelAndView SendNotificationStudent(HttpSession session, @RequestParam("SpecStudTextArea") String text, @RequestParam("StudName") String user) throws ExecutionException, InterruptedException, IOException {
        mv.setViewName("LecturerContactStudents.html");
        String sent = firebaseService.sendNotificationStudent(text, session, user);
        return mv;
    }
    // gets modules for students
    @PostMapping("/getModules")
    public void getModules(HttpSession session, HttpServletResponse response, Model model ) throws ExecutionException, InterruptedException, JSONException, IOException {
        List<Modules> moduleList = firebaseService.getModules(session);
        session.setAttribute("moduleList", moduleList);
        getStudents(session);
        getAssessments(session);
    }
    // lecturer mark assessments
    @PostMapping("/getStudents")
    public void getStudents(HttpSession session) throws ExecutionException, InterruptedException {
        List<Modules> moduleList = (List<Modules>) session.getAttribute("moduleList");
        Multimap<String, String> studentsInModule = ArrayListMultimap.create();
        // filtering students of the right course to show up on teacher's dashboard
        for(Modules modules: moduleList){
            List<String>students = modules.getStudents();
            for (String student: students){
                studentsInModule.put(modules.getId(),student);
            }
        }
        // unpacking json map from database
        Gson gson = new Gson();
        String unPrepared = gson.toJson(studentsInModule.asMap());
        String replaceString=unPrepared.replace('\"','#');
        String replaceString1=replaceString.replace(',','~');
        session.setAttribute("studentsInModule", replaceString1);
    }
    // page redirect to contact students for lecturers (Send Notifications to students)
    @RequestMapping("/redirect-contactStudents")
    public ModelAndView redirectContactStudents() {
        mv.setViewName("LecturerContactStudents.html");
        return mv;
    }

    // page redirect to releaseContent
    @RequestMapping("/redirect-releaseContent")
    public ModelAndView redirectReleaseContent() {
        mv.setViewName("LecturerReleaseContent.html");
        return mv;
    }


    // page redirect to setup assessments for lecturers
    @RequestMapping("/redirect-setupAssessments")
    public ModelAndView redirectSetupAssessments() {
        mv.setViewName("LecturerSetUpAssessment.html");
        return mv;
    }
    // page redirect to submit assessments for students
    @RequestMapping("/redirect-studentSubmitAssessment")
    public ModelAndView redirectSubmitAssessment(Model model,HttpSession session) throws ExecutionException, InterruptedException, IOException, JSONException {
        getStudents(session);
        getAssessmentStudent(session);
        model.addAttribute("studentsInModule",session.getAttribute("studentsInModule"));
        model.addAttribute("studentAssessments",session.getAttribute("studentAssessments"));

        mv.setViewName("StudentAssignmentSubmission.html");
        return mv;
    }
    // page redirect to mark assessments for lecturers
    @RequestMapping("/redirect-markAssessments")
    public ModelAndView redirectMarkAssessments(Model model,HttpSession session) throws ExecutionException, InterruptedException, IOException, JSONException {
        mv.setViewName("LecturerMarkAssessment.html");
        getStudents(session);
        getAssessments(session);
        model.addAttribute("studentsInModule",session.getAttribute("studentsInModule"));
        model.addAttribute("assessmentsInModule",session.getAttribute("assessmentsInModule"));
        return mv;
    }
    // page redirect from welcome screen to login screen
    @RequestMapping("/redirect-login")
    public ModelAndView loginRedirect() {
        mv.setViewName("login.html");
        return mv;
    }
    // page redirect from welcome screen to account creation screen
    @RequestMapping("/redirect-signUp")
    public ModelAndView singUpRedirect() {
        mv.setViewName("CreateAccount.html");
        return mv;
    }
    // page redirect for enrolling students
    @RequestMapping("/redirect-enrollStudents")
    public ModelAndView enrollStudentsRedirect(HttpSession session, HttpServletResponse response, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {

        List<User> studentList = firebaseService.usersStudent();
        model.addAttribute("students", studentList);
        mv.setViewName("LecturerEnrollStudents.html");
        return mv;
    }

    // page redirect for viewing student progress (lecturers)
    @RequestMapping("/redirect-lecturerViewProgress")
    public ModelAndView lecturerViewProgressRedirect(HttpSession session, HttpServletResponse response, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {
        getStudents(session);
        getModules(session, response, model);
        getAssessmentStudent(session);
        model.addAttribute("studentAssessments", session.getAttribute("studentAssessments"));
        model.addAttribute("studentsInModule", session.getAttribute("studentsInModule"));
        model.addAttribute("studentAssessments", session.getAttribute("studentAssessments"));
        mv.setViewName("LecturerViewProgress.html");
        return mv;
    }


    // page redirect for students viewing assignments
    @RequestMapping("/redirect-studentViewAssignment")
    public ModelAndView studendentViewAssignmentRedirect(HttpSession session, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {
        mv.setViewName("StudentAssignmentView.html");
        getAssessmentStudent(session);
        model.addAttribute("studentAssessments", session.getAttribute("studentAssessments"));
        return mv;
    }

    @RequestMapping("/redirect-studentViewProgress")
    public ModelAndView studendentViewProgressRedirect(HttpSession session, HttpServletResponse response, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {
        mv.setViewName("StudentCourseProgress.html");
        getModules(session, response, model);
        //controller to get the assignments for that student and model attribute
        getAssessmentStudent(session);
        model.addAttribute("studentAssessments", session.getAttribute("studentAssessments"));
        return mv;
    }
    @RequestMapping("/redirect-studentViewContents")
    public ModelAndView studendentViewContents(HttpSession session, HttpServletResponse response, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {
        mv.setViewName("StudentCourseContents.html");
        getModules(session, response, model);
        getContent(session, model);

        //puts the content into a model attribute
        model.addAttribute("contentInModule",session.getAttribute("contentInModule"));
        return mv;
    }
    // error page redirect
    @RequestMapping("/redirect-errorPage")
    public ModelAndView errorRedirect() {
        mv.setViewName("errorPage.html");
        return mv;
    }

    @RequestMapping("/redirect-rightDashboard")
    public ModelAndView redirectDashboard(HttpSession session, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {
        if(session.getAttribute("userrole").equals("Student")){
            mv.setViewName("StudentDashboard.html");
        } else if(session.getAttribute("userrole").equals("Lecturer")){
            mv.setViewName("Lecturer Dashboard.html");
        } else if (session.getAttribute("userrole").equals("Public")){
            mv.setViewName("PublicUser.html");
        }else if (session.getAttribute("userrole").equals("Admin")){
            mv.setViewName("AdminCreateStudent.html");
        } else {
            mv.setViewName("errorPage.html");
            model.addAttribute("error","Your session is invalid!");
        }


        return mv;
    }



    // viewing all students
    @RequestMapping("/allStudents")
    public void allStudents(Model model) throws ExecutionException, InterruptedException {
        List<User> studentList = firebaseService.usersStudent(); // get students
        model.addAttribute("students", studentList); // save to session
    }
    // lecturer creating assessments
    @PostMapping("/createAssessment")
    public ModelAndView createAssessment(HttpSession session, @RequestParam("AssessmentName") String name,
                                 @RequestParam("DueDate") String dueDate, @RequestParam("Class") String module,
                                 @RequestParam("Type") String assessmentType,
                                 @RequestParam("AssessmentTextArea") String spec) throws InterruptedException, ExecutionException, IOException {
        firebaseService.createAssessment(session, name,dueDate, module, assessmentType, spec);
        mv.setViewName("LecturerSetUpAssessment.html"); // page forwarding
        return mv;
    }
    // retrieving assessments
    public void getAssessments(HttpSession session) throws ExecutionException, InterruptedException, JSONException, IOException {
        Map<String,List<String>> assessmentMap= firebaseService.getAssessmentsList(session);
        // unpack json map
        Gson gson = new Gson();
        String unPrepared = gson.toJson(assessmentMap);
        String replaceString=unPrepared.replace('\"','#');
        String replaceString1=replaceString.replace(',','~');
        session.setAttribute("assessmentsInModule", replaceString1);
    }
    //lecturer grade assessments
    @PostMapping("/gradeAssessment")
    public ModelAndView gradeAssessment(HttpSession session, @RequestParam("Feedback") String feedback,
                                         @RequestParam("Grade") String grade, @RequestParam("Assessment") String assessment,
                                        @RequestParam("Student") String student, @RequestParam("Class") String module) throws InterruptedException, ExecutionException, IOException, JSONException {
        String success= firebaseService.gradeAssessment(session, feedback,grade, assessment, student, module);
        mv.setViewName("Success.html"); // redirect successful confirmation page
        return mv;
    }
    // enrolling assessments
    @PostMapping("/enroll")
    public ModelAndView enroll(HttpSession session,@RequestParam("StudentSelect") String student,
                                        @RequestParam("CourseSelect") String course) throws InterruptedException, ExecutionException, IOException, JSONException {
        ModelAndView x= firebaseService.enroll(session, student,course);

        return x;
    }
    // get assessments
    @RequestMapping("/getAssessmentStudent")
    public void getAssessmentStudent(HttpSession session) throws InterruptedException, ExecutionException, JSONException, IOException {
        Multimap<String, Map<String,Assessment>> studentAssessments= firebaseService.getAssessmentsStudent(session);
        // retrieve json value of assessment
        Gson gson = new Gson();
        String unPrepared = gson.toJson(studentAssessments.asMap());
        String replaceString=unPrepared.replace('\"','#');
        String replaceString1=replaceString.replace(',','~');
        session.setAttribute("studentAssessments", replaceString1);
    }
    // assessment submission for students
    @PostMapping("/assessmentSubmission")
    public ModelAndView assessmentSubmission(HttpSession session,@RequestParam("assignmentSelect") String assessment,
                                      @RequestParam("courseSelect") String module, @RequestParam("submitLink") String assessmentURL, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {
        String student = (String) session.getAttribute("userid"); // retrieve student name from session
        mv = firebaseService.submitAssessment(student,assessment, module, assessmentURL);
        return mv;
    }

    @PostMapping("/createContent")
    public ModelAndView createContent(HttpSession session,@RequestParam("Class") String module,
                                             @RequestParam("Title") String contentTitle, @RequestParam("URLText") String videoUrl,
                                      @RequestParam("image") String imageUrl, @RequestParam("Content") String content, Model model)
                                      throws InterruptedException, ExecutionException, JSONException, IOException {

        //create call to the method to push content into DB
        mv= firebaseService.createContent(module, contentTitle, videoUrl, imageUrl, content,session);
        return mv;
    }

    @RequestMapping("/getContent")
    public void getContent(HttpSession session, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {
        Map<String, Map<String,Content>> contentInModule= firebaseService.getContent();
        // retrieve json value of assessment
        Gson gson = new Gson();
        String unPrepared = gson.toJson(contentInModule);
        String replaceString=unPrepared.replace('\"','#');
        String replaceString1=replaceString.replace(',','~');
        session.setAttribute("contentInModule", replaceString1);
    }





}
