package epic.lms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
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
    public ModelAndView login(HttpSession session, @RequestParam("login") String username, @RequestParam("password") String password) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException {
        found = 0;

        List<User> users = firebaseService.getUser();
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
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

            if(session.getAttribute("userrole").equals("Admin")){
                mv.setViewName("StudentDashboard.html"); //ChangeURL
            } else if (session.getAttribute("userrole").equals("Student")){
                mv.setViewName("StudentDashboard.html");
            }else if (session.getAttribute("userrole").equals("Lecturer")){
                mv.setViewName("Lecturer Dashboard.html");
            }

            //Gets Modules to session
            getModules(session);
            //get Users to session
            session.setAttribute("usersList", users);

        } else {

            mv.setViewName("errorPage.html");
        }

        return mv;
    }

    @PostMapping(value = "/createAccount")
    public ModelAndView createAccount( @RequestParam("username") String username, @RequestParam("psw") String password, @RequestParam("email") String email, @RequestParam("firstname") String firstname,@RequestParam("lastname") String lastname , @RequestParam("role") String role) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException {
        User newUser = new User();
        mv.setViewName("userAccountPage.html"); //CHANGE URL
        newUser.setFirstname(firstname);
        newUser.setSurname(lastname);
        newUser.setRole(role);
        newUser.setPassword(password);
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
    public void getModules(HttpSession session) throws ExecutionException, InterruptedException {
        List<Modules> moduleList = firebaseService.getModules();
        session.setAttribute("moduleList", moduleList);
    }

    @RequestMapping("/redirect-contactStudents")
    public ModelAndView redirectContactStudents() {
        mv.setViewName("LecturerContactStudents.html");
        return mv;
    }

}
