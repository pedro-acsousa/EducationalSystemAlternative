package epic.lms;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
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
        newUser.put("lastname", userAdded.getSurname());
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
            if (doc.get("recipient").equals(session.getAttribute("userid")) && (Boolean) doc.get("read")==false){
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
                db.collection("Notifications").document("" + notification.getId() + " " + notification.getRecipient()).update("read", notification.isRead());
                allNotifications.remove(notification);
                break;
            }
        }
    return allNotifications;
    }

    public String sendNotificationAll(String text, HttpSession session) throws ExecutionException, InterruptedException, IOException {

        List<User> users = getUser();
        Firestore db = FirestoreClient.getFirestore();

        //increment id so id is unique
        CollectionReference collectionReference = db.collection("Notifications");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        int id = 0;
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id){
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }

        for(User user : users){
            id=id+1;
            Notifications newNotification = new Notifications();
            newNotification.setContent(text);
            Date date = Calendar.getInstance().getTime();
            SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");
            String s = SDF.format(date);
            newNotification.setDate(s);



            newNotification.setRecipient(user.getUsername());
            newNotification.setSender(String.valueOf(session.getAttribute("userid")));
            newNotification.setId(id);
            newNotification.setRead(false);
            db.collection("Notifications").document("" + id + " " + user.getUsername()).create(newNotification);
        }
        return "Sent Successfully";
    }

    public String sendNotificationModule(String text, HttpSession session, String module) throws ExecutionException, InterruptedException, IOException {
        List<String> studentsModule;
        Firestore db = FirestoreClient.getFirestore();

        //increment id so id is unique
        CollectionReference collectionReference = db.collection("Notifications");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        int id = 0;
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id){
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }

        studentsModule=null;
        //Check Students in a module
        CollectionReference collectionReference1 = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot1 = collectionReference1.get();
        for(DocumentSnapshot doc1 : querySnapshot1.get().getDocuments()){
            if(doc1.getId().equals(module)){
               studentsModule= (List<String>) doc1.get("students");
            }
        }
        for (String student : studentsModule){
            id=id+1;
            Notifications newNotification = new Notifications();
            newNotification.setContent(text);
            Date date = Calendar.getInstance().getTime();
            SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");
            String s = SDF.format(date);
            newNotification.setDate(s);
            newNotification.setRecipient(student);
            newNotification.setSender(String.valueOf(session.getAttribute("userid")));
            newNotification.setId(id);
            newNotification.setRead(false);
            db.collection("Notifications").document("" + id + " " + student).create(newNotification);


        }

    return "Sent Successfully";
    }

    public String sendNotificationStudent(String text, HttpSession session, String studentId) throws InterruptedException, ExecutionException, IOException {


        Firestore db = FirestoreClient.getFirestore();

        //increment id so id is unique
        CollectionReference collectionReference = db.collection("Notifications");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        int id = 0;
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id){
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }

        for (User student : getUser()){
            if (student.getUsername().equals(studentId)) {
                id = id + 1;
                Notifications newNotification = new Notifications();
                newNotification.setContent(text);
                Date date = Calendar.getInstance().getTime();
                SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");
                String s = SDF.format(date);
                newNotification.setDate(s);
                newNotification.setRecipient(studentId);
                newNotification.setSender(String.valueOf(session.getAttribute("userid")));
                newNotification.setId(id);
                newNotification.setRead(false);
                db.collection("Notifications").document("" + id + " " + studentId).create(newNotification);
            }

        }

        return "Sent Successfully";
    }



    public List<Modules> getModules() throws ExecutionException, InterruptedException {
        List<Modules> moduleList = new ArrayList<Modules>();

        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            moduleList.add(doc.toObject(Modules.class));
        }

        return  moduleList;
    }

    public void createAssessment(HttpSession session,String assessmentName, String dueDate, String module, String assessmentType, String spec) throws InterruptedException, ExecutionException, IOException {


        Firestore db = FirestoreClient.getFirestore();

        //Checks if a user has been created in the assignments collection
        for(User user: getUser()){
            for(Modules moduleiter: getModules()){
                List<String> studentsInModule= moduleiter.getStudents();
                if (studentsInModule.contains(user.getUsername()) && module.equals(moduleiter.getId())){


                    //Creates Outter map
                    Map<String, Map<String, Assessment> >  newAssessment = new HashMap<String, Map<String, Assessment>>();


                    Assessment assessmentObject = new Assessment();
                    assessmentObject.setCreator((String) session.getAttribute("userid"));
                    assessmentObject.setDue(dueDate);
                    assessmentObject.setSubmitted(false);
                    assessmentObject.setSpec(spec);
                    assessmentObject.setType(assessmentType);


                    //creates Inner map
                    Map<String, Assessment> newAssessmentDetail = new HashMap<String, Assessment>();
                    newAssessmentDetail.put(assessmentName, assessmentObject);
                    newAssessment.put(module, newAssessmentDetail);


                    //Creates document before writing if non existent
                    CollectionReference collectionReference = db.collection("Assignments");
                    ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

                    for(DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
                        if (!doc.getId().equals(user.getUsername())){
                            db.collection("Assignments").document(user.getUsername()).create(newAssessment);
                        } else{
                            //Updates Assessment in DB
                            db.collection("Assignments").document(user.getUsername()).set(newAssessment,SetOptions.merge());
                        }
                    }
                }
            }
        }

    }

}
