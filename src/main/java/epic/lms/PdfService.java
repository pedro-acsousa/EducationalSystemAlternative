package epic.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;



@Service
public class PdfService {
    @Autowired
    FirebaseService firebaseService;
    // document creation
    public InputStream createPdf(String module, String student, String assessment) throws Exception {
        Firestore db  = FirestoreClient.getFirestore();
        Assessment currentAssessment = new Assessment();
        Map<String,Map<String, Assessment>> assessmentStudent = new HashMap<>();
        assessmentStudent = firebaseService.getAssessments(module, student);

        // find assessments to include marks
        for (Map.Entry<String, Map<String, Assessment>> outterMap : assessmentStudent.entrySet()) {
            for (Map.Entry<String, Assessment> assessmentinModule : outterMap.getValue().entrySet()) {
                if (assessmentinModule.getKey().equals(assessment)) {
                    currentAssessment = assessmentinModule.getValue();
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // set document dimensions
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();

        // get image
        String imageFile ="https://firebasestorage.googleapis.com/v0/b/epic-learning.appspot.com/o/Epic.png?alt=media&token=35b02152-c301-4627-8283-f1e70e258f79";
        Image img = Image.getInstance(imageFile);
        // image formatting
        img.setAlignment(1);
        img.scaleToFit(70,70);
        document.add(img); // adding image
        // insert current date
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        Paragraph p = new Paragraph("Student Assignment report ("+ dtf.format(now) +")" );
        p.setAlignment(Element.ALIGN_CENTER); // formatting alignment
        document.add(p); // add to document
        // document details and remarks
        document.add(new Paragraph(Chunk.NEWLINE));
        document.add(new Paragraph(Chunk.NEWLINE));
        document.add(new Paragraph(Chunk.NEWLINE));
        document.add(new Paragraph("Student: " + student));
        document.add(new Paragraph(Chunk.NEWLINE));
        document.add(new Paragraph("Assignment creator: " + currentAssessment.getCreator()));
        document.add(new Paragraph(Chunk.NEWLINE));
        document.add(new Paragraph("Due: " + currentAssessment.getDue()));
        document.add(new Paragraph(Chunk.NEWLINE));
        document.add(new Paragraph("Assessment type: " + currentAssessment.getType()));
        document.add(new Paragraph(Chunk.NEWLINE));
        document.add(new Paragraph(Chunk.NEWLINE));

        // get assignment submission status for printing correct message
        if ("false".equals(String.valueOf(currentAssessment.isSubmitted()))){ // if there is no submission record
            Font f=new Font(Font.FontFamily.HELVETICA,15.0f,Font.UNDERLINE+ Font.BOLD,BaseColor.RED); // text format
            Paragraph p1 = new Paragraph("This assignment was not submitted yet!",f); // print not submitted message
            p1.setAlignment(Element.ALIGN_CENTER); // formatting alignment
            document.add(p1);
            // formatting
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));

        } else if ("true".equals(String.valueOf(currentAssessment.isSubmitted()))) { // if the assignment was submitted
            Font f = new Font(Font.FontFamily.HELVETICA, 15.0f, Font.UNDERLINE + Font.BOLD, BaseColor.GREEN); // text format
            Paragraph p1 = new Paragraph("This assignment was submitted. Submission details:", f); // print successful message
            p1.setAlignment(Element.ALIGN_CENTER); // text alignment
            document.add(p1);
            // formatting
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph("Date of submission: " + currentAssessment.getDatesubmitted())); // add date
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph("Submission URL: " + currentAssessment.getFileUrl())); // add submission URl
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));
            // check grading status of assignment
            if(currentAssessment.getMark()==null) { // if not graded
                Font f1 = new Font(Font.FontFamily.HELVETICA, 15.0f, Font.UNDERLINE+ Font.BOLD, BaseColor.ORANGE); // text format
                Paragraph p2 = new Paragraph("--This assignment is awaiting grading--", f1); // print awaiting grading message
                p2.setAlignment(Element.ALIGN_CENTER); // text alignment
                document.add(p2);
                // formatting
                document.add(new Paragraph(Chunk.NEWLINE));
                document.add(new Paragraph(Chunk.NEWLINE));
            } else { // assignment was graded
                Font f2 = new Font(Font.FontFamily.HELVETICA, 15.0f, Font.UNDERLINE+ Font.BOLD, BaseColor.GREEN); // text format
                Paragraph p3 = new Paragraph("--This assignment has been graded--", f2); // print assignment graded message
                p3.setAlignment(Element.ALIGN_CENTER); // text alignment
                document.add(p3);
                // formatting
                document.add(new Paragraph(Chunk.NEWLINE));
                document.add(new Paragraph(Chunk.NEWLINE));
                // adding supplmentary information
                document.add(new Paragraph("Grade attributed :" + currentAssessment.getMark())); // print grade of assignment
                document.add(new Paragraph(Chunk.NEWLINE));
                document.add(new Paragraph("Additional marking feedback: " + currentAssessment.getFeedback())); // print feedback for assignment
                // formatting
                document.add(new Paragraph(Chunk.NEWLINE));
                document.add(new Paragraph(Chunk.NEWLINE));
            }
        }
        // generate footer of document
        Paragraph p4 = new Paragraph("--End of Report--");
        p4.setAlignment(Element.ALIGN_CENTER);
        document.add(p4);
        document.close(); // finish document
        return new ByteArrayInputStream(out.toByteArray());
    }
    // initiate pdf generation process
    @Controller
    @RequestMapping("/admin/generate_pdf")
    public class PdfController {
        @Autowired
        private PdfService pdfService;
        @GetMapping("/online")
        public void generatePdf(HttpServletResponse response, @RequestParam("Class") String module, @RequestParam("Student") String student, @RequestParam("Assessment") String assessment) throws Exception {
            response.setContentType("application/pdf"); // set file format
            response.setHeader("Content-Disposition", "attachment; filename=\"document.pdf\""); // set file name
            InputStream pdf = pdfService.createPdf(module, student, assessment); // pdf generation process
            org.apache.commons.io.IOUtils.copy(pdf, response.getOutputStream());
            response.flushBuffer();
        }
    }
}