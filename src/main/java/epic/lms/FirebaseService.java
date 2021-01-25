package epic.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firestore.v1.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.io.DataInput;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;

@Service
public class FirebaseService {
    public List<User> getUser() throws ExecutionException, InterruptedException, IOException {

        Firestore db = FirestoreClient.getFirestore();
        List<User> userArrayList = new ArrayList<User>();

        CollectionReference collectionReference = db.collection("Users");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            userArrayList.add(doc.toObject(User.class));
        }
        return userArrayList;
    }

    public User addUser(User userAdded) {

        Firestore db = FirestoreClient.getFirestore();


        Map<String, String> newUser = new HashMap<String, String>();
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
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (doc.get("recipient").equals(session.getAttribute("userid")) && (Boolean) doc.get("read") == false) {
                notifications.add(doc.toObject(Notifications.class));
            }

        }

        return notifications;
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
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id) {
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }

        for (User user : users) {
            id = id + 1;
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
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id) {
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }

        studentsModule = null;
        //Check Students in a module
        CollectionReference collectionReference1 = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot1 = collectionReference1.get();
        for (DocumentSnapshot doc1 : querySnapshot1.get().getDocuments()) {
            if (doc1.getId().equals(module)) {
                studentsModule = (List<String>) doc1.get("students");
            }
        }
        for (String student : studentsModule) {
            id = id + 1;
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
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id) {
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }

        for (User student : getUser()) {
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

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            moduleList.add(doc.toObject(Modules.class));
        }
        return moduleList;
    }

    public Map<String, Map<String, Assessment>> getAssessments(String module, String student) throws ExecutionException, InterruptedException, JSONException, IOException {

        //CREATE CONDITION TO CHECK IF THE MARK IS NULL. IFF IT IS NULL GRADE THE ASSESSMENT, IF IT IS ALREADY DEFINED
        //REDIRECT TO ERROR PAGE WITH ALREADY GRADED MESSAGE

        List<Assessment> assessmentList = new ArrayList<Assessment>();
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Assignments");
        Map<String, Assessment> assessmentMap = new HashMap<>();
        Map<String, Map<String, Assessment>> studentModuleAssessmentMap = new HashMap<>();


        CollectionReference collection = db.collection("Assignments");
        for (DocumentReference document : collection.listDocuments()) {

            DocumentReference docRef = db.collection("Assignments").document(module + " : " + student);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot doc = future.get();

            Map<String, Object> assessmentMapObject;

            assessmentMapObject = doc.getData();
            for (Map.Entry<String, Object> pair : assessmentMapObject.entrySet()) {
                final ObjectMapper mapper = new ObjectMapper();
                final Assessment pojo = mapper.convertValue(pair.getValue(), Assessment.class);
                assessmentMap.put(pair.getKey(), pojo);
            }
            studentModuleAssessmentMap.put(module + " : " + student, assessmentMap);

        }
        return studentModuleAssessmentMap;
    }

    public void createAssessment(HttpSession session, String assessmentName, String dueDate, String module, String assessmentType, String spec) throws InterruptedException, ExecutionException, IOException {


        Firestore db = FirestoreClient.getFirestore();

        for (Modules moduleiter : getModules()) {
            List<String> studentsInModule = moduleiter.getStudents();
            if (module.equals(moduleiter.getId())) {


                Assessment assessmentObject = new Assessment();
                assessmentObject.setCreator((String) session.getAttribute("userid"));
                assessmentObject.setDue(dueDate);
                assessmentObject.setSubmitted(false);
                assessmentObject.setSpec(spec);
                assessmentObject.setType(assessmentType);
                assessmentObject.setFeedback(null);


                //creates Inner map
                Map<String, Assessment> newAssessmentDetail = new HashMap<String, Assessment>();
                newAssessmentDetail.put(assessmentName, assessmentObject);


                //Creates document before writing if non existent
                CollectionReference collectionReference = db.collection("Assignments");
                ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

                for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {

                    for (String user : studentsInModule) {
                        if (!doc.getId().equals(module + " : " + user)) {
                            db.collection("Assignments").document(module + " : " + user).create(newAssessmentDetail);
                        } else {
                            db.collection("Assignments").document(module + " : " + user).set(newAssessmentDetail, SetOptions.merge());
                        }
                    }
                }




            }
        }

        //Updates Modules Collection to contain an Array of the Assignment names
        CollectionReference collectionReference1 = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot1 = collectionReference1.get();

        for (DocumentSnapshot doc1 : querySnapshot1.get().getDocuments()) {


            List<String> currentAssessment = new ArrayList<>();
            currentAssessment.add(assessmentName);
            if (!doc1.getId().equals(module)) {
                db.collection("Modules").document(module).update("assignments",FieldValue.arrayUnion(assessmentName));
            }
        }


    }

    public Map<String,List<String>> getAssessmentsList(HttpSession session) throws ExecutionException, InterruptedException, JSONException, IOException {

        Firestore db = FirestoreClient.getFirestore();


        List<String> assessmentsInModule = new ArrayList<>();
        Map<String, List<String>> allAssessments = new HashMap<>();




            CollectionReference collectionReference = db.collection("Modules");
            ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

            for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
                assessmentsInModule= (List<String>) doc.get("assignments");
                allAssessments.put(doc.getId(), assessmentsInModule);

            }



       return allAssessments;
    }

    public String gradeAssessment(HttpSession session, String feedback, String grade, String assessment, String student,
                                  String module) throws ExecutionException, InterruptedException, JSONException, IOException {

        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Assignments");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (doc.getId().equals(module + " : " + student)){


                Map<String, Object> updates = new HashMap<>();

                updates.put(assessment+".mark", grade);
                updates.put(assessment+".feedback", feedback);

                DocumentReference DocRef = db.collection("Assignments").document(module + " : " + student);
                ApiFuture<WriteResult> writeResult = DocRef.update(updates);
            }

        }
        return "success";
    }


    public List<User> usersStudent() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Users");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        List<User> students = new ArrayList<>();
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if(Objects.equals(doc.get("role"), "Student")){
                students.add(doc.toObject(User.class));

            }

        }
      return students;
    }
    public ModelAndView enroll(HttpSession session,String student,String course) throws ExecutionException, InterruptedException {
        ModelAndView x = new ModelAndView();
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if(doc.getId().equals(course)){
                List<String> studentsinModule= (List<String>) doc.get("students");
                for(String studentModule: studentsinModule){
                    if(student.equals(studentModule)) {
                        x.setViewName("errorPage.html");
                        session.setAttribute("error", "Student is already in the module");
                        return x;
                    }


                }
            }
            db.collection("Modules").document(course).update("students",FieldValue.arrayUnion(student));
            x.setViewName("Success.html");
            session.setAttribute("success", "Student is now enrolled in the module!");


        }
        return x;
    }

    public Multimap<String, Map<String,Assessment>> getAssessmentsStudent(HttpSession session) throws ExecutionException, InterruptedException, JSONException, IOException {

        Firestore db = FirestoreClient.getFirestore();



        Multimap<String, Map<String,Assessment>> studentAssessments = ArrayListMultimap.create();
        Map<String, Object> studentAssessmentObject;
        Map<String,Assessment> innerMap = new HashMap<>();





        CollectionReference collectionReference = db.collection("Assignments");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if(doc.getId().contains((CharSequence) session.getAttribute("userid"))){
                studentAssessmentObject=doc.getData();
                for (Map.Entry<String, Object> pair : studentAssessmentObject.entrySet()) {
                    final ObjectMapper mapper = new ObjectMapper();
                    final Assessment pojo = mapper.convertValue(pair.getValue(), Assessment.class);
                    innerMap.put(pair.getKey(),pojo);

                }
                String complexName = doc.getId();
                String simpleName = complexName.substring(0,complexName.indexOf(" "));
                studentAssessments.put(simpleName, innerMap);
            }

        }



        return studentAssessments;
    }

    public ModelAndView submitAssessment(String student, String assessment, String module, String assessmentURL) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Assignments");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        ModelAndView mv = new ModelAndView();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {

            if (doc.getId().equals(module + " : " + student)){
                boolean submitted = doc.getBoolean(assessment +".submitted");
                if(!(submitted)) {
                    Map<String, Object> updates = new HashMap<>();


                    LocalDateTime dateTime = LocalDateTime.now();
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                    String formattedDate = dateTime.format(format);

                    updates.put(assessment + ".datesubmitted", formattedDate);
                    updates.put(assessment + ".fileUrl", assessmentURL);
                    updates.put(assessment + ".submitted", true);

                    DocumentReference DocRef = db.collection("Assignments").document(module + " : " + student);
                    ApiFuture<WriteResult> writeResult = DocRef.update(updates);
                    mv.setViewName("Success.html");
                    return mv;
                }
            }


        }
        mv.setViewName("errorPage.html");
        return mv;
    }

    public ModelAndView createContent(String module, String contentTitle, String videoUrl, String imageUrl, String content, HttpSession session) throws ExecutionException, InterruptedException {
        ModelAndView x = new ModelAndView();
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Content");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

        Map<String, Content> contentMap = new HashMap<>();
        Content contentObj = new Content();

        contentObj.setContentTitle(contentTitle);
        contentObj.setModule(module);
        contentObj.setVideoUrl(videoUrl);
        contentObj.setImageUrl(imageUrl);
        contentObj.setContent(content);



        contentMap.put(contentTitle, contentObj);

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (!doc.getId().equals(module)) {
                db.collection("Content").document(module).create(contentMap);
                x.setViewName("Success.html");
                session.setAttribute("success", "Added to the DB!");
                return x;
            } else {
                String contentTitleInDb = (String) doc.get(contentTitle+".contentTitle");
                if (contentTitleInDb==null){
                    contentTitleInDb="";
                }
                if((contentTitleInDb.equals(contentTitle))){
                    x.setViewName("errorPage.html");
                    session.setAttribute("error", "Error, value is already in the DB");
                    return x;
                } else{
                    db.collection("Content").document(module).set(contentMap, SetOptions.merge());
                    x.setViewName("Success.html");
                    session.setAttribute("success", "Added to the DB!");
                    return x;
                }

            }
        }
        x.setViewName("errorPage.html");
        return x;

    }


}
