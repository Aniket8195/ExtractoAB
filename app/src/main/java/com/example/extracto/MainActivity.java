package com.example.extracto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private static final int STORAGE_PERMISSION_REQUEST = 2;

    private Button btnUpload;
    private TableLayout tableLayout;
    private boolean headerAdded;

    private ArrayList<String> namesList;
    private ArrayList<String> prnsList;
    private ArrayList<String> courseNamesList;
    private ArrayList<String> gradePointsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpload = findViewById(R.id.btnUpload);
        tableLayout = findViewById(R.id.tableLayout);
        headerAdded = false;

        namesList = new ArrayList<>();
        prnsList = new ArrayList<>();
        courseNamesList = new ArrayList<>();
        gradePointsList = new ArrayList<>();

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
        namesList.clear();
        prnsList.clear();
        courseNamesList.clear();
        gradePointsList.clear();

        try {
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            PdfReader reader = new PdfReader(inputStream);

            int numPages = reader.getNumberOfPages();
            boolean courseSectionStarted = false;

            for (int i = 1; i <= numPages; i++) {
                extractDataFromPage(reader, i, courseSectionStarted);
                courseSectionStarted = true;
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred while extracting PDF", Toast.LENGTH_SHORT).show();
        }

        displayTable();
    }

    private void extractDataFromPage(PdfReader reader, int pageNum, boolean courseSectionStarted) {
        try {
            // Extract text from the specified page
            String pageText = PdfTextExtractor.getTextFromPage(reader, pageNum);

            // Process the extracted text
            String[] lines = pageText.split("\n");

            boolean pageHeaderFound = false; // Track if the page header is already found
            boolean namePrnAdded = false; // Track if name and PRN are already added

            for (String line : lines) {
                // Check if the course section has started
                if (line.contains("Semester")) {
                    // Append the extracted data to the tableLayout
                    if (!pageHeaderFound) {
                        addTableHeader();
                        pageHeaderFound = true;
                    }

                    if (!namePrnAdded) {
                        addTableRow(extractName(lines), extractPRN(lines), "", ""); // Add the name and PRN to the table
                        namePrnAdded = true;
                    }

                    courseSectionStarted = true;
                }

                // Exclude specific sections
                if (line.contains("PIMPRI CHINCHWAD EDUCATION TRUST's") ||
                        line.contains("PIMPRI CHINCHWAD COLLEGE OF ENGINEERING,") ||
                        line.contains("Statement of Grades") ||
                        line.contains("Semester III Semester IV Cumulative Semester Record")) {
                    courseSectionStarted = false;
                }

                // Extract course name and grade points
                if (courseSectionStarted) {
                    String[] courseData = extractCourseData(line);
                    if (courseData != null && courseData.length == 2) {
                        addTableRow("", "", courseData[0], courseData[1]); // Add the course name and grade points to the table

                        // Add the extracted data to the respective ArrayLists
                        courseNamesList.add(courseData[0]);
                        gradePointsList.add(courseData[1]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred while extracting data from page " + pageNum, Toast.LENGTH_SHORT).show();
        }
    }

    private void addTableHeader() {
        TableRow headerRow = new TableRow(this);

        TextView nameHeader = new TextView(this);
        nameHeader.setText("Name");
        nameHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(nameHeader);

        TextView prnHeader = new TextView(this);
        prnHeader.setText("PRN");
        prnHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(prnHeader);

        TextView courseNameHeader = new TextView(this);
        courseNameHeader.setText("Course Name");
        courseNameHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(courseNameHeader);

        TextView gradePointsHeader = new TextView(this);
        gradePointsHeader.setText("Grade Points");
        gradePointsHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(gradePointsHeader);

        tableLayout.addView(headerRow);
    }

    private void addTableRow(String name, String prn, String courseName, String gradePoints) {
        TableRow tableRow = new TableRow(this);

        TextView nameTextView = new TextView(this);
        nameTextView.setText(name);
        tableRow.addView(nameTextView);

        TextView prnTextView = new TextView(this);
        prnTextView.setText(prn);
        tableRow.addView(prnTextView);

        TextView courseNameTextView = new TextView(this);
        courseNameTextView.setText(courseName);
        tableRow.addView(courseNameTextView);

        TextView gradePointsTextView = new TextView(this);
        gradePointsTextView.setText(gradePoints);
        tableRow.addView(gradePointsTextView);

        tableLayout.addView(tableRow);
    }

    private String extractName(String[] lines) {
        for (String line : lines) {
            // Extract the name (only the first occurrence)
            if (line.contains("Name :")) {
                return line.substring(line.indexOf("Name :") + 7).trim();
            }
        }
        return "";
    }

    private String extractPRN(String[] lines) {
        for (String line : lines) {
            // Extract the PRN
            if (line.contains("PRN :")) {
                return line.substring(line.indexOf("PRN :") + 6).trim();
            }
        }
        return "";
    }

    private String[] extractCourseData(String line) {
        String[] courseData = line.trim().split("\\s+");
        if (courseData.length >= 6) {
            StringBuilder courseNameBuilder = new StringBuilder();
            for (int i = 2; i < courseData.length - 2; i++) {
                courseNameBuilder.append(courseData[i]).append(" ");
            }
            String courseName = courseNameBuilder.toString().trim();
            String gradePoints = courseData[courseData.length - 1];
            return new String[]{courseName, gradePoints};
        }
        return null;
    }

    private void displayTable() {
        // Display the extracted data from the ArrayLists
        for (int i = 0; i < namesList.size(); i++) {
            addTableRow(namesList.get(i), prnsList.get(i), courseNamesList.get(i), gradePointsList.get(i));
        }
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
