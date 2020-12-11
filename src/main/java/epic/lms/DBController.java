package epic.lms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
public class DBController {
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
                break;
            } else {
                found = 0;
            }
        }
        ModelAndView mv = new ModelAndView();
        mv.setViewName("StudentDashboard.html");


        if (found == 1) {
            //set the rest of the session dont forget
            session.setAttribute("firstName", firstname);
            mv.setViewName("StudentDashboard.html");
        } else {

            mv.setViewName("errorPage.html");
        }

        return mv;
    }

    @PostMapping(value = "/createAccount")
    public ModelAndView createAccount( @RequestParam("username") String username, @RequestParam("psw") String password, @RequestParam("email") String email) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException {
        User newUser = new User();
        ModelAndView mv = new ModelAndView();
        mv.setViewName("login.html"); //CHANGE URL
        newUser.setFirstname("");
        newUser.setLastname("");
        newUser.setRole(""); //CHANGE TO RECEIVE ATTRIBUTE FROM HIDDEN LABEL FROM HTML
        newUser.setPassword(password);
        newUser.setUsername(username);
        newUser.setEmail(email);

        firebaseService.addUser(newUser);
    return mv;
    }
}
