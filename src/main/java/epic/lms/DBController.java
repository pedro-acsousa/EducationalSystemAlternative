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

    /**
     *  This method validates the user password and its hashed value in the database
     *  and redirects to their respective pages with response to the user's role
     * @param response
     * @param session
     * @param username this requests the username from http session
     * @param password this requests the password from http session
     * @param model unused
     * @return unused
     * @throws JSONException if the json file from the db is not valid
     * @throws InvalidKeySpecException if the key type is not supported
     * @throws NoSuchAlgorithmException if decrypt algorithm is not valid
     */
    @PostMapping(value = "/login")
    public ModelAndView login( HttpServletResponse response, HttpSession session, @RequestParam("login") String username, @RequestParam("password") String password, Model model) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException, JSONException, InvalidKeySpecException, NoSuchAlgorithmException {
        found = 0; // resets login valid check
        Hash hash = new Hash();
        String HashedPassword = hash.HashString(password)[1]; // hash entered password in login screen
        List<User> users = firebaseService.getUser(); // get users from firebase database
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(HashedPassword)) { //if both entered username and password match database values
                firstname = user.getFirstname(); // gets the firstname of the user from the database with the getter
                found = 1; // mark login as valid
                session.setAttribute("userid", user.getUsername()); // saves username to session
                session.setAttribute("userrole", user.getRole()); // saves userrole to session
                break;
            } else {
                found = 0; // set login as not successful
            }
        }




        if (found == 1) {
            //set the rest of the session dont forget
            session.setAttribute("firstName", firstname);



            // gets Modules to session
            getModules(session, response, model);
            // get Users to session
            session.setAttribute("usersList", users);

            // page redirects
            if(session.getAttribute("userrole").equals("Public")){
                mv.setViewName("PublicUser.html");
            } else if (session.getAttribute("userrole").equals("Student")){
                mv.setViewName("StudentDashboard.html");
            }else if (session.getAttribute("userrole").equals("Lecturer")){
                mv.setViewName("Lecturer Dashboard.html");
            }

        } else {
            // returns error page
            mv.setViewName("errorPage.html");
        }

        return mv;
    }

    /**
     * This
     *
     * @param username retrieves value from html form
     * @param password retrieves value from html form
     * @param email retrieves value from html form
     * @param firstname retrieves value from html form
     * @param lastname retrieves value from html form
     * @param role retrieves value from html form
     * @return Public User page
     * @throws InvalidKeySpecException if the key is not valid
     * @throws NoSuchAlgorithmException if the hash algorithm does not exist
     */
    @PostMapping(value = "/createAccount")
    public ModelAndView createAccount( @RequestParam("username") String username, @RequestParam("psw") String password, @RequestParam("email") String email, @RequestParam("firstname") String firstname,@RequestParam("lastname") String lastname , @RequestParam("role") String role) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException, InvalidKeySpecException, NoSuchAlgorithmException {
        User newUser = new User();
        Hash hash = new Hash();
        String HashedPassword = hash.HashString(password)[1];
        mv.setViewName("PublicUser.html"); // redirects to public user page
        // saves and add users to the database
        newUser.setFirstname(firstname);
        newUser.setSurname(lastname);
        newUser.setRole(role);
        newUser.setPassword(HashedPassword);
        newUser.setUsername(username);
        newUser.setEmail(email);

        firebaseService.addUser(newUser);
    return mv;
    }

    /**
     * remove read notifications for students
     *
     * @param session gets http session
     * @param id gets all notification
     * @return notifications displaying on student dashboard
     * @throws InterruptedException if firebase connection is not responsive
     */
    @GetMapping(value= "/dismissNotification")
    public ModelAndView dismissNotifs(HttpSession session, @RequestParam("id") String id) throws ExecutionException, InterruptedException {

        System.out.println(id);
        List<Notifications> notifs = firebaseService.getAllNotifications(session);
        List<Notifications> newListNotifs;

        newListNotifs = firebaseService.deleteNotification(notifs, Integer.parseInt(id), session); // removes read notifications
        mv.setViewName("StudentDashboard.html");

        session.setAttribute("notifications", newListNotifs); // setup new notification list
        return mv;
    }


    /**
     * gets notifications
     * @param session gets http session
     * @return notifications on webpage
     */
    @PostMapping("/getNotifications")
    public ModelAndView getNotifs(HttpSession session) throws ExecutionException, InterruptedException {
        session.setAttribute("notifications",firebaseService.getAllNotifications(session)); //gets notifications
        return mv;
    }

    //sends notifications to all people
    @PostMapping("/sendNotificationAll")
    public ModelAndView SendNotificationAll(HttpSession session, @RequestParam("AllStudentsTextArea") String text) throws ExecutionException, InterruptedException, IOException {
        mv.setViewName("LecturerContactStudents.html");
        String sent = firebaseService.sendNotificationAll(text, session);
        return mv;
    }

    //sends notifications by modules
    @PostMapping("/sendNotificationModule")
    public ModelAndView SendNotificationModule(HttpSession session, @RequestParam("ClassWideTextArea") String text, @RequestParam("Classname") String module) throws ExecutionException, InterruptedException, IOException {
        mv.setViewName("LecturerContactStudents.html");
        String sent = firebaseService.sendNotificationModule(text, session, module);
        return mv;
    }
    //sends notifications by students
    @PostMapping("/sendNotificationStudent")
    public ModelAndView SendNotificationStudent(HttpSession session, @RequestParam("SpecStudTextArea") String text, @RequestParam("StudName") String user) throws ExecutionException, InterruptedException, IOException {
        mv.setViewName("LecturerContactStudents.html");
        String sent = firebaseService.sendNotificationStudent(text, session, user);
        return mv;
    }

    //gets modules for students
    @PostMapping("/getModules")
    public void getModules(HttpSession session, HttpServletResponse response, Model model ) throws ExecutionException, InterruptedException, JSONException, IOException {
        List<Modules> moduleList = firebaseService.getModules();
        session.setAttribute("moduleList", moduleList);
        getStudents(session);
        getAssessments(session);

    }

    //lecturer mark assessments
    @PostMapping("/getStudents")
    public void getStudents(HttpSession session) throws ExecutionException, InterruptedException {
        List<Modules> moduleList = (List<Modules>) session.getAttribute("moduleList");
        Multimap<String, String> studentsInModule = ArrayListMultimap.create();

        for (Modules modules: moduleList){ // filtering students of the right course to show up on teacher's dashboard
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

        mv.setViewName("LecturerMarkAssessment.html"); //redirect to page

        session.setAttribute("studentsInModule", replaceString1);

    }


    // page redirect to contact students for lecturers
    @RequestMapping("/redirect-contactStudents")
    public ModelAndView redirectContactStudents() {
        mv.setViewName("LecturerContactStudents.html");
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

    // page redirect from welcome screen to login screen
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

    // page redirect for students viewing assignments
    @RequestMapping("/redirect-studentViewAssignment")
    public ModelAndView studendentViewAssignmentRedirect(HttpSession session, Model model) throws InterruptedException, ExecutionException, JSONException, IOException {
        mv.setViewName("StudentAssignmentView.html"); //redirect
        getAssessmentStudent(session);
        model.addAttribute("studentAssessments", session.getAttribute("studentAssessments"));
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
        String success= firebaseService.gradeAssessment(session, feedback, grade, assessment, student, module);
        mv.setViewName("Success.html"); // page redirect
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
        Multimap<String, Map<String,Assessment>> studentAssessments= firebaseService.getAssessmentsStudent(session); // get student assessments json from firebase
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
        mv = firebaseService.submitAssessment(student, assessment, module, assessmentURL);
        return mv;
    }




}
