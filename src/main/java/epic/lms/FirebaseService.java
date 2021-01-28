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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

        Firestore db = FirestoreClient.getFirestore(); // firestore database initialise
        List<User> userArrayList = new ArrayList<User>();

        CollectionReference collectionReference = db.collection("Users"); // get user collection from json map
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();// get username from user collection
        // Add users to array list
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            userArrayList.add(doc.toObject(User.class));
        }
        return userArrayList;
    }

    public User addUser(User userAdded) {
        Firestore db = FirestoreClient.getFirestore();

        Map<String, String> newUser = new HashMap<String, String>();
        // get account creation details from session
        newUser.put("username", userAdded.getUsername());
        newUser.put("password", userAdded.getPassword());
        newUser.put("firstname", userAdded.getFirstname());
        newUser.put("surname", userAdded.getSurname());
        newUser.put("email", userAdded.getEmail());
        newUser.put("role", userAdded.getRole());

        // add details to database
        db.collection("Users").document(userAdded.getUsername()).create(newUser);

        return userAdded;
    }

    public List<Notifications> getAllNotifications(HttpSession session) throws ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();

        List<Notifications> notifications = new ArrayList<>();
        CollectionReference collectionReference = db.collection("Notifications"); // get notifications json from database
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get(); // get notifications from json file
        // save all available notifications to the database
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (session.getAttribute("userrole").equals("Student")) {
                if (doc.get("recipient").equals(session.getAttribute("userid")) && (Boolean) doc.get("read") == false) {
                    notifications.add(doc.toObject(Notifications.class));
                }
            } else if (session.getAttribute("userrole").equals("Lecturer")) {
                if (doc.get("sender").equals(session.getAttribute("userid")) && (Boolean) doc.get("read") == false) {
                    notifications.add(doc.toObject(Notifications.class));
                }
            }

        }
        return notifications;
    }

    public List<Notifications> deleteNotification(List<Notifications> allNotifications, int id, HttpSession session) throws ExecutionException, InterruptedException {
        // delete notifications if selected id matches
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

        CollectionReference collectionReference = db.collection("Notifications"); // get notifications json
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get(); // get json contents
        //increment id so id is unique
        int id = 0;
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id) {
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }
        // for all users add notifications to their notification list
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

        CollectionReference collectionReference = db.collection("Notifications"); // get notifications json
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get(); // get data from json
        //increment id so id is unique
        int id = 0;
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id) {
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }
        studentsModule = null;
        // Filter students that are only in the selected module
        CollectionReference collectionReference1 = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot1 = collectionReference1.get();
        for (DocumentSnapshot doc1 : querySnapshot1.get().getDocuments()) {
            if (doc1.getId().equals(module)) {
                studentsModule = (List<String>) doc1.get("students");
            }
        }
        // Add notifications to all students in the filtered list
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

        CollectionReference collectionReference = db.collection("Notifications"); // get notifications json
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get(); // get notifications content from json
        //increment id so id is unique
        int id = 0;
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id) {
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }
        // send notification to selected desired student
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
        return "Sent Successfully.";
    }


    public List<Modules> getModules(HttpSession session) throws ExecutionException, InterruptedException {
        List<Modules> moduleList = new ArrayList<Modules>();

        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Modules"); // get modules json
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get(); // get module details from json
        // read every module from json and import to local arraylist
        if (session.getAttribute("userrole").equals("Lecturer")) {
            for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
                moduleList.add(doc.toObject(Modules.class));
            }
        } else {
            //if the session user is a student it will only display the modules the student is registered in
            for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
                List<String> studentsInModule = new ArrayList<>();
                studentsInModule = (List<String>) doc.get("students");
                for (String student : studentsInModule) {
                    if (session.getAttribute("userid").equals(student)) {
                        moduleList.add(doc.toObject(Modules.class));
                    }

                }

            }
        }


        return moduleList;
    }

    public Map<String, Map<String, Assessment>> getAssessments(String module, String student) throws ExecutionException, InterruptedException, JSONException, IOException {

        Firestore db = FirestoreClient.getFirestore();
        Map<String, Assessment> assessmentMap = new HashMap<>();
        Map<String, Map<String, Assessment>> studentModuleAssessmentMap = new HashMap<>();
        CollectionReference collection = db.collection("Assignments");

        //CREATE CONDITION TO CHECK IF THE MARK IS NULL. IF IT IS NULL GRADE THE ASSESSMENT, IF IT IS ALREADY DEFINED
        //REDIRECT TO ERROR PAGE WITH ALREADY GRADED MESSAGE

        for (DocumentReference document : collection.listDocuments()) {
            // extract assignments details from json
            DocumentReference docRef = db.collection("Assignments").document(module + " : " + student);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot doc = future.get();

            Map<String, Object> assessmentMapObject;

            assessmentMapObject = doc.getData();
            // save map to hashmap
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
        // find the right module to add the assessment to
        for (Modules moduleiter : getModules(session)) {
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

                // for each student in each module, add the assessment to their collection
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

        // Updates Modules Collection to contain an Array of the Assignment names
        CollectionReference collectionReference1 = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot1 = collectionReference1.get();
        for (DocumentSnapshot doc1 : querySnapshot1.get().getDocuments()) {
            List<String> currentAssessment = new ArrayList<>();
            currentAssessment.add(assessmentName);
            if (!doc1.getId().equals(module)) {
                db.collection("Modules").document(module).update("assignments", FieldValue.arrayUnion(assessmentName));
            }
        }
    }

    public Map<String, List<String>> getAssessmentsList(HttpSession session) throws ExecutionException, InterruptedException, JSONException, IOException {

        Firestore db = FirestoreClient.getFirestore();
        List<String> assessmentsInModule = new ArrayList<>();
        Map<String, List<String>> allAssessments = new HashMap<>();
        CollectionReference collectionReference = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        // get assignments from the modules json
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            assessmentsInModule = (List<String>) doc.get("assignments");
            allAssessments.put(doc.getId(), assessmentsInModule);
        }
        return allAssessments;
    }

    public String gradeAssessment(HttpSession session, String feedback, String grade, String assessment, String student,
                                  String module) throws ExecutionException, InterruptedException, JSONException, IOException {

        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Assignments");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        // select student with student id, and recording grade and feedback
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (doc.getId().equals(module + " : " + student)) {
                Map<String, Object> updates = new HashMap<>();

                updates.put(assessment + ".mark", grade);
                updates.put(assessment + ".feedback", feedback);

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
        // gets list of all students
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            if (Objects.equals(doc.get("role"), "Student")) {
                students.add(doc.toObject(User.class));
            }
        }
        return students;
    }

    public ModelAndView enroll(HttpSession session, String student, String course) throws ExecutionException, InterruptedException {
        ModelAndView x = new ModelAndView();
        Firestore db = FirestoreClient.getFirestore();
        // retrieves all modules from database
        CollectionReference collectionReference = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        // if course is found then enroll student to the course
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            // checks if student is already enrolls in the course
            if (doc.getId().equals(course)) {
                List<String> studentsinModule = (List<String>) doc.get("students");
                for (String studentModule : studentsinModule) {
                    // if student is in course than send to error page
                    if (student.equals(studentModule)) {
                        x.setViewName("errorPage.html");
                        session.setAttribute("error", "Student is already in the module");
                        return x;
                    }
                }
            }
            // enrolls student to the course
            db.collection("Modules").document(course).update("students", FieldValue.arrayUnion(student));
            x.setViewName("Success.html");
            session.setAttribute("success", "Student is now enrolled in the module!");
        }
        return x;
    }

    public Multimap<String, Map<String, Assessment>> getAssessmentsStudent(HttpSession session) throws ExecutionException, InterruptedException, JSONException, IOException {

        Firestore db = FirestoreClient.getFirestore();
        Multimap<String, Map<String, Assessment>> studentAssessments = ArrayListMultimap.create();
        Map<String, Object> studentAssessmentObject;


        //get assessments list from database
        CollectionReference collectionReference = db.collection("Assignments");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            // if assessments title contain userid
            if (doc.getId().contains((CharSequence) session.getAttribute("userid"))) {
                studentAssessmentObject = doc.getData();
                Map<String, Assessment> innerMap = new HashMap<>();
                for (Map.Entry<String, Object> pair : studentAssessmentObject.entrySet()) {
                    final ObjectMapper mapper = new ObjectMapper();
                    final Assessment pojo = mapper.convertValue(pair.getValue(), Assessment.class);
                    innerMap.put(pair.getKey(), pojo);
                }
                // save assessment names to a hashmap
                String complexName = doc.getId();
                String simpleName = complexName.substring(0, complexName.indexOf(" "));
                studentAssessments.put(simpleName, innerMap);
            }

            if (session.getAttribute("userrole").toString().equals("Lecturer")) {
                studentAssessmentObject = doc.getData();
                Map<String, Assessment> innerMap1 = new HashMap<>();
                for (Map.Entry<String, Object> pair : studentAssessmentObject.entrySet()) {
                    final ObjectMapper mapper = new ObjectMapper();
                    final Assessment pojo = mapper.convertValue(pair.getValue(), Assessment.class);

                    innerMap1.put(pair.getKey(), pojo);
                }
                // save assessment names to a hashmap
                String complexName = doc.getId();
                String complexNameJsonValid = complexName.replace(":", "^");
                studentAssessments.put(complexNameJsonValid, innerMap1);

            }


        }
        return studentAssessments;
    }

    public ModelAndView submitAssessment(String student, String assessment, String module, String assessmentURL) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Assignments");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        ModelAndView mv = new ModelAndView();
        // for every entry from assignment json
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            // if assignments name equal module and student id in title
            if (doc.getId().equals(module + " : " + student)) {
                // get submission status
                boolean submitted = doc.getBoolean(assessment + ".submitted");
                // if hw not submitted
                if (!(submitted)) {
                    Map<String, Object> updates = new HashMap<>();
                    // get current date
                    LocalDateTime dateTime = LocalDateTime.now();
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                    String formattedDate = dateTime.format(format);
                    // save submitted work
                    updates.put(assessment + ".datesubmitted", formattedDate);
                    updates.put(assessment + ".fileUrl", assessmentURL);
                    updates.put(assessment + ".submitted", true); // change status to submitted
                    // save updates to the relevant json collection in assignments
                    DocumentReference DocRef = db.collection("Assignments").document(module + " : " + student);
                    ApiFuture<WriteResult> writeResult = DocRef.update(updates);
                    mv.setViewName("Success.html");
                    return mv;
                }
            }
        }
        // show error page if assignment for student is not found
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
            if (doc.getId().equals(module)) {
                String contentTitleInDb = (String) doc.get(contentTitle + ".contentTitle");
                if (contentTitleInDb == null) {
                    contentTitleInDb = "";
                }
                if (!(contentTitle).equals(contentTitleInDb)) {
                    db.collection("Content").document(module).set(contentMap, SetOptions.merge());
                    x.setViewName("Success.html");
                    session.setAttribute("success", "Added to the DB!");
                    return x;
                } else {
                    x.setViewName("errorPage.html");
                    session.setAttribute("error", "Error, value is already in the DB");
                    return x;
                }
            } else {
                db.collection("Content").document(module).create(contentMap);
                x.setViewName("Success.html");
                session.setAttribute("success", "Added to the DB!");
                return x;
            }


        }
        x.setViewName("errorPage.html");
        return x;

    }

    public Map<String, Map<String, Content>> getContent() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        Map<String, Map<String, Content>> contentMap = new HashMap<>();
        CollectionReference collection = db.collection("Content");

        for (DocumentReference document : collection.listDocuments()) {
            // extract content from the DB and placing them in an nested map object structure
            DocumentReference docRef = db.collection("Content").document(document.getId());
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot doc = future.get();

            Map<String, Object> contentMapObject;

            contentMapObject = doc.getData();
            // save map to hashmap
            Map<String, Content> innerContentMap = new HashMap<>();
            for (Map.Entry<String, Object> pair : contentMapObject.entrySet()) {
                final ObjectMapper mapper = new ObjectMapper();
                final Content pojo = mapper.convertValue(pair.getValue(), Content.class);

                innerContentMap.put(pair.getKey(), pojo);

            }
            contentMap.put(document.getId(), innerContentMap);

        }
        return contentMap;

    }

    public Map<String, News> getNews() throws ExecutionException, InterruptedException {


        Firestore db = FirestoreClient.getFirestore();

        Map<String, News> mapNews = new HashMap<>();
        CollectionReference collection = db.collection("News");

        for (DocumentReference document : collection.listDocuments()) {
            // extract content from the DB and placing them in a map object structure
            DocumentReference docRef = db.collection("News").document(document.getId());
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot doc = future.get();


            News newNews = doc.toObject(News.class);

            mapNews.put(document.getId(), newNews);

        }




        return mapNews;
    }
}
