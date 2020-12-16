package epic.lms;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;

@Service
public class FirebaseService {
    public List<User> getUser() throws ExecutionException, InterruptedException, IOException {

        Firestore db = FirestoreClient.getFirestore();
        List<User> userArrayList = new ArrayList<User>();

        CollectionReference collectionReference = db.collection("Users");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            userArrayList.add(doc.toObject(User.class));
        }
        return userArrayList;
    }

    public User addUser(User userAdded){

        Firestore db = FirestoreClient.getFirestore();


        Map<String,String> newUser = new HashMap<String,String>();
        newUser.put("username", userAdded.getUsername());
        newUser.put("password", userAdded.getPassword());
        newUser.put("firstname", userAdded.getFirstname());
        newUser.put("lastname", userAdded.getLastname());
        newUser.put("email", userAdded.getEmail());
        newUser.put("role", userAdded.getRole());

        db.collection("Users").document(userAdded.getUsername()).create(newUser);

        return userAdded;

    }

    public List<Notifications> getAllNotifications(HttpSession session) throws ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();

        List<Notifications> notifications = new ArrayList<>();
        CollectionReference collectionReference = db.collection("Notifications");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            if (doc.get("recipient").equals(session.getAttribute("userid")) && (Boolean)doc.get("read")==false){
                notifications.add(doc.toObject(Notifications.class));
            }

        }

        return  notifications;
    }

    public List<Notifications> deleteNotification(List<Notifications> allNotifications, int id, HttpSession session) throws ExecutionException, InterruptedException {

        for (Notifications notification : allNotifications) {
            if (notification.getId() == id) {
                Firestore db = FirestoreClient.getFirestore();
                notification.setRead(true);
                db.collection("Notifications").document("" + notification.getId()).update("read", notification.isRead());
                allNotifications.remove(notification);
                break;
            }
        }
    return allNotifications;
    }
}
