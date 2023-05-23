package com.example.extracto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private static final int STORAGE_PERMISSION_REQUEST = 2;

    private Button btnUpload;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpload = findViewById(R.id.btnUpload);
        tvResult = findViewById(R.id.tvResult);

        btnUpload.setOnClickListener(v -> checkStoragePermission());
    }

    private void checkStoragePermission() {
        // Check if storage permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openFilePicker();
        } else {
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
        }
    }

    @SuppressWarnings("deprecation")
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_PDF_REQUEST);
    }
    private void extractInformationFromPdf(Uri pdfUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            PdfReader reader = new PdfReader(inputStream);

            StringBuilder extractedText = new StringBuilder();
            int numPages = reader.getNumberOfPages();
            for (int i = 1; i <= numPages; i++) {
                extractedText.append(extractDataFromPage(reader, i));
            }

            reader.close();

            // Set the extracted text to the TextView
            tvResult.setText(extractedText.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred while extracting PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private String extractDataFromPage(PdfReader reader, int pageNum) {
        StringBuilder pageData = new StringBuilder();

        try {
            // Extract text from the specified page
            String pageText = PdfTextExtractor.getTextFromPage(reader, pageNum);

            // Process the extracted text
            String[] lines = pageText.split("\n");

            String name = "";
            String prn = "";
            boolean courseSectionStarted = false;

            for (String line : lines) {
                // Extract the name (only the first occurrence)
                if (line.contains("Name :") && name.isEmpty()) {
                    String fullName = line.substring(line.indexOf("Name :") + 7).trim();
                    int lastNameIndex = fullName.lastIndexOf(" ");
                    if (lastNameIndex != -1) {
                        name = fullName.substring(lastNameIndex + 1) + " " + fullName.substring(0, lastNameIndex);
                    } else {
                        name = fullName;
                    }
                }

                // Extract the PRN
                if (line.contains("PRN :")) {
                    prn = line.substring(line.indexOf("PRN :") + 6).trim();
                }

                // Check if the course section has started
                if (line.contains("Semester")) {
                    courseSectionStarted = true;
                    // Append the extracted data to the pageData StringBuilder
                    pageData.append("Page ").append(pageNum).append(":\n");
                    pageData.append("Name: ").append(name).append("\n");
                    pageData.append("PRN: ").append(prn).append("\n");
                    pageData.append("Course Name\tGrade Points\n");
                    continue;
                }

                // Extract course name and grade points
                if (courseSectionStarted) {
                    String[] courseData = line.trim().split("\\s+");
                    if (courseData.length >= 6) {
                        StringBuilder courseNameBuilder = new StringBuilder();
                        for (int i = 2; i < courseData.length - 2; i++) {
                            courseNameBuilder.append(courseData[i]).append(" ");
                        }
                        String courseName = courseNameBuilder.toString().trim();
                        String gradePoints = courseData[courseData.length - 1];
                        pageData.append(courseName).append("\t").append(gradePoints).append("\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred while extracting data from page " + pageNum, Toast.LENGTH_SHORT).show();
        }

        return pageData.toString();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri pdfUri = data.getData();
            extractInformationFromPdf(pdfUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}