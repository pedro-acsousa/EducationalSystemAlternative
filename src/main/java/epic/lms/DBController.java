package epic.lms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
public class DBController {

    @Autowired
    FirebaseService firebaseService;

    @GetMapping("/getUser")
    public User getUserDetails() throws InterruptedException, ExecutionException, IOException {
        return firebaseService.getUser("George");
    }
}
